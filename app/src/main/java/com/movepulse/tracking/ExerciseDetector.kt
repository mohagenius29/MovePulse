package com.movepulse.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.sqrt

/** What the UI shows live: current activity and reps (reps only for squats / jumping jacks). */
data class Live(val type: ActivityType = ActivityType.STILL, val reps: Int = 0)

object LiveState {
    val current = MutableStateFlow(Live())
}

/**
 * Thresholds for the exercise detector. These are sensible starting values from
 * accelerometer physics, NOT tuned on real phones yet. Adjust here after testing.
 */
object DetectorConfig {
    const val WINDOW_MS = 8000L
    const val EVAL_MS = 1000L
    const val MIN_SAMPLES = 20

    const val MIN_ACTIVITY_RMS = 0.7f           // below this: not exercising
    const val MAX_STEPS_PER_MIN = 100f          // above this: walking/jogging/running logic handles it

    const val SQUAT_MIN_HZ = 0.15f
    const val SQUAT_MAX_HZ = 1.0f
    const val SQUAT_MAX_RMS = 4.5f

    const val JUMP_MIN_HZ = 1.3f
    const val JUMP_MAX_HZ = 3.2f
    const val JUMP_MIN_RMS = 2.5f               // smoothing shrinks fast motion, so this is lower than the raw amplitude
    const val JUMP_MAX_CV = 0.35f               // jumping jacks are very regular

    const val DANCE_MIN_HZ = 0.8f
    const val DANCE_MAX_HZ = 3.5f
    const val DANCE_MIN_RMS = 1.0f
    const val DANCE_MAX_RMS = 7.0f

    const val ENTER_VOTES = 2                   // consecutive 1 s checks to start an exercise
    const val EXIT_VOTES = 3                    // consecutive checks to end it
}

/** Counts local maxima above [threshold], at least [minSepMs] apart. */
class PeakCounter(private val threshold: Float, private val minSepMs: Long) {
    private var p2 = 0f
    private var p1 = 0f
    private var t1 = 0L
    private var lastPeak = Long.MIN_VALUE / 2
    val times = ArrayDeque<Long>()

    fun onSample(t: Long, v: Float): Boolean {
        var peak = false
        if (p1 > p2 && p1 >= v && p1 > threshold && t1 - lastPeak >= minSepMs) {
            lastPeak = t1
            times.addLast(t1)
            peak = true
        }
        p2 = p1
        p1 = v
        t1 = t
        return peak
    }

    fun trim(now: Long, windowMs: Long) {
        while (times.isNotEmpty() && now - times.first() > windowMs) times.removeFirst()
    }

    fun clear() = times.clear()
}

/**
 * Detects squats, jumping jacks and dancing from the phone's accelerometer
 * (phone in a pocket, armband or hand). Orientation independent: it projects
 * body acceleration onto the gravity direction to get the vertical component.
 *
 * Feed it with [onAccel] (about 25 Hz) and [onStep] (step detector events).
 */
class ExerciseDetector {
    @Volatile var current: ActivityType = ActivityType.STILL
        private set
    @Volatile var reps: Int = 0
        private set
    /** Reps of the exercise that just ended, so callers can save a session. */
    @Volatile var lastFinishedReps: Int = 0
        private set

    private val classify = PeakCounter(0.8f, 250)
    private val squatReps = PeakCounter(1.0f, 1000)
    private val jumpReps = PeakCounter(3.0f, 300)
    private val stepTimes = ArrayDeque<Long>()
    private val energy = ArrayDeque<Pair<Long, Float>>()

    private val gravity = FloatArray(3)
    private var gravityReady = false
    private var smooth = 0f
    private var lastEval = Long.MIN_VALUE / 2
    private var candidate = ActivityType.STILL
    private var votes = 0
    private var offVotes = 0

    fun onStep(tMs: Long) {
        stepTimes.addLast(tMs)
    }

    /** Returns true when [current] or [reps] changed. */
    fun onAccel(tMs: Long, x: Float, y: Float, z: Float): Boolean {
        val beforeType = current
        val beforeReps = reps

        if (!gravityReady) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z
            gravityReady = true
        }
        val a = 0.98f
        gravity[0] = a * gravity[0] + (1 - a) * x
        gravity[1] = a * gravity[1] + (1 - a) * y
        gravity[2] = a * gravity[2] + (1 - a) * z
        val g = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]).coerceAtLeast(0.1f)
        val vertical = ((x - gravity[0]) * gravity[0] + (y - gravity[1]) * gravity[1] + (z - gravity[2]) * gravity[2]) / g
        smooth += 0.35f * (vertical - smooth)

        energy.addLast(tMs to smooth * smooth)
        classify.onSample(tMs, smooth)
        if (squatReps.onSample(tMs, smooth) && current == ActivityType.SQUAT) reps++
        if (jumpReps.onSample(tMs, smooth) && current == ActivityType.JUMP) reps++

        if (tMs - lastEval >= DetectorConfig.EVAL_MS) {
            lastEval = tMs
            evaluate(tMs)
        }
        return current != beforeType || reps != beforeReps
    }

    private fun evaluate(now: Long) {
        val w = DetectorConfig.WINDOW_MS
        classify.trim(now, w)
        squatReps.trim(now, w)
        jumpReps.trim(now, w)
        while (energy.isNotEmpty() && now - energy.first().first > w) energy.removeFirst()
        while (stepTimes.isNotEmpty() && now - stepTimes.first() > w) stepTimes.removeFirst()
        if (energy.size < DetectorConfig.MIN_SAMPLES) return

        val rms = sqrt(energy.sumOf { it.second.toDouble() } / energy.size).toFloat()
        val hz = classify.times.size / (w / 1000f)
        val cv = intervalCv(classify.times)
        val stepsPerMin = stepTimes.size * 60_000f / w

        val c = classifyWindow(rms, hz, cv, stepsPerMin)
        vote(c, now)
    }

    private fun classifyWindow(rms: Float, hz: Float, cv: Float, stepsPerMin: Float): ActivityType = with(DetectorConfig) {
        when {
            rms < MIN_ACTIVITY_RMS -> ActivityType.STILL
            stepsPerMin > MAX_STEPS_PER_MIN -> ActivityType.STILL
            hz in SQUAT_MIN_HZ..SQUAT_MAX_HZ && rms <= SQUAT_MAX_RMS -> ActivityType.SQUAT
            hz in JUMP_MIN_HZ..JUMP_MAX_HZ && rms >= JUMP_MIN_RMS && cv <= JUMP_MAX_CV -> ActivityType.JUMP
            hz in DANCE_MIN_HZ..DANCE_MAX_HZ && rms in DANCE_MIN_RMS..DANCE_MAX_RMS -> ActivityType.DANCE
            else -> ActivityType.STILL
        }
    }

    private fun vote(c: ActivityType, now: Long) {
        if (c == current) {
            votes = 0
            offVotes = 0
            return
        }
        if (c == ActivityType.STILL) {
            votes = 0
            offVotes++
            if (offVotes >= DetectorConfig.EXIT_VOTES) switchTo(ActivityType.STILL, now)
        } else {
            offVotes = 0
            if (c == candidate) votes++ else { candidate = c; votes = 1 }
            if (votes >= DetectorConfig.ENTER_VOTES) switchTo(c, now)
        }
    }

    private fun switchTo(type: ActivityType, now: Long) {
        lastFinishedReps = reps
        current = type
        votes = 0
        offVotes = 0
        candidate = ActivityType.STILL
        // Reps done while the detector was still making up its mind are not lost.
        reps = when (type) {
            ActivityType.SQUAT -> squatReps.times.count { now - it <= DetectorConfig.WINDOW_MS }
            ActivityType.JUMP -> jumpReps.times.count { now - it <= DetectorConfig.WINDOW_MS }
            else -> 0
        }
    }

    private fun intervalCv(times: ArrayDeque<Long>): Float {
        if (times.size < 3) return 1f
        val iv = times.zipWithNext { a, b -> (b - a).toFloat() }
        val mean = iv.average().toFloat()
        if (mean <= 0f) return 1f
        val sd = sqrt(iv.map { (it - mean) * (it - mean) }.average()).toFloat()
        return sd / mean
    }
}
