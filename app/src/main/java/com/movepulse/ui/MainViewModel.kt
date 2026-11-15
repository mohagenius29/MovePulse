package com.movepulse.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movepulse.data.AppDatabase
import com.movepulse.data.Prefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

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

    var weightKg by mutableFloatStateOf(prefs.weightKg)
        private set

    fun updateWeight(kg: Float) {
        if (kg in 20f..300f) {
            prefs.weightKg = kg
            weightKg = kg
        }
    }
}
