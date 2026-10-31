package com.example.tracker.ui

import androidx.annotation.DrawableRes
import com.example.tracker.R
import com.example.tracker.domain.AgeGroup
import com.example.tracker.domain.Gender

/** All pictures that are not icons, in one place: the mascot and the emoji. They are language neutral. */
object Mascots {
    /**
     * The capsule mascot (vector, palette colors): CHILD streak ended (sleepy, then happy to start
     * again) and the empty state (happy). The CHILD and TEEN rewards show the celebration avatar
     * picture instead (batch 6, R3); ADULT keeps its calm style.
     */
    @DrawableRes val ART_HAPPY = R.drawable.mascot_happy
    @DrawableRes val ART_SLEEPY = R.drawable.mascot_sleepy

    const val TEEN_RESTART = "🔄"
    const val ADULT_PAUSE = "⏸"
    const val PILL = "💊"
    const val TAKEN = "✅"
    const val MISSED = "⏰"
    const val STREAK = "🔥"
    const val POINTS = "⭐"
    const val LEVEL = "🏆"

    /**
     * Avatar per (age group, gender), so the same name at different ages is easy to tell apart.
     * Gender only picks the picture; null (not set) gives the neutral one.
     */
    private val AVATARS: Map<AgeGroup, Map<Gender?, String>> = mapOf(
        AgeGroup.CHILD to mapOf(Gender.MALE to "👦", Gender.FEMALE to "👧", null to "🧒"),
        AgeGroup.TEEN to mapOf(Gender.MALE to "👨‍🎓", Gender.FEMALE to "👩‍🎓", null to "🧑‍🎓"),
        AgeGroup.ADULT to mapOf(Gender.MALE to "👨", Gender.FEMALE to "👩", null to "🧑"),
    )

    fun avatar(group: AgeGroup, gender: Gender?): String = AVATARS.getValue(group).getValue(gender)
}
