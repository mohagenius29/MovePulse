package com.movepulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movepulse.data.HourStat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalysisScreen(todayHours: List<HourStat>, week: List<HourStat>, goal: Int) {
    var showWeek by remember { mutableStateOf(false) }
    val rows = if (showWeek) week else todayHours
    val t = rows.totals()
    val primary = MaterialTheme.colorScheme.primary

    val values: List<Float>
    val labels: List<String>
    if (showWeek) {
        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        values = days.map { d -> week.filter { it.date == d.toString() }.sumOf { it.steps }.toFloat() }
        labels = days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    } else {
        values = (0..23).map { h -> (todayHours.firstOrNull { it.hour == h }?.steps ?: 0).toFloat() }
        labels = (0..23).map { if (it % 6 == 0) "${it}h" else "" }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Activity analysis", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = primary)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showWeek, onClick = { showWeek = false }, label = { Text("Today by hour") })
            FilterChip(selected = showWeek, onClick = { showWeek = true }, label = { Text("Last 7 days") })
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Steps", "${t.steps}", Modifier.weight(1f))
            Tile("Calories", "${t.kcal}", Modifier.weight(1f))
            Tile("Active min", "${t.activeMin}", Modifier.weight(1f))
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (showWeek) "Steps per day" else "Steps per hour", fontWeight = FontWeight.SemiBold)
                BarChart(values, labels, primary)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Activity mix", fontWeight = FontWeight.SemiBold)
                LegendRow(Color(0xFF66BB6A), "Walking", t.walkMin)
                LegendRow(Color(0xFFFFA726), "Jogging", t.jogMin)
                LegendRow(Color(0xFFEF5350), "Running", t.runMin)
                LegendRow(Color(0xFF42A5F5), "Other movement", t.moveMin)
            }
        }

        val insights = buildInsights(week, goal)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Insights", fontWeight = FontWeight.SemiBold)
                insights.forEach { Text("${it.emoji}  ${it.text}") }
            }
        }
    }
}

@Composable
private fun Tile(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
