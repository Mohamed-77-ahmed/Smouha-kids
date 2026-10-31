package com.example.tracker.domain

import kotlin.math.abs

enum class DoseStatus { PENDING, TAKEN, MISSED }

/** Points for one confirmed dose. */
data class DosePoints(val base: Int, val onTimeBonus: Int) {
    val total: Int get() = base + onTimeBonus
}

object PointsRules {
    const val DOSE_POINTS = 10
    const val ON_TIME_BONUS = 5
    const val ON_TIME_WINDOW_SECONDS = 30 * 60
    const val PERFECT_DAY_BONUS = 20
    const val STREAK_MILESTONE_BONUS = 50
    val STREAK_MILESTONES: List<Int> = listOf(3, 7, 14, 30, 60, 100)
    const val POINTS_PER_LEVEL = 100

    /** Level = floor(points / 100) + 1. */
    fun level(totalPoints: Int): Int = maxOf(totalPoints, 0) / POINTS_PER_LEVEL + 1

    fun pointsToNextLevel(totalPoints: Int): Int =
        POINTS_PER_LEVEL - maxOf(totalPoints, 0) % POINTS_PER_LEVEL

    /**
     * +10 per confirmed pill, +5 when confirmed within 30 minutes (inclusive) before or after
     * the scheduled time. Both times are local times of the dose's own day.
     */
    fun dosePoints(scheduledMinuteOfDay: Int, confirmedSecondOfDay: Int): DosePoints {
        val diff = abs(confirmedSecondOfDay - scheduledMinuteOfDay * 60)
        return DosePoints(DOSE_POINTS, if (diff <= ON_TIME_WINDOW_SECONDS) ON_TIME_BONUS else 0)
    }

    fun milestoneFor(runLength: Int): Int? = runLength.takeIf { it in STREAK_MILESTONES }
}

enum class ConfirmCheck { OK, NOT_PENDING, EXPIRED, NOT_YET }

object DoseRules {
    /** A dose can be confirmed only while PENDING and only on its own scheduled day (until 23:59). */
    fun canConfirm(status: DoseStatus, doseEpochDay: Long, todayEpochDay: Long): ConfirmCheck = when {
        status != DoseStatus.PENDING -> ConfirmCheck.NOT_PENDING
        doseEpochDay < todayEpochDay -> ConfirmCheck.EXPIRED
        doseEpochDay > todayEpochDay -> ConfirmCheck.NOT_YET
        else -> ConfirmCheck.OK
    }
}

enum class DayResult { PERFECT, MISSED, NEUTRAL }

/**
 * Finalized streak state of one patient. [currentStreak] counts perfect days up to and
 * including [lastEvaluatedEpochDay]; today is added live once it is complete.
 */
data class StreakState(
    val currentStreak: Int,
    val streakStartEpochDay: Long?,
    val bestStreak: Int,
    val lastEvaluatedEpochDay: Long,
    /** Length of a streak that broke and whose animation was not shown yet, else null. */
    val brokenStreakPending: Int?,
)

object StreakRules {

    /** Doses of a day that is over: no doses = neutral, all taken = perfect, anything else = missed. */
    fun dayResult(statuses: List<DoseStatus>): DayResult = when {
        statuses.isEmpty() -> DayResult.NEUTRAL
        statuses.all { it == DoseStatus.TAKEN } -> DayResult.PERFECT
        else -> DayResult.MISSED
    }

    /** Today is complete when it has at least one dose and every dose is taken. */
    fun isComplete(statuses: List<DoseStatus>): Boolean =
        statuses.isNotEmpty() && statuses.all { it == DoseStatus.TAKEN }

    /** Applies one finished day. Days must be applied in order without gaps. */
    fun finalizeDay(state: StreakState, epochDay: Long, result: DayResult): StreakState {
        require(epochDay == state.lastEvaluatedEpochDay + 1) {
            "Day $epochDay applied after ${state.lastEvaluatedEpochDay}"
        }
        return when (result) {
            DayResult.PERFECT -> {
                val streak = state.currentStreak + 1
                state.copy(
                    currentStreak = streak,
                    streakStartEpochDay = if (state.currentStreak == 0) epochDay else state.streakStartEpochDay,
                    bestStreak = maxOf(state.bestStreak, streak),
                    lastEvaluatedEpochDay = epochDay,
                )
            }
            DayResult.MISSED -> state.copy(
                currentStreak = 0,
                streakStartEpochDay = null,
                lastEvaluatedEpochDay = epochDay,
                // Several missed days in a row keep one pending animation for the streak that broke.
                brokenStreakPending = if (state.currentStreak > 0) state.currentStreak else state.brokenStreakPending,
            )
            DayResult.NEUTRAL -> state.copy(lastEvaluatedEpochDay = epochDay)
        }
    }

    /** True when [todayEpochDay] is not finalized yet, so it may still count and earn bonuses. */
    fun isOpenDay(state: StreakState, todayEpochDay: Long): Boolean =
        todayEpochDay > state.lastEvaluatedEpochDay

    /** Streak shown to the patient: finalized days plus today when today is already complete. */
    fun liveStreak(state: StreakState, todayEpochDay: Long, todayComplete: Boolean): Int =
        if (todayComplete && isOpenDay(state, todayEpochDay)) state.currentStreak + 1 else state.currentStreak

    /** First day of the run that today belongs to if today completes. */
    fun runStartIfComplete(state: StreakState, todayEpochDay: Long): Long =
        if (state.currentStreak == 0) todayEpochDay else state.streakStartEpochDay ?: todayEpochDay
}
