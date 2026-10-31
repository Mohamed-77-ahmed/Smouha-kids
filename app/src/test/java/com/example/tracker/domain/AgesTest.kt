package com.example.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AgesTest {

    private fun day(iso: String) = LocalDate.parse(iso).toEpochDay()

    @Test
    fun ageGroupBoundaries() {
        // CHILD 0 to 12, TEEN 13 to 18, ADULT 19 and over (batch 3).
        assertEquals(AgeGroup.CHILD, AgeGroup.of(0))
        assertEquals(AgeGroup.CHILD, AgeGroup.of(12))
        assertEquals(AgeGroup.TEEN, AgeGroup.of(13))
        assertEquals(AgeGroup.TEEN, AgeGroup.of(17))
        assertEquals(AgeGroup.TEEN, AgeGroup.of(18))
        assertEquals(AgeGroup.ADULT, AgeGroup.of(19))
        assertEquals(AgeGroup.ADULT, AgeGroup.of(120))
    }

    @Test
    fun twelveYearOldBecomesTeenExactlyOneYearLater() {
        val created = day("2025-03-10")
        assertEquals(12, Ages.currentAge(12, created, day("2026-03-09")))
        assertEquals(AgeGroup.CHILD, AgeGroup.of(Ages.currentAge(12, created, day("2026-03-09"))))
        assertEquals(13, Ages.currentAge(12, created, day("2026-03-10")))
        assertEquals(AgeGroup.TEEN, AgeGroup.of(Ages.currentAge(12, created, day("2026-03-10"))))
    }

    @Test
    fun seventeenStaysTeenAtEighteen() {
        val created = day("2020-06-01")
        assertEquals(17, Ages.currentAge(17, created, day("2021-05-31")))
        assertEquals(18, Ages.currentAge(17, created, day("2021-06-01")))
        assertEquals(AgeGroup.TEEN, AgeGroup.of(Ages.currentAge(17, created, day("2021-06-01"))))
    }

    @Test
    fun eighteenBecomesAdultExactlyOneYearLater() {
        val created = day("2025-09-15")
        assertEquals(AgeGroup.TEEN, AgeGroup.of(Ages.currentAge(18, created, day("2026-09-14"))))
        assertEquals(19, Ages.currentAge(18, created, day("2026-09-15")))
        assertEquals(AgeGroup.ADULT, AgeGroup.of(Ages.currentAge(18, created, day("2026-09-15"))))
    }

    @Test
    fun leapDayAnchorCountsFullYearsOnly() {
        val created = day("2024-02-29")
        assertEquals(5, Ages.currentAge(5, created, day("2025-02-28")))
        assertEquals(6, Ages.currentAge(5, created, day("2025-03-01")))
    }

    @Test
    fun clockBeforeAnchorKeepsAnchoredAge() {
        assertEquals(9, Ages.currentAge(9, day("2026-10-16"), day("2026-01-01")))
    }

    @Test
    fun severalYearsAccumulate() {
        assertEquals(47, Ages.currentAge(40, day("2019-10-16"), day("2026-10-16")))
    }
}
