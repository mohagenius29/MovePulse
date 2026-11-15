package com.movepulse.ui

import com.movepulse.data.HourStat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

data class Insight(val emoji: String, val text: String)

data class Totals(
    val steps: Int,
    val kcal: Int,
    val activeMin: Int,
    val walkMin: Int,
    val jogMin: Int,
    val runMin: Int,
    val moveMin: Int,
    val squatMin: Int,
    val jumpMin: Int,
    val danceMin: Int,
)

fun List<HourStat>.totals() = Totals(
    steps = sumOf { it.steps },
    kcal = sumOf { it.calories.toDouble() }.roundToInt(),
    activeMin = sumOf { it.activeSeconds } / 60,
    walkMin = sumOf { it.walkSeconds } / 60,
    jogMin = sumOf { it.jogSeconds } / 60,
    runMin = sumOf { it.runSeconds } / 60,
    moveMin = sumOf { it.moveSeconds } / 60,
    squatMin = sumOf { it.squatSeconds } / 60,
    jumpMin = sumOf { it.jumpSeconds } / 60,
    danceMin = sumOf { it.danceSeconds } / 60,
)

private fun hourLabel(h: Int): String {
    val suffix = if (h < 12) "am" else "pm"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12$suffix"
}

/** Plain-language analysis built from the last 7 days of hourly stats. */
fun buildInsights(week: List<HourStat>, goal: Int): List<Insight> {
    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    val todayRows = week.filter { it.date == today }
    val result = mutableListOf<Insight>()

    val todaySteps = todayRows.sumOf { it.steps }
    val pct = if (goal > 0) (todaySteps * 100f / goal).roundToInt() else 0
    result += Insight("🎯", "You are at $pct% of your $goal step goal today.")

    val yesterdaySteps = week.filter { it.date == yesterday }.sumOf { it.steps }
    if (yesterdaySteps > 0) {
        val diff = todaySteps - yesterdaySteps
        result += Insight(
            if (diff >= 0) "📈" else "📉",
            if (diff >= 0) "That's ${diff} more steps than yesterday at this point."
            else "You're ${-diff} steps behind yesterday's total. A short walk closes the gap."
        )
    }

    todayRows.maxByOrNull { it.activeSeconds }?.takeIf { it.activeSeconds > 0 }?.let {
        result += Insight("⏰", "Your most active hour today was ${hourLabel(it.hour)} (${it.activeSeconds / 60} active min).")
    }

    val ex = todayRows.totals()
    if (ex.squatMin + ex.jumpMin + ex.danceMin > 0) {
        result += Insight(
            "🏋️",
            "Auto-detected today: ${ex.squatMin} min squats, ${ex.jumpMin} min jumping jacks, ${ex.danceMin} min dancing."
        )
    }

    val nowHour = LocalDateTime.now().hour
    if (nowHour >= 9) {
        val lazyHours = (8..minOf(nowHour, 20)).count { h ->
            (todayRows.firstOrNull { it.hour == h }?.activeSeconds ?: 0) < 120
        }
        if (lazyHours >= 3) {
            result += Insight("🪑", "$lazyHours daytime hours had under 2 active minutes. Try a movement break each hour.")
        }
    }

    val perDay = week.groupBy { it.date }.mapValues { (_, rows) -> rows.sumOf { it.steps } }
    if (perDay.size >= 2) {
        val avg = perDay.values.average().roundToInt()
        val best = perDay.maxByOrNull { it.value }!!
        val bestName = LocalDate.parse(best.key).dayOfWeek
            .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
        result += Insight("🏆", "Your 7-day average is $avg steps. Best day: $bestName with ${best.value}.")
    }

    return result
}
