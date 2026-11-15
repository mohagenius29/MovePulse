package com.movepulse.tracking

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.movepulse.data.AppDatabase
import com.movepulse.data.HourStat
import com.movepulse.data.Prefs
import com.movepulse.data.RoutePoint
import com.movepulse.data.StatsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Foreground service that runs all day:
 *  - counts steps (hardware step counter)
 *  - classifies the last 10 seconds as still / moving / walking / jogging / running
 *  - estimates calories
 *  - records the GPS route while walking/jogging/running
 *  - sends milestone and inactivity notifications
 */
class TrackingService : Service(), SensorEventListener, LocationListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var dao: StatsDao
    private lateinit var prefs: Prefs
    private lateinit var sensors: SensorManager
    private var loop: Job? = null

    @Volatile private var rawSteps = -1f
    private var lastRaw = -1f
    private var accelSum = 0.0
    private var accelCount = 0
    @Volatile private var currentType = ActivityType.STILL
    private var lastMotion = System.currentTimeMillis()
    private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        dao = AppDatabase.get(this).statsDao()
        prefs = Prefs(this)
        sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Notifier.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startAsForeground()
        } catch (e: SecurityException) {
            stopSelf()
            return START_NOT_STICKY
        }
        registerListeners()
        if (loop == null) {
            loop = scope.launch {
                while (isActive) {
                    delay(TICK_SECONDS * 1000L)
                    tick()
                }
            }
        }
        return START_STICKY
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun startAsForeground() {
        var type = 0
        if (Build.VERSION.SDK_INT >= 34) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        if (Build.VERSION.SDK_INT >= 29 && hasLocationPermission()) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        ServiceCompat.startForeground(this, Notifier.SERVICE_ID, Notifier.serviceNotification(this), type)
    }

    private fun registerListeners() {
        sensors.unregisterListener(this)
        sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (hasLocationPermission()) {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                lm.removeUpdates(this)
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this, Looper.getMainLooper())
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    // ---- sensors ----
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> rawSteps = event.values[0]
            Sensor.TYPE_ACCELEROMETER -> {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                val magnitude = sqrt(x * x + y * y + z * z)
                synchronized(this) {
                    accelSum += abs(magnitude - 9.81f)
                    accelCount++
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ---- location ----
    override fun onLocationChanged(location: Location) {
        if (!currentType.isOutdoorRoute || location.accuracy > 30f) return
        val prev = lastLocation
        if (prev != null && prev.distanceTo(location) < 4f) return
        lastLocation = location
        val date = LocalDateTime.now().toLocalDate().toString()
        scope.launch(Dispatchers.IO) {
            dao.insertPoint(RoutePoint(date = date, ts = location.time, lat = location.latitude, lon = location.longitude))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    // ---- the 10-second tick ----
    private suspend fun tick() {
        val raw = rawSteps
        var delta = 0
        if (raw >= 0) {
            if (lastRaw < 0 || raw < lastRaw) lastRaw = raw   // first reading or reboot reset
            delta = (raw - lastRaw).roundToInt()
            lastRaw = raw
        }
        val avgAccel = synchronized(this) {
            val a = if (accelCount > 0) accelSum / accelCount else 0.0
            accelSum = 0.0
            accelCount = 0
            a
        }

        val cadence = delta * (60 / TICK_SECONDS)   // steps per minute
        val type = when {
            delta >= 3 -> when {
                cadence < 135 -> ActivityType.WALK
                cadence < 165 -> ActivityType.JOG
                else -> ActivityType.RUN
            }
            avgAccel > 0.8 -> ActivityType.MOVE
            else -> ActivityType.STILL
        }
        currentType = type
        if (type != ActivityType.STILL) lastMotion = System.currentTimeMillis()

        val now = LocalDateTime.now()
        val date = now.toLocalDate().toString()
        val cur = dao.get(date, now.hour) ?: HourStat(date = date, hour = now.hour)
        dao.upsert(
            cur.copy(
                steps = cur.steps + delta,
                calories = cur.calories + CalorieCalculator.kcal(type, prefs.weightKg, TICK_SECONDS),
                walkSeconds = cur.walkSeconds + if (type == ActivityType.WALK) TICK_SECONDS else 0,
                jogSeconds = cur.jogSeconds + if (type == ActivityType.JOG) TICK_SECONDS else 0,
                runSeconds = cur.runSeconds + if (type == ActivityType.RUN) TICK_SECONDS else 0,
                moveSeconds = cur.moveSeconds + if (type == ActivityType.MOVE) TICK_SECONDS else 0,
            )
        )

        checkMilestones(date, type)
        checkInactivity(now)
    }

    private suspend fun checkMilestones(date: String, type: ActivityType) {
        val steps = dao.totalSteps(date)
        val kcal = dao.totalCalories(date).roundToInt()
        if (prefs.lastMilestoneDate != date) {
            prefs.lastMilestoneDate = date
            prefs.lastMilestoneLevel = 0
        }
        val level = steps / MILESTONE_STEPS
        if (steps >= prefs.dailyGoal && prefs.goalNotifiedDate != date) {
            prefs.goalNotifiedDate = date
            prefs.lastMilestoneLevel = level
            Notifier.progress(this, "Daily goal reached! 🎉", MotivationEngine.goalReached(steps, kcal, prefs.dailyGoal))
        } else if (level > prefs.lastMilestoneLevel) {
            prefs.lastMilestoneLevel = level
            Notifier.progress(this, "Great job! 🔥", MotivationEngine.milestone(level * MILESTONE_STEPS, kcal, type))
        }
    }

    private fun checkInactivity(now: LocalDateTime) {
        val nowMs = System.currentTimeMillis()
        val idleMinutes = (nowMs - lastMotion) / 60_000
        val gapMs = prefs.inactivityMinutes * 60_000L
        // Only remind between 08:00 and 20:59 so nights stay quiet.
        if (idleMinutes >= prefs.inactivityMinutes && now.hour in 8..20 &&
            nowMs - prefs.lastInactivityNotify >= gapMs
        ) {
            prefs.lastInactivityNotify = nowMs
            Notifier.reminder(this, "Time to move! ⏰", MotivationEngine.inactivity(idleMinutes))
        }
    }

    override fun onDestroy() {
        sensors.unregisterListener(this)
        (getSystemService(Context.LOCATION_SERVICE) as LocationManager).removeUpdates(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TICK_SECONDS = 10
        const val MILESTONE_STEPS = 500
    }
}
