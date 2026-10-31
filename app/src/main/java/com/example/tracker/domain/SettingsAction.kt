package com.example.tracker.domain

/**
 * Everything a parent can do inside parent settings, and whether it needs the parent PIN
 * (CLAUDE.md 8). Opening settings itself needs no PIN. The settings screen routes every action
 * through this table, so it is the single place that decides.
 */
enum class SettingsAction(val needsPin: Boolean) {
    TOGGLE_PIN_ON_CONFIRM(true),
    CHANGE_PIN(true),
    ALARM_SOUND(true),
    CELEBRATION_SOUND(true),
    MANAGE_PATIENTS(true),
    ERASE_ALL(true),
    LANGUAGE(false),
    FIX_NOTIFICATIONS(false),
    FIX_EXACT_ALARMS(false),
    BATTERY_SETTINGS(false),
    SOUND_SETTINGS(false),
    DND_SETTINGS(false),
    TEST_REMINDER(false),
    ABOUT(false),
}
