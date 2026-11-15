package com.movepulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movepulse.data.ExerciseSession
import com.movepulse.tracking.ActivityType
import com.movepulse.tracking.LiveState
import com.movepulse.tracking.MotivationEngine

@Composable
fun TodayScreen(
    vm: MainViewModel,
    hoursState: List<com.movepulse.data.HourStat>,
    sessions: List<ExerciseSession>,
) {
    val live by LiveState.current.collectAsStateWithLifecycle()
    val t = hoursState.totals()
    val goal = vm.prefs.dailyGoal
    val quote = remember { MotivationEngine.quote() }
    var showWeight by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("MovePulse", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = if (goal > 0) t.steps / goal.toFloat() else 0f,
                color = MaterialTheme.colorScheme.primary,
                track = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(220.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${t.steps}", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text("of $goal steps")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("🔥 Calories", "${t.kcal} kcal", Modifier.weight(1f))
            StatCard("⏱ Active", "${t.activeMin} min", Modifier.weight(1f))
        }

        if (live.type != ActivityType.STILL) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "Now: ${live.type.label}" + if (live.type.countsReps) "  x ${live.reps}" else "",
                    Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (sessions.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Workouts today", fontWeight = FontWeight.SemiBold)
                    sessions.forEach {
                        val reps = if (it.reps > 0) "${it.reps} reps · " else ""
                        val tag = if (it.auto) " (auto)" else ""
                        Text("${it.name}$tag: $reps${it.durationSec / 60} min ${it.durationSec % 60}s · ${it.kcal.toInt()} kcal")
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Text("\"$quote\"", Modifier.padding(16.dp), fontSize = 16.sp)
        }

        TextButton(onClick = { showWeight = true }) {
            Text("Weight: ${"%.0f".format(vm.weightKg)} kg (tap to change, used for calories)")
        }
        Text(
            "Keep the app running in the background. Notifications will cheer you on and nudge you when you've been still.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showWeight) {
        var text by remember { mutableStateOf("%.0f".format(vm.weightKg)) }
        AlertDialog(
            onDismissRequest = { showWeight = false },
            title = { Text("Your weight (kg)") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    text.toFloatOrNull()?.let { vm.updateWeight(it) }
                    showWeight = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showWeight = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 13.sp)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
