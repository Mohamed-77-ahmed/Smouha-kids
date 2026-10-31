package com.example.tracker.domain

import com.example.tracker.domain.SoundPolicy.CelebrationPlan
import com.example.tracker.domain.SoundPolicy.Plan
import com.example.tracker.domain.SoundPolicy.StopReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Who makes a reminder's sound, and when the app's own alarm sound stops (CLAUDE.md 7); when a
 * reward plays its short sound and how long the reward stays (batch 6).
 */
class SoundPolicyTest {

    @Test
    fun alarmSoundOnAndPendingDoseThenTheAppPlays() {
        assertEquals(Plan.APP_PLAYS, SoundPolicy.plan(canPost = true, dosePending = true, alarmSoundOn = true))
    }

    @Test
    fun settingOffMeansNoPlaybackOnlyTheNormalNotificationSound() {
        assertEquals(Plan.STANDARD_CHANNEL, SoundPolicy.plan(canPost = true, dosePending = true, alarmSoundOn = false))
    }

    @Test
    fun takenDosePlaysNothingWhateverTheSetting() {
        assertEquals(Plan.NOTHING, SoundPolicy.plan(canPost = true, dosePending = false, alarmSoundOn = true))
        assertEquals(Plan.NOTHING, SoundPolicy.plan(canPost = true, dosePending = false, alarmSoundOn = false))
    }

    @Test
    fun noPlaybackWhenTheReminderCannotBeShown() {
        // A sound with no visible reminder could not be stopped from the notification.
        assertEquals(Plan.NOTHING, SoundPolicy.plan(canPost = false, dosePending = true, alarmSoundOn = true))
        assertEquals(Plan.NOTHING, SoundPolicy.plan(canPost = false, dosePending = true, alarmSoundOn = false))
    }

    @Test
    fun soundStopsAfterExactly30Seconds() {
        assertEquals(30_000L, SoundPolicy.MAX_PLAY_MILLIS)
        val start = 1_000_000L
        assertEquals(30_000L, SoundPolicy.remainingMillis(start, start))
        assertEquals(1L, SoundPolicy.remainingMillis(start, start + 29_999))
        assertEquals(0L, SoundPolicy.remainingMillis(start, start + 30_000))
        assertEquals(0L, SoundPolicy.remainingMillis(start, start + 3_600_000))
    }

    @Test
    fun clockMovedBackNeverExtendsTheSound() {
        assertEquals(30_000L, SoundPolicy.remainingMillis(startedAtMillis = 1_000_000L, nowMillis = 0L))
    }

    @Test
    fun everyStopReasonAndWhetherTheReminderStays() {
        val keeps = StopReason.entries.associateWith { it.keepsNotification }
        assertEquals(
            mapOf(
                StopReason.TAPPED to false,
                StopReason.CONFIRMED to false,
                StopReason.STOP_BUTTON to true,
                StopReason.DISMISSED to false,
                StopReason.TIMEOUT to true,
                StopReason.REPLACED to true,
                StopReason.FOCUS_LOST to true,
                StopReason.PLAYER_ERROR to true,
                StopReason.DESTROYED to true,
            ),
            keeps,
        )
    }

    @Test
    fun alarmSoundFirstThenTheNotificationSound() {
        assertEquals(SoundPolicy.Source.ALARM_SOUND, SoundPolicy.source(alarmSoundSet = true, notificationSoundSet = true))
        assertEquals(SoundPolicy.Source.ALARM_SOUND, SoundPolicy.source(alarmSoundSet = true, notificationSoundSet = false))
        assertEquals(SoundPolicy.Source.NOTIFICATION_SOUND, SoundPolicy.source(alarmSoundSet = false, notificationSoundSet = true))
        assertNull(SoundPolicy.source(alarmSoundSet = false, notificationSoundSet = false))
    }

    // ------------------------------------------------------------ the reward's sound (batch 6)

    private val none = SoundPolicy.NO_ALARM
    private val confirmed = 42

    @Test
    fun rewardSoundOnlyForChildAndTeenWithTheSettingOn() {
        assertEquals(CelebrationPlan.PLAY, SoundPolicy.celebration(settingOn = true, AgeGroup.CHILD, none, confirmed))
        assertEquals(CelebrationPlan.PLAY, SoundPolicy.celebration(settingOn = true, AgeGroup.TEEN, none, confirmed))
        // Adults keep the calm style; the setting off silences everyone.
        assertEquals(CelebrationPlan.SILENT, SoundPolicy.celebration(settingOn = true, AgeGroup.ADULT, none, confirmed))
        for (group in AgeGroup.entries) {
            assertEquals(group.name, CelebrationPlan.SILENT, SoundPolicy.celebration(settingOn = false, group, none, confirmed))
        }
    }

    @Test
    fun noRewardSoundWithoutTheScreen() {
        for (group in AgeGroup.entries) {
            assertEquals(group.name, CelebrationPlan.SILENT, SoundPolicy.celebration(settingOn = true, group, none, confirmed, screenShown = false))
        }
        // The control: the same reward on screen plays.
        assertEquals(CelebrationPlan.PLAY, SoundPolicy.celebration(settingOn = true, AgeGroup.CHILD, none, confirmed, screenShown = true))
    }

    @Test
    fun neverTogetherWithAnAlarmSound() {
        // The confirmed dose's own alarm is being stopped: the reward waits for it.
        assertEquals(CelebrationPlan.AFTER_ALARM_STOPS, SoundPolicy.celebration(settingOn = true, AgeGroup.CHILD, confirmed, confirmed))
        // Another reminder rings: the reward stays silent, that alarm keeps ringing.
        assertEquals(CelebrationPlan.SILENT, SoundPolicy.celebration(settingOn = true, AgeGroup.CHILD, 7, confirmed))
        assertEquals(CelebrationPlan.SILENT, SoundPolicy.celebration(settingOn = true, AgeGroup.TEEN, 7, confirmed))
        // The setting off stays off even while waiting would be possible.
        assertEquals(CelebrationPlan.SILENT, SoundPolicy.celebration(settingOn = false, AgeGroup.CHILD, confirmed, confirmed))
    }

    @Test
    fun rewardStaysAtLeast3AndAtMost6Seconds() {
        assertEquals(3_000L, SoundPolicy.celebrationMillis(null))
        assertEquals(3_000L, SoundPolicy.celebrationMillis(0))
        assertEquals(3_000L, SoundPolicy.celebrationMillis(-5))
        assertEquals(3_000L, SoundPolicy.celebrationMillis(2_769)) // the dose sound: 2.77 s
        assertEquals(3_000L, SoundPolicy.celebrationMillis(3_000))
        assertEquals(4_500L, SoundPolicy.celebrationMillis(4_500))
        assertEquals(6_000L, SoundPolicy.celebrationMillis(6_000))
        assertEquals(6_000L, SoundPolicy.celebrationMillis(6_001))
        assertEquals(6_000L, SoundPolicy.celebrationMillis(60_000))
    }

    @Test
    fun aLongerSoundFadesOutBeforeItIsCut() {
        assertNull(SoundPolicy.fadeOutAtMillis(2_769))
        assertNull(SoundPolicy.fadeOutAtMillis(6_000))
        assertEquals(5_500L, SoundPolicy.fadeOutAtMillis(6_001))
        assertEquals(5_500L, SoundPolicy.fadeOutAtMillis(10_000))
        // The fade ends exactly when the reward closes.
        assertEquals(SoundPolicy.MAX_CELEBRATION_MILLIS, SoundPolicy.fadeOutAtMillis(10_000)!! + SoundPolicy.FADE_MILLIS)
    }

    @Test
    fun noAlarmMatchesTheServicesNoId() {
        assertEquals(com.example.tracker.reminders.AlarmSoundService.NO_ID, SoundPolicy.NO_ALARM)
    }
}
