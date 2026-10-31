package com.example.tracker

import com.example.tracker.domain.SettingsAction
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which settings actions need the parent PIN (CLAUDE.md 8). Opening settings itself never does. */
class SettingsActionTest {

    @Test
    fun exactlyTheSensitiveActionsNeedThePin() {
        assertEquals(
            setOf(
                SettingsAction.TOGGLE_PIN_ON_CONFIRM,
                SettingsAction.CHANGE_PIN,
                SettingsAction.ALARM_SOUND,
                SettingsAction.CELEBRATION_SOUND,
                SettingsAction.MANAGE_PATIENTS,
                SettingsAction.ERASE_ALL,
            ),
            SettingsAction.entries.filter { it.needsPin }.toSet(),
        )
    }

    @Test
    fun languagePermissionsSoundChecksAndTestReminderAreFree() {
        assertEquals(
            setOf(
                SettingsAction.LANGUAGE,
                SettingsAction.FIX_NOTIFICATIONS,
                SettingsAction.FIX_EXACT_ALARMS,
                SettingsAction.BATTERY_SETTINGS,
                SettingsAction.SOUND_SETTINGS,
                SettingsAction.DND_SETTINGS,
                SettingsAction.TEST_REMINDER,
                SettingsAction.ABOUT,
            ),
            SettingsAction.entries.filterNot { it.needsPin }.toSet(),
        )
    }
}
