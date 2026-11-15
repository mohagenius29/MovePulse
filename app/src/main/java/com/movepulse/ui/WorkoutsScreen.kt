package com.movepulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movepulse.tracking.ActivityType
import com.movepulse.tracking.LiveState
import kotlinx.coroutines.delay

/** [auto] = MovePulse detects and counts it by itself; otherwise you start/finish the timer. */
data class Workout(
    val name: String,
    val emoji: String,
    val level: String,
    val minutes: Int,
    val met: Float,
    val auto: Boolean,
    val steps: List<String>,
    val tip: String,
)

val WORKOUTS = listOf(
    Workout(
        "Squats", "🦵", "Beginner", 5, 5.0f, true,
        listOf("Put the phone in your front pocket", "Feet shoulder-width apart", "Lower until thighs are near parallel", "Stand tall and repeat"),
        "Knees over toes, chest up. Try 3 sets of 12.",
    ),
    Workout(
        "Jumping jacks", "⭐", "Beginner", 5, 8.0f, true,
        listOf("Phone in your pocket", "Jump feet wide while raising arms overhead", "Jump back to start", "Keep a steady rhythm"),
        "Land softly on the balls of your feet. 3 rounds of 30.",
    ),
    Workout(
        "Dance party", "💃", "All levels", 10, 5.0f, true,
        listOf("Phone in your pocket or armband", "Play your favourite songs", "Move your whole body, no rules"),
        "Great cardio that doesn't feel like exercise.",
    ),
    Workout(
        "March in place", "🚶", "Beginner", 10, 3.5f, true,
        listOf("Phone in your pocket", "Lift knees to hip height", "Swing your arms"),
        "Steps are counted automatically. Perfect while watching TV.",
    ),
    Workout(
        "High knees", "🏃", "Intermediate", 5, 8.0f, true,
        listOf("Phone in your pocket", "Run in place, driving knees high", "Stay light on your feet"),
        "Do 30 seconds on, 30 seconds off.",
    ),
    Workout(
        "Lunges", "🏋", "Beginner", 8, 4.0f, false,
        listOf("Step forward and lower both knees to 90 degrees", "Push back to standing", "Alternate legs"),
        "Keep your front knee behind your toes.",
    ),
    Workout(
        "Push-ups", "💪", "Intermediate", 5, 3.8f, false,
        listOf("Hands under shoulders, body in a straight line", "Lower chest toward the floor", "Press back up"),
        "Do them on your knees to start. Quality over quantity.",
    ),
    Workout(
        "Plank", "🧱", "Beginner", 3, 3.0f, false,
        listOf("Forearms on the floor, elbows under shoulders", "Keep body straight from head to heels", "Breathe steadily"),
        "Start with 20-30 seconds and build up.",
    ),
    Workout(
        "Wall sit", "🪑", "Beginner", 3, 3.0f, false,
        listOf("Back against a wall", "Slide down until knees are at 90 degrees", "Hold"),
        "Keep your back flat and weight in your heels.",
    ),
    Workout(
        "Stretch & mobility", "🧘", "All levels", 10, 2.3f, false,
        listOf("Neck and shoulder rolls", "Hamstring and quad stretches", "Hip openers and gentle twists"),
        "Never bounce. Hold each stretch 20-30 seconds.",
    ),
)

@Composable
fun WorkoutsScreen(vm: MainViewModel) {
    var selected by remember { mutableStateOf<Workout?>(null) }
    val w = selected
    BackHandler(enabled = w != null) { selected = null }
    if (w == null) {
        WorkoutList { selected = it }
    } else {
        WorkoutDetail(w, vm) { selected = null }
    }
}

@Composable
private fun WorkoutList(onPick: (Workout) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Home workouts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("No equipment needed. Workouts marked AUTO are detected and counted for you.", fontSize = 13.sp)
        WORKOUTS.forEach { w ->
            Card(Modifier.fillMaxWidth().clickable { onPick(w) }) {
                Column(Modifier.padding(16.dp)) {
                    Text("${w.emoji} ${w.name}" + if (w.auto) "   AUTO" else "", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("${w.level} · about ${w.minutes} min", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun WorkoutDetail(w: Workout, vm: MainViewModel, onBack: () -> Unit) {
    val live by LiveState.current.collectAsStateWithLifecycle()
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        if (running) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("< Back") }
        Text("${w.emoji} ${w.name}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("${w.level} · about ${w.minutes} min")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                w.steps.forEachIndexed { i, s -> Text("${i + 1}. $s") }
                Text("Tip: ${w.tip}", fontWeight = FontWeight.SemiBold)
            }
        }

        Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 48.sp, fontWeight = FontWeight.Bold)

        if (w.auto) {
            val text = if (live.type != ActivityType.STILL) {
                "Detected: ${live.type.label}" + if (live.type.countsReps) "  x ${live.reps}" else ""
            } else {
                "Waiting for movement..."
            }
            Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(16.dp), fontSize = 18.sp) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { running = !running; finished = false }) {
                Text(if (running) "Pause" else if (seconds > 0) "Resume" else "Start")
            }
            if (seconds > 0 && !finished) {
                Button(onClick = {
                    running = false
                    finished = true
                    if (!w.auto) vm.finishManual(w.name, w.met, seconds)
                }) { Text("Finish") }
            }
        }

        if (finished) {
            Text(
                if (w.auto) "Nice work! Your reps and calories were tracked automatically. See the Today tab."
                else "Saved! ${w.name} for ${seconds / 60} min ${seconds % 60} s was added to your calories.",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
