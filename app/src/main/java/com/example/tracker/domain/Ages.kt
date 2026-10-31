package com.example.tracker.domain

import java.time.LocalDate
import java.time.Period

/**
 * Reward and animation style. Derived from the current age, never stored. CHILD 0 to 12, TEEN 13
 * to 18 (the hospital treats patients up to 18), ADULT 19 and over.
 */
enum class AgeGroup {
    CHILD, TEEN, ADULT;

    companion object {
        const val CHILD_MAX_AGE = 12
        const val TEEN_MAX_AGE = 18

        fun of(age: Int): AgeGroup = when {
            age <= CHILD_MAX_AGE -> CHILD
            age <= TEEN_MAX_AGE -> TEEN
            else -> ADULT
        }
    }
}

object Ages {
    const val MIN_AGE = 0
    const val MAX_AGE = 120

    /**
     * Age today = age given by the parent + full years since that day, so a 12 year old
     * automatically becomes a TEEN a year later. A clock set before the anchor day keeps the
     * anchored age instead of going negative.
     */
    fun currentAge(ageAtCreation: Int, createdAtEpochDay: Long, todayEpochDay: Long): Int {
        if (todayEpochDay <= createdAtEpochDay) return ageAtCreation
        val years = Period.between(
            LocalDate.ofEpochDay(createdAtEpochDay),
            LocalDate.ofEpochDay(todayEpochDay),
        ).years
        return ageAtCreation + years
    }
}
