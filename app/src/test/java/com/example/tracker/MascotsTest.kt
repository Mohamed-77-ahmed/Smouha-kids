package com.example.tracker

import com.example.tracker.domain.AgeGroup
import com.example.tracker.domain.Gender
import com.example.tracker.ui.Mascots
import org.junit.Assert.assertEquals
import org.junit.Test

/** One avatar per (age group, gender), all different; not set = the neutral one of the group. */
class MascotsTest {

    @Test
    fun exactAvatarTable() {
        val table = AgeGroup.entries.associateWith { group ->
            listOf(Gender.MALE, Gender.FEMALE, null).map { Mascots.avatar(group, it) }
        }
        assertEquals(
            mapOf(
                AgeGroup.CHILD to listOf("👦", "👧", "🧒"),
                AgeGroup.TEEN to listOf("👨‍🎓", "👩‍🎓", "🧑‍🎓"),
                AgeGroup.ADULT to listOf("👨", "👩", "🧑"),
            ),
            table,
        )
        assertEquals(9, table.values.flatten().toSet().size)
    }
}
