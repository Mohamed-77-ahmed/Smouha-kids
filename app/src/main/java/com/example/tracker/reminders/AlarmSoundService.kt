package com.example.tracker.reminders

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.tracker.domain.SoundPolicy
import com.example.tracker.domain.SoundPolicy.StopReason
import com.example.tracker.ui.CelebrationSound
import com.example.tracker.ui.CourseEndScreen

/**
 * Plays the phone's alarm sound for one reminder, like a clock app (CLAUDE.md 7): looping for at
 * most [SoundPolicy.MAX_PLAY_MILLIS], on the alarm stream, with audio focus, never touching a
 * volume. Its foreground notification is the reminder itself, on a silent channel with a
 * "Stop sound" button, so there is one sound and no extra notification. One sound at a time: a
 * newer reminder takes the sound over and the older one stays in the list, quiet.
 *
 * Foreground service type `systemExempted`: Android documents it for apps holding USE_EXACT_ALARM
 * or SCHEDULE_EXACT_ALARM "using Foreground Service to continue alarms in the background", and an
 * exact alarm lets the app start it from the background (docs/DECISIONS.md D28).
 */
class AlarmSoundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish(StopReason.TIMEOUT) }
    private val audio by lazy { getSystemService(AudioManager::class.java) }
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    /** The reminder whose sound plays now. */
    private var playing: Notifications.Content? = null
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        val action = intent?.action
        val content = intent?.let { Notifications.Content.from(it) }
        val stopId = intent?.getIntExtra(EXTRA_STOP_ID, NO_ID) ?: NO_ID
        val reason = StopReason.entries.firstOrNull { it.name == intent?.getStringExtra(EXTRA_REASON) }
        when {
            action == ACTION_PLAY && content != null -> play(content)
            action == ACTION_STOP && playing?.notificationId == stopId -> finish(reason ?: StopReason.DESTROYED)
            // A stop for a reminder that no longer plays changes nothing.
            playing == null -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        val content = playing
        stopPlayer()
        setPlaying(null)
        if (content != null) {
            // Ended by Android, not by a stop: the reminder stays in the list, quiet.
            Log.i(AlarmSound.TAG, "Sound of reminder ${content.notificationId} ended: ${StopReason.DESTROYED}")
            Notifications.post(this, content, Notifications.Style.RANG)
        }
        super.onDestroy()
    }

    private fun play(content: Notifications.Content) {
        // One sound at a time: a reward's short sound stops, a course-complete video goes silent,
        // before the alarm rings.
        CelebrationSound.stopForAlarm()
        CourseEndScreen.silenceForAlarm()
        val previous = playing
        if (previous != null && previous.notificationId != content.notificationId) {
            // The older reminder keeps its place in the list without "Stop sound" and without alerting again.
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            Notifications.post(this, previous, Notifications.Style.RANG)
            Log.i(AlarmSound.TAG, "Sound of reminder ${previous.notificationId} ended: ${StopReason.REPLACED}")
        }
        stopPlayer()
        setPlaying(null)
        try {
            // Foreground first: Android expects it within seconds of startForegroundService().
            ServiceCompat.startForeground(
                this,
                content.notificationId,
                Notifications.build(this, content, Notifications.Style.RINGING),
                foregroundType(),
            )
            if (AlarmSound.forcedFailure == AlarmSound.Failure.START_FOREGROUND) {
                throw IllegalStateException("startForeground made to fail by a test")
            }
        } catch (e: RuntimeException) {
            fallBack(content, "startForeground failed: $e")
            return
        }
        if (!startPlayer()) {
            fallBack(content, "no alarm sound could start")
            return
        }
        setPlaying(content)
        handler.postDelayed(timeout, SoundPolicy.MAX_PLAY_MILLIS)
    }

    /** The app's own sound cannot play, so the alarm channel rings instead: never silence by accident, never both. */
    private fun fallBack(content: Notifications.Content, why: String) {
        Log.w(AlarmSound.TAG, "Reminder ${content.notificationId}: $why. The alarm channel rings instead.")
        stopPlayer()
        // Removed and posted again (not updated), so the channel's sound plays.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        Notifications.post(this, content, Notifications.Style.ALARM)
        stopSelf(lastStartId)
    }

    /** Ends the sound. The reminder stays in the list, quiet, unless [reason] closes it. */
    private fun finish(reason: StopReason) {
        val content = playing ?: return
        stopPlayer()
        setPlaying(null)
        Log.i(AlarmSound.TAG, "Sound of reminder ${content.notificationId} ended: $reason")
        if (reason.keepsNotification) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            Notifications.post(this, content, Notifications.Style.RANG)
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf(lastStartId)
    }

    /** Starts the looping alarm sound with audio focus. False when no sound could start. */
    private fun startPlayer(): Boolean {
        val source = SoundPolicy.source(
            alarmSoundSet = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM) != null,
            notificationSoundSet = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION) != null,
        ) ?: return false
        // The settings URI of the default sound: Android serves it without any storage permission.
        val uri = RingtoneManager.getDefaultUri(
            if (source == SoundPolicy.Source.ALARM_SOUND) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        )
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener({ change ->
                // A call or another alarm: stop for good (the reminder stays). Short ducking is left to Android.
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    finish(StopReason.FOCUS_LOST)
                }
            }, handler)
            .build()
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(AlarmSound.TAG, "Audio focus refused")
            return false
        }
        focus = request
        return try {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(this@AlarmSoundService, uri)
                isLooping = true
                setOnErrorListener { _, what, extra ->
                    Log.w(AlarmSound.TAG, "Player error $what/$extra")
                    finish(StopReason.PLAYER_ERROR)
                    true
                }
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.w(AlarmSound.TAG, "Alarm sound could not play: $e")
            stopPlayer()
            false
        }
    }

    private fun stopPlayer() {
        handler.removeCallbacks(timeout)
        player?.let {
            it.setOnErrorListener(null)
            try {
                it.stop()
            } catch (_: IllegalStateException) {
                // Never started; release below is enough.
            }
            it.release()
        }
        player = null
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun setPlaying(content: Notifications.Content?) {
        playing = content
        playingId = content?.notificationId ?: NO_ID
    }

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else 0

    companion object {
        private const val ACTION_PLAY = "com.smouha.kidsreminder.action.PLAY_SOUND"
        private const val ACTION_STOP = "com.smouha.kidsreminder.action.STOP_SOUND"
        private const val EXTRA_STOP_ID = "stop_id"
        private const val EXTRA_REASON = "stop_reason"
        const val NO_ID = SoundPolicy.NO_ALARM

        /** The reminder whose sound plays now, [NO_ID] when the app plays nothing. */
        @Volatile
        var playingId: Int = NO_ID
            private set

        fun playIntent(context: Context, content: Notifications.Content): Intent =
            content.putInto(Intent(context, AlarmSoundService::class.java).setAction(ACTION_PLAY))

        fun stopIntent(context: Context, reason: StopReason, notificationId: Int): Intent =
            Intent(context, AlarmSoundService::class.java)
                .setAction(ACTION_STOP)
                // Each (reminder, reason) needs its own PendingIntent; extras do not tell them apart.
                .setData(Uri.fromParts("kidsreminder", "$notificationId/${reason.name}", null))
                .putExtra(EXTRA_STOP_ID, notificationId)
                .putExtra(EXTRA_REASON, reason.name)

        fun stopButtonIntent(context: Context, notificationId: Int): PendingIntent =
            pending(context, stopIntent(context, StopReason.STOP_BUTTON, notificationId))

        fun dismissedIntent(context: Context, notificationId: Int): PendingIntent =
            pending(context, stopIntent(context, StopReason.DISMISSED, notificationId))

        private fun pending(context: Context, intent: Intent): PendingIntent =
            PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
