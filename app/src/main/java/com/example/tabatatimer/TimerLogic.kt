package com.example.tabatatimer

internal enum class TimerPhase {
    PREPARE,
    WORK,
    REST,
    CYCLE_REST,
    FINISHED
}

internal data class TimerPosition(
    val phase: TimerPhase,
    val secondsLeft: Long,
    val round: Int,
    val cycle: Int
)

internal fun workoutDurationSeconds(
    workout: Workout
): Long {
    return workout.prepareSeconds.toLong() +
            workout.workSeconds.toLong() *
            workout.rounds * workout.cycles +
            workout.restSeconds.toLong() *
            (workout.rounds - 1) * workout.cycles +
            workout.cycleRestSeconds.toLong() *
            (workout.cycles - 1)
}

internal fun timerPosition(
    workout: Workout,
    elapsedMillis: Long
): TimerPosition {
    val elapsedSeconds = elapsedMillis / 1000
    val total = workoutDurationSeconds(workout)

    if (elapsedSeconds >= total) {
        return TimerPosition(
            phase = TimerPhase.FINISHED,
            secondsLeft = 0,
            round = workout.rounds,
            cycle = workout.cycles
        )
    }

    if (elapsedSeconds < workout.prepareSeconds) {
        return TimerPosition(
            phase = TimerPhase.PREPARE,
            secondsLeft =
                workout.prepareSeconds - elapsedSeconds,
            round = 1,
            cycle = 1
        )
    }

    val cycleDuration =
        workout.workSeconds.toLong() * workout.rounds +
                workout.restSeconds.toLong() * (workout.rounds - 1)

    val cycleBlock =
        cycleDuration + workout.cycleRestSeconds

    val afterPrepare =
        elapsedSeconds - workout.prepareSeconds

    val cycleIndex = (
            afterPrepare / cycleBlock
            ).coerceAtMost((workout.cycles - 1).toLong())

    val withinCycle =
        afterPrepare - cycleIndex * cycleBlock

    if (withinCycle >= cycleDuration) {
        return TimerPosition(
            phase = TimerPhase.CYCLE_REST,
            secondsLeft = cycleBlock - withinCycle,
            round = workout.rounds,
            cycle = cycleIndex.toInt() + 1
        )
    }

    val roundBlock =
        workout.workSeconds.toLong() + workout.restSeconds

    val roundIndex = (
            withinCycle / roundBlock
            ).coerceAtMost((workout.rounds - 1).toLong())

    val withinRound =
        withinCycle - roundIndex * roundBlock

    return if (withinRound < workout.workSeconds) {
        TimerPosition(
            phase = TimerPhase.WORK,
            secondsLeft = workout.workSeconds - withinRound,
            round = roundIndex.toInt() + 1,
            cycle = cycleIndex.toInt() + 1
        )
    } else {
        TimerPosition(
            phase = TimerPhase.REST,
            secondsLeft = roundBlock - withinRound,
            round = roundIndex.toInt() + 1,
            cycle = cycleIndex.toInt() + 1
        )
    }
}

internal fun upcomingTimerPositions(
    workout: Workout,
    elapsedMillis: Long
): List<TimerPosition> {
    val upcoming = mutableListOf<TimerPosition>()

    var position = timerPosition(
        workout = workout,
        elapsedMillis = elapsedMillis
    )

    var boundarySeconds = elapsedMillis / 1000

    repeat(1) {
        if (position.phase != TimerPhase.FINISHED) {
            boundarySeconds += position.secondsLeft

            position = timerPosition(
                workout = workout,
                elapsedMillis = boundarySeconds * 1000
            )

            if (position.phase != TimerPhase.FINISHED) {
                upcoming.add(position)
            }
        }
    }

    return upcoming
}