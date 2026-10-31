package com.example.tracker.domain

/**
 * Read-only sound checks shown in parent settings (CLAUDE.md 7): alarm volume, Do Not Disturb and
 * battery optimization. Pure; the phone's values come through [Source], so tests use a fake.
 */
object SoundStatus {

    /** What the phone reports right now. */
    interface Source {
        val alarmVolume: Int
        val alarmVolumeMax: Int
        /** `NotificationManager.getCurrentInterruptionFilter()`: one of the FILTER_ values. */
        val interruptionFilter: Int
        /** `NotificationManager.Policy.priorityCategories` of the policy in force. */
        val priorityCategories: Int
        val ignoresBatteryOptimizations: Boolean
    }

    // The documented values of NotificationManager's constants (checked against Android in a test).
    const val FILTER_UNKNOWN = 0
    const val FILTER_ALL = 1
    const val FILTER_PRIORITY = 2
    const val FILTER_NONE = 3
    const val FILTER_ALARMS = 4
    const val PRIORITY_CATEGORY_ALARMS = 1 shl 5

    enum class Level { OK, WARNING }

    enum class Text {
        VOLUME_LEVEL, VOLUME_ZERO,
        DND_OFF, DND_ALLOWS_ALARMS, DND_BLOCKS_ALARMS,
        BATTERY_UNRESTRICTED, BATTERY_OPTIMIZED,
    }

    /** One settings row: its words, whether it warns, and whether its settings button shows. */
    data class Row(val text: Text, val level: Level, val showButton: Boolean)

    data class Report(val volume: Row, val doNotDisturb: Row, val battery: Row, val showSilentNote: Boolean)

    fun check(source: Source, alarmSoundOn: Boolean): Report {
        val volume = if (source.alarmVolume <= 0) {
            Row(Text.VOLUME_ZERO, Level.WARNING, showButton = true)
        } else {
            Row(Text.VOLUME_LEVEL, Level.OK, showButton = false)
        }
        val dnd = when (doNotDisturb(source.interruptionFilter, source.priorityCategories)) {
            Dnd.OFF -> Row(Text.DND_OFF, Level.OK, showButton = false)
            Dnd.ALARMS_ALLOWED -> Row(Text.DND_ALLOWS_ALARMS, Level.OK, showButton = false)
            Dnd.ALARMS_BLOCKED -> Row(Text.DND_BLOCKS_ALARMS, Level.WARNING, showButton = true)
        }
        // The battery button was always there (CLAUDE.md 7); the row now also says the state.
        val battery = if (source.ignoresBatteryOptimizations) {
            Row(Text.BATTERY_UNRESTRICTED, Level.OK, showButton = true)
        } else {
            Row(Text.BATTERY_OPTIMIZED, Level.WARNING, showButton = true)
        }
        return Report(volume, dnd, battery, showSilentNote = alarmSoundOn)
    }

    enum class Dnd { OFF, ALARMS_ALLOWED, ALARMS_BLOCKED }

    /** Total silence blocks alarms; priority mode lets them through only when the policy allows alarms. */
    fun doNotDisturb(filter: Int, priorityCategories: Int): Dnd = when (filter) {
        FILTER_PRIORITY ->
            if (priorityCategories and PRIORITY_CATEGORY_ALARMS != 0) Dnd.ALARMS_ALLOWED else Dnd.ALARMS_BLOCKED
        FILTER_ALARMS -> Dnd.ALARMS_ALLOWED
        FILTER_NONE -> Dnd.ALARMS_BLOCKED
        else -> Dnd.OFF
    }
}
