package com.movepulse.tracking

import kotlin.random.Random

/** Message bank. Placeholders: {steps} {kcal} {goal} {min} */
object MotivationEngine {

    private val milestone = listOf(
        "{steps} steps! That's about {kcal} calories burned. Your body is thanking you 🔥",
        "You've walked {steps} steps and burned around {kcal} kcal. Unstoppable! 💪",
        "{steps} steps down, {kcal} calories gone. Legends are built one step at a time 🏆",
        "Boom! {steps} steps and {kcal} kcal burned. Keep that engine running ⚡",
        "{kcal} calories torched already! {steps} steps of pure momentum 🚀",
        "Look at you go: {steps} steps today. That's {kcal} kcal of effort 🌟",
        "{steps} steps. {kcal} kcal. Zero excuses. Keep moving! 🙌",
        "Every step counts and you've made {steps} of them, burning about {kcal} kcal. Proud of you! 👏",
    )

    private val running = listOf(
        "You're running! Heart pumping, calories flying. {steps} steps so far 🏃‍♂️💨",
        "Running mode! That's {kcal} kcal burned today. Feel the power! 🔥",
    )

    private val goalReached = listOf(
        "GOAL SMASHED! {steps} steps today, past your {goal} target 🎉 You're a champion!",
        "You did it! {goal} steps reached and about {kcal} kcal burned. Take a bow 🏅",
    )

    private val inactivity = listOf(
        "You've been still for {min} minutes. Stand up and stretch, your body will love it 🧘",
        "Time to move! A quick walk or 10 squats wakes up your whole body ⚡",
        "{min} minutes without movement. Let's do 20 jumping jacks, you've got this! 💪",
        "Your couch can wait. Two minutes of dancing to your favourite song? 💃",
        "A tiny break for big energy: walk around the house for 3 minutes 🚶",
    )

    private val quotes = listOf(
        "Small steps every day build a strong body.",
        "Movement is medicine. Take your dose today.",
        "You don't have to be extreme, just consistent.",
        "The best workout is the one you actually do.",
        "Your future self is cheering for you right now.",
    )

    private fun fill(t: String, steps: Int = 0, kcal: Int = 0, goal: Int = 0, min: Long = 0) =
        t.replace("{steps}", steps.toString())
            .replace("{kcal}", kcal.toString())
            .replace("{goal}", goal.toString())
            .replace("{min}", min.toString())

    fun milestone(steps: Int, kcal: Int, type: ActivityType): String {
        val pool = if (type == ActivityType.RUN) milestone + running else milestone
        return fill(pool.random(), steps = steps, kcal = kcal)
    }

    fun goalReached(steps: Int, kcal: Int, goal: Int) =
        fill(goalReached.random(), steps = steps, kcal = kcal, goal = goal)

    fun inactivity(minutes: Long) = fill(inactivity.random(), min = minutes)

    private val repLines = listOf(
        "{reps} {name} and counting! About {kcal} kcal burned. Your muscles are getting stronger 💪",
        "{reps} {name}! That's {kcal} calories torched, don't stop now 🔥",
        "Machine mode: {reps} {name} done, {kcal} kcal gone ⚡",
        "{reps} {name}! Every rep is a vote for the person you're becoming 🌟",
    )

    private val sessionLines = listOf(
        "{name} done: {detail}about {kcal} kcal burned. That's how champions train 🏆",
        "Great session! {name}, {detail}{kcal} kcal burned. Rest, hydrate, repeat 💧",
        "{name} complete. {detail}You burned around {kcal} calories. Proud of you! 👏",
    )

    fun repMilestone(type: ActivityType, reps: Int, kcal: Int) =
        repLines.random()
            .replace("{reps}", reps.toString())
            .replace("{name}", type.label.lowercase())
            .replace("{kcal}", kcal.toString())

    fun sessionDone(type: ActivityType, reps: Int, seconds: Int, kcal: Int): String {
        val mins = seconds / 60
        val time = if (mins >= 1) "$mins min" else "$seconds s"
        val detail = if (reps > 0) "$reps reps in $time, " else "$time, "
        return sessionLines.random()
            .replace("{name}", type.label)
            .replace("{detail}", detail)
            .replace("{kcal}", kcal.toString())
    }

    fun quote() = quotes[Random.nextInt(quotes.size)]
}
