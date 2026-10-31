package com.example.tracker.domain

import android.app.NotificationManager
import com.example.tracker.domain.SoundStatus.Level
import com.example.tracker.domain.SoundStatus.Row
import com.example.tracker.domain.SoundStatus.Text
import org.junit.Assert.assertEquals
import org.junit.Test

/** The read-only sound rows of parent settings, with a fake phone (CLAUDE.md 7). */
class SoundStatusTest {

    private data class FakePhone(
        override val alarmVolume: Int = 5,
        override val alarmVolumeMax: Int = 7,
        override val interruptionFilter: Int = SoundStatus.FILTER_ALL,
        override val priorityCategories: Int = 0,
        override val ignoresBatteryOptimizations: Boolean = true,
    ) : SoundStatus.Source

    private fun check(phone: FakePhone, alarmSoundOn: Boolean = true) = SoundStatus.check(phone, alarmSoundOn)

    @Test
    fun everythingFineShowsNoWarningAndNoFixButtons() {
        val report = check(FakePhone())
        assertEquals(Row(Text.VOLUME_LEVEL, Level.OK, showButton = false), report.volume)
        assertEquals(Row(Text.DND_OFF, Level.OK, showButton = false), report.doNotDisturb)
        assertEquals(Row(Text.BATTERY_UNRESTRICTED, Level.OK, showButton = true), report.battery)
    }

    @Test
    fun alarmVolumeZeroWarnsWithTheSoundSettingsButton() {
        assertEquals(Row(Text.VOLUME_ZERO, Level.WARNING, showButton = true), check(FakePhone(alarmVolume = 0)).volume)
        // Control: the lowest audible step is not a warning.
        assertEquals(Row(Text.VOLUME_LEVEL, Level.OK, showButton = false), check(FakePhone(alarmVolume = 1)).volume)
    }

    @Test
    fun doNotDisturbWarnsOnlyWhenItBlocksAlarms() {
        val alarmsAllowed = SoundStatus.PRIORITY_CATEGORY_ALARMS or 1 // plus reminders, an unrelated bit
        assertEquals(
            Row(Text.DND_ALLOWS_ALARMS, Level.OK, showButton = false),
            check(FakePhone(interruptionFilter = SoundStatus.FILTER_PRIORITY, priorityCategories = alarmsAllowed)).doNotDisturb,
        )
        assertEquals(
            Row(Text.DND_BLOCKS_ALARMS, Level.WARNING, showButton = true),
            check(FakePhone(interruptionFilter = SoundStatus.FILTER_PRIORITY, priorityCategories = 1)).doNotDisturb,
        )
        assertEquals(
            Row(Text.DND_BLOCKS_ALARMS, Level.WARNING, showButton = true),
            check(FakePhone(interruptionFilter = SoundStatus.FILTER_NONE, priorityCategories = alarmsAllowed)).doNotDisturb,
        )
        assertEquals(
            Row(Text.DND_ALLOWS_ALARMS, Level.OK, showButton = false),
            check(FakePhone(interruptionFilter = SoundStatus.FILTER_ALARMS)).doNotDisturb,
        )
        assertEquals(Text.DND_OFF, check(FakePhone(interruptionFilter = SoundStatus.FILTER_UNKNOWN)).doNotDisturb.text)
    }

    @Test
    fun batteryOptimizationWarnsAndTheButtonAlwaysShows() {
        assertEquals(Row(Text.BATTERY_OPTIMIZED, Level.WARNING, showButton = true), check(FakePhone(ignoresBatteryOptimizations = false)).battery)
        assertEquals(Row(Text.BATTERY_UNRESTRICTED, Level.OK, showButton = true), check(FakePhone(ignoresBatteryOptimizations = true)).battery)
    }

    @Test
    fun silentNoteOnlyWhileTheAlarmSoundIsOn() {
        assertEquals(true, check(FakePhone(), alarmSoundOn = true).showSilentNote)
        assertEquals(false, check(FakePhone(), alarmSoundOn = false).showSilentNote)
    }

    @Test
    fun constantsMatchAndroid() {
        assertEquals(NotificationManager.INTERRUPTION_FILTER_UNKNOWN, SoundStatus.FILTER_UNKNOWN)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, SoundStatus.FILTER_ALL)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, SoundStatus.FILTER_PRIORITY)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE, SoundStatus.FILTER_NONE)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALARMS, SoundStatus.FILTER_ALARMS)
        assertEquals(NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS, SoundStatus.PRIORITY_CATEGORY_ALARMS)
    }
}
