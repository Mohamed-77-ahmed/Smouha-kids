package com.example.tracker.ui

import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.example.tracker.R
import com.example.tracker.databinding.ViewCourseEndBinding
import com.example.tracker.domain.SoundPolicy
import com.example.tracker.reminders.AlarmSound
import com.example.tracker.reminders.AlarmSoundService

/**
 * The course-complete screen (batch 6, R6), shown after the reward of a course's final dose. The
 * video plays once in a rounded card and then holds its last frame with the text below. Its sound
 * follows "Celebration sound" (muted when off, when the video has no audio track or while an alarm
 * rings); it pauses in the background, continues after a rotation, and is released on close. If it
 * cannot play, the celebration avatar shows as a still with the same text. No PIN; a tap outside
 * the card, the close button or Back closes it. It never changes data.
 */
class CourseEndScreen(
    private val b: ViewCourseEndBinding,
    /** Called with true when it shows and false when it closed (Back handling, the next overlay). */
    private val onShowing: (Boolean) -> Unit = {},
) : TextureView.SurfaceTextureListener {

    private val ctx get() = b.root.context
    private val audio by lazy { ctx.getSystemService(AudioManager::class.java) }
    private val handler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        .build()

    private var medicine: String? = null
    private var soundOn = true
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var prepared = false
    private var positionMillis = 0
    private var completed = false
    private var failed = false
    private var foreground = true
    private var focus: AudioFocusRequest? = null

    val isShowing: Boolean get() = b.courseEndOverlay.isVisible

    /** True while the video's own sound is silenced (tests). */
    val isMuted: Boolean get() = muted

    /** True when the still picture replaced the video (tests). */
    val isFallback: Boolean get() = failed

    private var muted = false

    init {
        b.courseEndOverlay.setOnClickListener { close() }
        // Taps on the card itself do not close it.
        b.courseCard.setOnClickListener { }
        b.courseClose.setOnClickListener { close() }
        b.video.surfaceTextureListener = this
        val radius = ctx.resources.displayMetrics.density * VIDEO_CORNER_DP
        b.videoBox.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
        b.videoBox.clipToOutline = true
        Motion.springPress(b.courseClose)
    }

    /** Shows the screen for [medicine]; [position] and [done] continue a screen restored after a rotation. */
    fun show(medicine: String, soundOn: Boolean, position: Int = 0, done: Boolean = false) {
        this.medicine = medicine
        this.soundOn = soundOn
        positionMillis = position
        completed = done
        failed = false
        b.courseMessage.text = ctx.getString(R.string.course_end_message, Fmt.bidi(ctx, medicine))
        b.fallback.isVisible = false
        b.video.isVisible = true
        b.courseEndOverlay.isVisible = true
        current = this
        onShowing(true)
        if (forcedFailure) {
            fallBack("playback made to fail by a test")
            return
        }
        b.video.surfaceTexture?.takeIf { b.video.isAvailable }?.let { open(it) }
    }

    fun close() {
        if (!isShowing) return
        releasePlayer()
        b.courseEndOverlay.isVisible = false
        medicine = null
        if (current === this) current = null
        onShowing(false)
    }

    /** The app goes to the background: the video pauses where it is. */
    fun pause() {
        foreground = false
        player?.takeIf { prepared && it.isPlaying }?.let {
            positionMillis = it.currentPosition
            it.pause()
        }
        abandonFocus()
    }

    /** Back in the foreground: it goes on from where it paused (a finished video keeps its last frame). */
    fun resume() {
        foreground = true
        val p = player ?: return
        if (prepared && !completed && !p.isPlaying) {
            applyVolume(p)
            p.start()
        }
    }

    /** The screen is destroyed (rotation, closing the app): the player goes, the state is saved first. */
    fun release() {
        releasePlayer()
        if (current === this) current = null
    }

    fun save(outState: Bundle) {
        val m = medicine ?: return
        if (!isShowing) return
        val position = player?.takeIf { prepared }?.currentPosition ?: positionMillis
        outState.putBundle(STATE_KEY, bundleOf(MEDICINE to m, SOUND to soundOn, POSITION to position, DONE to completed))
    }

    fun restore(savedState: Bundle?) {
        val s = savedState?.getBundle(STATE_KEY) ?: return
        val m = s.getString(MEDICINE) ?: return
        show(m, s.getBoolean(SOUND, true), s.getInt(POSITION), s.getBoolean(DONE))
    }

    // ------------------------------------------------------------------ the player

    private fun open(texture: SurfaceTexture) {
        if (player != null || failed || !isShowing) return
        val s = Surface(texture)
        surface = s
        prepared = false
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(ctx, "android.resource://${ctx.packageName}/${R.raw.avatar_end}".toUri())
                setSurface(s)
                setOnPreparedListener { onPrepared(it) }
                setOnCompletionListener {
                    completed = true
                    positionMillis = it.duration
                    abandonFocus()
                }
                setOnErrorListener { _, what, extra ->
                    fallBack("player error $what/$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            fallBack("the video could not open: $e")
        }
    }

    private fun onPrepared(p: MediaPlayer) {
        prepared = true
        val hasAudio = p.trackInfo.any { it.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO }
        muted = !soundOn || !hasAudio
        applyVolume(p)
        if (completed) {
            // Already played once (before a rotation): run the last moment silently to show the last frame again.
            muted = true
            p.setVolume(0f, 0f)
            p.seekTo((p.duration - LAST_MOMENT_MILLIS).coerceAtLeast(0).toLong(), MediaPlayer.SEEK_CLOSEST)
            p.start()
            return
        }
        if (positionMillis > 0) p.seekTo(positionMillis.toLong(), MediaPlayer.SEEK_CLOSEST)
        if (foreground) p.start()
    }

    /** Its own sound only with the setting on, an audio track, audio focus and no alarm ringing. */
    private fun applyVolume(p: MediaPlayer) {
        val audible = !muted && AlarmSoundService.playingId == SoundPolicy.NO_ALARM && requestFocus()
        p.setVolume(if (audible) 1f else 0f, if (audible) 1f else 0f)
    }

    private fun requestFocus(): Boolean {
        if (focus != null) return true
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener({ change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) silence()
            }, handler)
            .build()
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
        focus = request
        return true
    }

    private fun abandonFocus() {
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
    }

    /** An alarm or a call: the picture goes on, its sound stops for good. */
    private fun silence() {
        muted = true
        player?.setVolume(0f, 0f)
        abandonFocus()
    }

    private fun fallBack(why: String) {
        Log.w(AlarmSound.TAG, "Course-complete video: $why. The still picture shows instead.")
        failed = true
        releasePlayer()
        b.video.isVisible = false
        b.fallback.isVisible = true
    }

    private fun releasePlayer() {
        player?.let {
            it.setOnCompletionListener(null)
            it.setOnErrorListener(null)
            try {
                it.stop()
            } catch (_: IllegalStateException) {
                // Not prepared yet; release below is enough.
            }
            it.release()
        }
        player = null
        prepared = false
        surface?.release()
        surface = null
        abandonFocus()
    }

    // ------------------------------------------------------------------ the TextureView's surface

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) = open(texture)

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        player?.takeIf { prepared }?.let { positionMillis = it.currentPosition }
        releasePlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    companion object {
        private const val STATE_KEY = "course_end"
        private const val MEDICINE = "medicine"
        private const val SOUND = "sound"
        private const val POSITION = "position"
        private const val DONE = "done"
        private const val VIDEO_CORNER_DP = 20f
        private const val LAST_MOMENT_MILLIS = 300

        /** Test hook only (CourseEndUiTest): the video "fails", so the still picture shows. */
        @Volatile
        var forcedFailure = false

        @Volatile
        private var current: CourseEndScreen? = null

        /** The screen on display now, if any (tests read its state). */
        val showing: CourseEndScreen? get() = current

        /** Called by the alarm sound service before it rings: one sound at a time, the alarm wins. */
        fun silenceForAlarm() {
            val screen = current ?: return
            if (Looper.myLooper() == Looper.getMainLooper()) screen.silence() else Handler(Looper.getMainLooper()).post { screen.silence() }
        }
    }
}
