package com.example.tracker.domain

/**
 * How a reminder makes its sound (CLAUDE.md 7), and when a reward plays its short sound and how
 * long it stays. Pure, so every rule is unit-tested.
 *
 * A notification channel's sound alone is not enough: a Samsung phone on silent played nothing for
 * an alarm-stream channel (docs/evidence/batch2/phone-diagnosis.txt). So the app plays the alarm
 * sound itself, from a short foreground service, and the notification it shows stays silent.
 */
object SoundPolicy {

    /** The longest the app plays the alarm sound for one reminder. */
    const val MAX_PLAY_MILLIS = 30_000L

    enum class Plan {
        /** No notification and no sound: notifications are off, or the dose is no longer pending. */
        NOTHING,
        /** The app plays the alarm sound; the notification goes to a silent channel (never a double sound). */
        APP_PLAYS,
        /** "Alarm sound" is off: a normal notification with the normal notification sound. */
        STANDARD_CHANNEL,
    }

    fun plan(canPost: Boolean, dosePending: Boolean, alarmSoundOn: Boolean): Plan = when {
        !canPost || !dosePending -> Plan.NOTHING
        alarmSoundOn -> Plan.APP_PLAYS
        else -> Plan.STANDARD_CHANNEL
    }

    /** What ended the app's alarm sound, and whether the reminder stays in the notification list. */
    enum class StopReason(val keepsNotification: Boolean) {
        /** The reminder was tapped and its dose opens (the reminder goes, as a tap always did). */
        TAPPED(false),
        /** The dose was confirmed, or deleted with its medicine. */
        CONFIRMED(false),
        /** "Stop sound": silence only, the reminder stays and follow-ups still come. */
        STOP_BUTTON(true),
        /** The reminder was swiped away. */
        DISMISSED(false),
        /** It played [MAX_PLAY_MILLIS]. */
        TIMEOUT(true),
        /** A newer reminder took over the sound; this reminder stays, quiet. */
        REPLACED(true),
        /** A call or another alarm took the audio. */
        FOCUS_LOST(true),
        /** The player failed while playing. */
        PLAYER_ERROR(true),
        /** The service ended for any other reason. */
        DESTROYED(true),
    }

    /** Play time left for a sound that started at [startedAtMillis]; 0 means stop now. Never above the cap. */
    fun remainingMillis(startedAtMillis: Long, nowMillis: Long): Long =
        (MAX_PLAY_MILLIS - (nowMillis - startedAtMillis)).coerceIn(0L, MAX_PLAY_MILLIS)

    enum class Source { ALARM_SOUND, NOTIFICATION_SOUND }

    /** The phone's alarm sound; its notification sound when no alarm sound is set; null when neither is. */
    fun source(alarmSoundSet: Boolean, notificationSoundSet: Boolean): Source? = when {
        alarmSoundSet -> Source.ALARM_SOUND
        notificationSoundSet -> Source.NOTIFICATION_SOUND
        else -> null
    }

    // ------------------------------------------------------------ the reward's sound (batch 6, R3)

    /** No reminder's alarm sound plays (the alarm sound service's "no id"). */
    const val NO_ALARM = -1

    /** The shortest and longest a reward stays on screen by itself; a tap still closes it earlier. */
    const val MIN_CELEBRATION_MILLIS = 3_000L
    const val MAX_CELEBRATION_MILLIS = 6_000L

    /** A sound longer than the reward is cut with this fade-out. */
    const val FADE_MILLIS = 500L

    enum class CelebrationPlan {
        /** Play the dose sound once, now. */
        PLAY,
        /** The confirmed dose's alarm sound is being stopped: play once it has stopped. */
        AFTER_ALARM_STOPS,
        /** No sound: the setting is off, an adult (calm style), or another reminder rings. */
        SILENT,
    }

    /**
     * Whether a reward plays the dose sound. Only CHILD and TEEN, only with "Celebration sound" on,
     * only while the reward is on screen, and never together with an alarm sound: the confirmed
     * dose's alarm stops first, and while another reminder rings the reward stays silent.
     */
    fun celebration(
        settingOn: Boolean,
        group: AgeGroup,
        alarmPlayingId: Int,
        confirmedReminderId: Int,
        screenShown: Boolean = true,
    ): CelebrationPlan = when {
        !settingOn || group == AgeGroup.ADULT || !screenShown -> CelebrationPlan.SILENT
        alarmPlayingId == NO_ALARM -> CelebrationPlan.PLAY
        alarmPlayingId == confirmedReminderId -> CelebrationPlan.AFTER_ALARM_STOPS
        else -> CelebrationPlan.SILENT
    }

    /** How long a reward stays: max(3 s, min(sound, 6 s)); 3 s without a sound. Replaces D22's fixed 3 s. */
    fun celebrationMillis(soundMillis: Long?): Long =
        if (soundMillis == null || soundMillis <= 0) MIN_CELEBRATION_MILLIS
        else soundMillis.coerceIn(MIN_CELEBRATION_MILLIS, MAX_CELEBRATION_MILLIS)

    /** When the sound starts fading out, after its start: only when it is longer than the reward and gets cut. */
    fun fadeOutAtMillis(soundMillis: Long): Long? =
        if (soundMillis > MAX_CELEBRATION_MILLIS) MAX_CELEBRATION_MILLIS - FADE_MILLIS else null
}
