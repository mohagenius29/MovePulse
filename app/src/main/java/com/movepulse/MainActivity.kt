package com.movepulse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movepulse.tracking.TrackingService
import com.movepulse.ui.AnalysisScreen
import com.movepulse.ui.MainViewModel
import com.movepulse.ui.MapScreen
import com.movepulse.ui.TodayScreen
import com.movepulse.ui.WorkoutsScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startTracking() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF2E7D32))) {
                var tab by remember { mutableIntStateOf(0) }
                val todayHours by vm.todayHours.collectAsStateWithLifecycle()
                val week by vm.week.collectAsStateWithLifecycle()
                val route by vm.route.collectAsStateWithLifecycle()
                val sessions by vm.sessions.collectAsStateWithLifecycle()

                Scaffold(bottomBar = {
                    NavigationBar {
                        listOf("🏠" to "Today", "🗺" to "Map", "🏋" to "Workouts", "📊" to "Analysis").forEachIndexed { i, (icon, label) ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Text(icon) },
                                label = { Text(label) },
                            )
                        }
                    }
                }) { padding ->
                    Box(Modifier.padding(padding)) {
                        when (tab) {
                            0 -> TodayScreen(vm, todayHours, sessions)
                            1 -> MapScreen(route)
                            2 -> WorkoutsScreen(vm)
                            else -> AnalysisScreen(todayHours, week, vm.prefs.dailyGoal)
                        }
                    }
                }
            }
        }
        permissionLauncher.launch(requiredPermissions())
    }

    private fun requiredPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private fun startTracking() {
        val activityOk = Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
        if (activityOk) {
            ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java))
        }
    }
}
