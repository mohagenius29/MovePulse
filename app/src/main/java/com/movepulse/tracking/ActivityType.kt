package com.movepulse.tracking

/** MET values come from the Compendium of Physical Activities (approximate). */
enum class ActivityType(val label: String, val met: Float) {
    STILL("Still", 0f),
    MOVE("Moving", 2.5f),
    WALK("Walking", 3.5f),
    JOG("Jogging", 7.0f),
    RUN("Running", 9.8f),
    SQUAT("Squats", 5.0f),
    JUMP("Jumping jacks", 8.0f),
    DANCE("Dancing", 5.0f);

    val isOutdoorRoute: Boolean get() = this == WALK || this == JOG || this == RUN
    val isExercise: Boolean get() = this == SQUAT || this == JUMP || this == DANCE
    val countsReps: Boolean get() = this == SQUAT || this == JUMP
}

object CalorieCalculator {
    /** kcal burned over [seconds] at this activity: MET * 3.5 * kg / 200 per minute. */
    fun kcal(type: ActivityType, weightKg: Float, seconds: Int): Float {
        if (type == ActivityType.STILL) return 0f
        return type.met * 3.5f * weightKg / 200f * (seconds / 60f)
    }
}
