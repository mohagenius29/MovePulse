package com.movepulse.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("movepulse", Context.MODE_PRIVATE)

    var weightKg: Float
        get() = sp.getFloat("weight", 70f)
        set(v) = sp.edit().putFloat("weight", v).apply()

    var dailyGoal: Int
        get() = sp.getInt("goal", 8000)
        set(v) = sp.edit().putInt("goal", v).apply()

    var inactivityMinutes: Int
        get() = sp.getInt("idle_min", 45)
        set(v) = sp.edit().putInt("idle_min", v).apply()

    var lastMilestoneDate: String
        get() = sp.getString("ms_date", "") ?: ""
        set(v) = sp.edit().putString("ms_date", v).apply()

    var lastMilestoneLevel: Int
        get() = sp.getInt("ms_level", 0)
        set(v) = sp.edit().putInt("ms_level", v).apply()

    var goalNotifiedDate: String
        get() = sp.getString("goal_date", "") ?: ""
        set(v) = sp.edit().putString("goal_date", v).apply()

    var lastInactivityNotify: Long
        get() = sp.getLong("idle_ts", 0L)
        set(v) = sp.edit().putLong("idle_ts", v).apply()
}
