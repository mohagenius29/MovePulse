package com.movepulse.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movepulse.data.AppDatabase
import com.movepulse.data.ExerciseSession
import com.movepulse.data.HourStat
import com.movepulse.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).statsDao()
    val prefs = Prefs(app)

    private val today = LocalDate.now().toString()
    private val weekStart = LocalDate.now().minusDays(6).toString()

    val todayHours = dao.hoursForDay(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val week = dao.since(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val route = dao.routeFor(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions = dao.sessionsFor(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Saves a manually timed workout and adds its calories to today's stats. */
    fun finishManual(name: String, met: Float, seconds: Int) {
        if (seconds < 10) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = LocalDateTime.now()
            val date = now.toLocalDate().toString()
            val kcal = met * 3.5f * prefs.weightKg / 200f * (seconds / 60f)
            dao.insertSession(
                ExerciseSession(
                    date = date, startTs = System.currentTimeMillis() - seconds * 1000L, name = name,
                    reps = 0, durationSec = seconds, kcal = kcal, auto = false,
                )
            )
            val cur = dao.get(date, now.hour) ?: HourStat(date = date, hour = now.hour)
            dao.upsert(cur.copy(calories = cur.calories + kcal, moveSeconds = cur.moveSeconds + seconds))
        }
    }

    var weightKg by mutableFloatStateOf(prefs.weightKg)
        private set

    fun updateWeight(kg: Float) {
        if (kg in 20f..300f) {
            prefs.weightKg = kg
            weightKg = kg
        }
    }
}
