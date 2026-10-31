package com.example.tracker.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.tracker.R
import com.example.tracker.domain.SoundPolicy
import com.example.tracker.reminders.AlarmSound
import com.example.tracker.reminders.AlarmSoundService

/**
 * The reward's short sound (batch 6, R3): the dose sound, once, as a game sound on the media volume
 * (USAGE_GAME), with audio focus that lets music duck. It never changes a volume. [SoundPolicy]
 * decides whether it plays; an alarm sound always wins: [AlarmSoundService] calls [stopForAlarm]
 * before it rings, and a reward waits for the confirmed dose's alarm to stop before it plays.
 * Everything runs on the main thread.
 */
class CelebrationSound(context: Context) {

    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    /** Loads the dose sound and returns its length in milliseconds; null when it cannot be loaded. */
    fun prepare(): Long? {
        stop()
        val p = try {
            MediaPlayer.create(appContext, R.raw.dose_sound, attributes, audio.generateAudioSessionId())
        } catch (e: RuntimeException) {
            Log.w(AlarmSound.TAG, "Celebration sound could not load: $e")
            null
        } ?: return null
        player = p
        return p.duration.toLong().takeIf { it > 0 }
    }

    /**
     * Plays the prepared sound once, as [plan] says. [onStarted] runs when it really starts (later
     * than now when it waits for the confirmed dose's alarm to stop).
     */
    fun start(plan: SoundPolicy.CelebrationPlan, onStarted: () -> Unit = {}) {
        when (plan) {
            SoundPolicy.CelebrationPlan.SILENT -> stop()
            SoundPolicy.CelebrationPlan.PLAY -> begin(onStarted)
            SoundPolicy.CelebrationPlan.AFTER_ALARM_STOPS -> waitForAlarm(System.currentTimeMillis(), onStarted)
        }
    }

    /** Stops and releases the sound, if any. Safe to call any time. */
    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.let {
            it.setOnCompletionListener(null)
            try {
                it.stop()
            } catch (_: IllegalStateException) {
                // Prepared but never started; release below is enough.
            }
            it.release()
        }
        player = null
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
        if (current === this) current = null
    }

    /** The stop of the confirmed dose's alarm is asynchronous: start only once nothing rings. */
    private fun waitForAlarm(since: Long, onStarted: () -> Unit) {
        when {
            player == null -> Unit
            AlarmSoundService.playingId == SoundPolicy.NO_ALARM -> begin(onStarted)
            System.currentTimeMillis() - since > ALARM_WAIT_MILLIS -> {
                Log.i(AlarmSound.TAG, "Celebration sound skipped: an alarm sound still plays")
                stop()
            }
            else -> handler.postDelayed({ waitForAlarm(since, onStarted) }, POLL_MILLIS)
        }
    }

    private fun begin(onStarted: () -> Unit) {
        val p = player ?: return
        // An alarm that started meanwhile wins: no sound at all.
        if (AlarmSoundService.playingId != SoundPolicy.NO_ALARM) {
            stop()
            return
        }
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener({ change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
            }, handler)
            .build()
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.i(AlarmSound.TAG, "Celebration sound skipped: audio focus refused")
            stop()
            return
        }
        focus = request
        current = this
        p.setOnCompletionListener { stop() }
        try {
            p.start()
        } catch (e: IllegalStateException) {
            Log.w(AlarmSound.TAG, "Celebration sound could not start: $e")
            stop()
            return
        }
        // A sound longer than the reward is cut at its end with a short fade (not ours: 2.8 s).
        SoundPolicy.fadeOutAtMillis(p.duration.toLong())?.let { at -> handler.postDelayed({ fadeOut(1f) }, at) }
        onStarted()
    }

    private fun fadeOut(volume: Float) {
        val p = player ?: return
        val next = volume - 1f / FADE_STEPS
        if (next <= 0f) {
            stop()
            return
        }
        // The player's own gain, not a device volume.
        p.setVolume(next, next)
        handler.postDelayed({ fadeOut(next) }, SoundPolicy.FADE_MILLIS / FADE_STEPS)
    }

    companion object {
        private const val POLL_MILLIS = 50L
        private const val ALARM_WAIT_MILLIS = 1_500L
        private const val FADE_STEPS = 10

        /** The sound that plays now, so an alarm can silence it first. */
        @Volatile
        private var current: CelebrationSound? = null

        /** Called by the alarm sound service before it rings: one sound at a time, the alarm wins. */
        fun stopForAlarm() {
            val sound = current ?: return
            if (Looper.myLooper() == Looper.getMainLooper()) sound.stop() else Handler(Looper.getMainLooper()).post { sound.stop() }
        }
    }
}
