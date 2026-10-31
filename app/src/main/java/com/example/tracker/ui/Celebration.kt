package com.example.tracker.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.example.tracker.R
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.databinding.ViewCelebrationBinding
import com.example.tracker.domain.AgeGroup
import com.example.tracker.domain.Gender
import com.example.tracker.domain.SoundPolicy
import com.example.tracker.reminders.AlarmSoundService

/**
 * Reward and streak-broken overlay, styled per age group (CLAUDE.md 5 and 6b). It never changes
 * data, closes on tap or by itself (a reward after max(3 s, min(its sound, 6 s)), everything else
 * after 3 s: [SoundPolicy.celebrationMillis]), and re-shows its final state after rotation without
 * playing the sound again.
 */
class Celebration(
    private val b: ViewCelebrationBinding,
    /** The reward's short sound (CHILD and TEEN, when the parent setting is on); null plays nothing. */
    private val sound: CelebrationSound? = null,
    /** Called after the overlay closed, so the next pending animation can be shown. */
    private val onHidden: () -> Unit = {},
) {

    private val ctx get() = b.root.context
    private var spec: Bundle? = null
    private val autoHide = Runnable { hide() }

    val isShowing: Boolean get() = b.overlay.isVisible

    init {
        b.overlay.setOnClickListener { hide() }
        b.close.setOnClickListener { hide() }
        Motion.springPress(b.close)
    }

    /**
     * [soundOn] is the parent setting "Celebration sound"; [reminderId] is the confirmed dose's
     * reminder, whose alarm sound (if it rings) stops before the reward's sound starts.
     */
    fun showReward(o: ConfirmOutcome.Confirmed, soundOn: Boolean = false, reminderId: Int = SoundPolicy.NO_ALARM) {
        val cheers = ctx.resources.getStringArray(R.array.cheers)
        // Never without the screen: the confirmation can finish while the app goes to the background.
        val onScreen = b.overlay.isAttachedToWindow && b.overlay.windowVisibility == View.VISIBLE
        val plan = SoundPolicy.celebration(soundOn, o.ageGroup, AlarmSoundService.playingId, reminderId, onScreen)
        val soundMillis = if (plan == SoundPolicy.CelebrationPlan.SILENT) null else sound?.prepare()
        if (soundMillis == null) sound?.stop()
        val millis = SoundPolicy.celebrationMillis(soundMillis)
        show(
            Bundle().apply {
                putLong(DURATION, millis)
                putString(TYPE, TYPE_REWARD)
                putString(GROUP, o.ageGroup.name)
                putString(PATIENT, o.patientName)
                putString(AVATAR, Mascots.avatar(o.ageGroup, o.gender))
                putString(MEDICINE, o.medicineName)
                putBoolean(DOSE_PAID, o.dosePaid)
                putInt(DOSE_POINTS, o.dosePoints)
                putInt(ON_TIME, o.onTimeBonus)
                putInt(PERFECT, o.perfectDayBonus)
                putInt(MILESTONE, o.milestone ?: 0)
                putInt(MILESTONE_BONUS, o.milestoneBonus)
                putInt(STREAK, o.liveStreak)
                putInt(EARNED, o.earned)
                putBoolean(DAY_COMPLETE, o.dayComplete)
                putInt(CHEER, cheers.indices.random())
            },
            animate = true,
        )
        // Once, now (or right after the confirmed dose's alarm stopped); the reward then stays its
        // full time from the moment the sound really starts.
        if (soundMillis != null) {
            sound?.start(plan) {
                b.overlay.removeCallbacks(autoHide)
                b.overlay.postDelayed(autoHide, millis)
            }
        }
    }

    fun showStreakBroken(patientName: String, group: AgeGroup, gender: Gender?, lostStreak: Int, bestStreak: Int) {
        sound?.stop()
        show(
            Bundle().apply {
                putString(TYPE, TYPE_BROKEN)
                putString(GROUP, group.name)
                putString(PATIENT, patientName)
                putString(AVATAR, Mascots.avatar(group, gender))
                putInt(STREAK, lostStreak)
                putInt(BEST, bestStreak)
            },
            animate = true,
        )
    }

    fun save(outState: Bundle) {
        if (isShowing) spec?.let { outState.putBundle(STATE_KEY, it) }
    }

    fun restore(savedState: Bundle?) {
        savedState?.getBundle(STATE_KEY)?.let { show(it, animate = false) }
    }

    fun hide() {
        sound?.stop()
        b.overlay.removeCallbacks(autoHide)
        b.mascotArt.removeCallbacks(wakeUp)
        Motion.stopIdle(b.mascotArt)
        if (!b.overlay.isVisible) return
        b.confetti.stop()
        b.overlay.animate().alpha(0f).setDuration(180).withEndAction {
            b.overlay.isVisible = false
            spec = null
            onHidden()
        }.start()
    }

    private fun show(spec: Bundle, animate: Boolean) {
        this.spec = spec
        b.mascotArt.removeCallbacks(wakeUp)
        b.mascotArt.isVisible = false
        b.avatarArt.isVisible = false
        val group = AgeGroup.valueOf(spec.getString(GROUP, AgeGroup.CHILD.name))
        if (spec.getString(TYPE) == TYPE_BROKEN) bindBroken(spec, group, animate) else bindReward(spec, group, animate)
        // The patient's avatar (age group and gender) before the line that names them.
        spec.getString(AVATAR)?.let { b.subtitle.text = ctx.getString(R.string.avatar_label, it, b.subtitle.text) }
        b.overlay.animate().cancel()
        b.overlay.alpha = if (animate) 0f else 1f
        b.overlay.isVisible = true
        if (animate) b.overlay.animate().alpha(1f).setDuration(180).start()
        b.overlay.removeCallbacks(autoHide)
        b.overlay.postDelayed(autoHide, spec.getLong(DURATION, SoundPolicy.MIN_CELEBRATION_MILLIS))
    }

    private fun bindReward(s: Bundle, group: AgeGroup, animate: Boolean) {
        val patient = Fmt.bidi(ctx, s.getString(PATIENT).orEmpty())
        val medicine = Fmt.bidi(ctx, s.getString(MEDICINE).orEmpty())
        val earned = s.getInt(EARNED)
        val streak = s.getInt(STREAK)

        b.subtitle.text = ctx.getString(R.string.reward_subtitle, patient, medicine)
        b.details.text = rewardDetails(s)
        b.details.isVisible = b.details.text.isNotEmpty()
        b.streak.isVisible = streak > 0
        b.streak.text = ctx.getString(R.string.streak_line, Fmt.days(ctx, streak))
        b.points.isVisible = true
        val pointsText = { n: Int ->
            if (n == 0) ctx.getString(R.string.reward_no_points)
            else ctx.resources.getQuantityString(R.plurals.plus_points, n, n)
        }

        when (group) {
            // CHILD and TEEN: the celebration avatar springs in, confetti, the points count up (batch 6, R3).
            AgeGroup.CHILD -> {
                b.mascot.isVisible = false
                b.checkmark.isVisible = false
                showAvatar(animate)
                b.title.text = if (s.getBoolean(DAY_COMPLETE)) {
                    ctx.getString(R.string.cheer_perfect_day)
                } else {
                    ctx.resources.getStringArray(R.array.cheers)[s.getInt(CHEER)]
                }
                b.close.setText(R.string.reward_close_child)
                if (animate) {
                    popCard()
                    countUp(earned, pointsText)
                    b.confetti.burst()
                    haptic()
                } else {
                    b.points.text = pointsText(earned)
                }
            }
            AgeGroup.TEEN -> {
                b.mascot.isVisible = false
                b.checkmark.isVisible = false
                showAvatar(animate)
                b.title.text = ctx.getString(R.string.reward_teen_title, patient)
                b.close.setText(R.string.close)
                if (animate) {
                    popCard()
                    countUp(earned, pointsText)
                    // Lighter than a child's: fewer, shorter confetti.
                    b.confetti.burst(pieceCount = TEEN_PIECES, durationMs = TEEN_CONFETTI_MS)
                    haptic()
                } else {
                    b.points.text = pointsText(earned)
                }
            }
            AgeGroup.ADULT -> {
                b.mascot.isVisible = false
                b.checkmark.isVisible = true
                b.title.text = ctx.getString(R.string.reward_adult_title)
                b.close.setText(R.string.close)
                b.points.text = pointsText(earned)
                if (animate) b.checkmark.play() else b.checkmark.showDone()
            }
        }
    }

    private fun rewardDetails(s: Bundle): String = buildList {
        if (s.getBoolean(DOSE_PAID)) {
            add(ctx.getString(R.string.reward_dose, s.getInt(DOSE_POINTS)))
            if (s.getInt(ON_TIME) > 0) add(ctx.getString(R.string.reward_on_time, s.getInt(ON_TIME)))
        }
        if (s.getInt(PERFECT) > 0) add(ctx.getString(R.string.reward_perfect_day, s.getInt(PERFECT)))
        if (s.getInt(MILESTONE_BONUS) > 0) {
            add(ctx.getString(R.string.reward_milestone, Fmt.days(ctx, s.getInt(MILESTONE)), s.getInt(MILESTONE_BONUS)))
        }
    }.joinToString("\n")

    private fun bindBroken(s: Bundle, group: AgeGroup, animate: Boolean) {
        val patient = Fmt.bidi(ctx, s.getString(PATIENT).orEmpty())
        val lost = Fmt.days(ctx, s.getInt(STREAK))
        val best = Fmt.days(ctx, s.getInt(BEST))
        b.checkmark.isVisible = false
        b.points.isVisible = false
        b.streak.isVisible = false
        b.details.isVisible = false
        when (group) {
            AgeGroup.CHILD -> {
                b.mascot.isVisible = false
                b.title.text = ctx.getString(R.string.broken_child_title)
                b.subtitle.text = ctx.getString(R.string.broken_child_subtitle, patient, lost)
                b.close.setText(R.string.broken_child_action)
                if (animate) {
                    // The sleepy mascot sways gently, then wakes up happy: start again. No harsh effects.
                    showArt(Mascots.ART_SLEEPY)
                    popCard()
                    ObjectAnimator.ofFloat(b.mascotArt, View.ROTATION, 0f, -6f, 6f, -4f, 4f, 0f)
                        .setDuration(1_400).start()
                    b.mascotArt.postDelayed(wakeUp, 1_500)
                } else {
                    showArt(Mascots.ART_HAPPY)
                }
            }
            AgeGroup.TEEN -> {
                b.mascot.isVisible = true
                b.mascot.textSize = 40f
                b.mascot.text = Mascots.TEEN_RESTART
                b.title.text = ctx.getString(R.string.broken_teen_title, lost)
                b.subtitle.text = ctx.getString(R.string.broken_teen_subtitle, patient)
                b.close.setText(R.string.broken_teen_action)
                if (animate) popCard()
            }
            AgeGroup.ADULT -> {
                b.mascot.isVisible = true
                b.mascot.textSize = 32f
                b.mascot.text = Mascots.ADULT_PAUSE
                b.title.text = ctx.getString(R.string.broken_adult_title, best)
                b.subtitle.text = patient
                b.close.setText(R.string.close)
            }
        }
    }

    /** CHILD and TEEN reward: the celebration avatar, springing in (at full size with animations off or after rotation). */
    private fun showAvatar(animate: Boolean) {
        b.avatarArt.isVisible = true
        if (animate) {
            Motion.springPop(b.avatarArt)
        } else {
            b.avatarArt.scaleX = 1f
            b.avatarArt.scaleY = 1f
        }
    }

    /** CHILD streak ended: the vector mascot in [art], at rest, breathing gently (Motion.idle, nothing with animations off). */
    private fun showArt(@DrawableRes art: Int) {
        b.mascotArt.setImageResource(art)
        b.mascotArt.tag = art
        b.mascotArt.rotation = 0f
        b.mascotArt.translationY = 0f
        b.mascotArt.isVisible = true
        Motion.idle(b.mascotArt)
    }

    private val wakeUp = Runnable {
        showArt(Mascots.ART_HAPPY)
        if (Motion.animationsOn()) {
            ObjectAnimator.ofFloat(b.mascotArt, View.TRANSLATION_Y, 0f, -24f, 0f).setDuration(500).start()
        }
    }

    private fun popCard() {
        Motion.springIn(b.card)
        b.card.scaleX = 0.4f
        b.card.scaleY = 0.4f
        b.card.animate().scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(2.2f)).setDuration(450).start()
    }

    private fun countUp(target: Int, format: (Int) -> String) {
        if (target == 0) {
            b.points.text = format(0)
            return
        }
        ValueAnimator.ofInt(0, target).apply {
            duration = 900
            startDelay = 200
            addUpdateListener { b.points.text = format(maxOf(1, it.animatedValue as Int)) }
            start()
        }
    }

    private fun haptic() {
        b.overlay.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
        )
    }

    private companion object {
        const val STATE_KEY = "celebration"
        const val TYPE = "type"
        const val TYPE_REWARD = "reward"
        const val TYPE_BROKEN = "broken"
        const val GROUP = "group"
        const val PATIENT = "patient"
        const val AVATAR = "avatar"
        const val MEDICINE = "medicine"
        const val DOSE_PAID = "dose_paid"
        const val DOSE_POINTS = "dose_points"
        const val ON_TIME = "on_time"
        const val PERFECT = "perfect"
        const val MILESTONE = "milestone"
        const val MILESTONE_BONUS = "milestone_bonus"
        const val STREAK = "streak"
        const val BEST = "best"
        const val EARNED = "earned"
        const val DAY_COMPLETE = "day_complete"
        const val CHEER = "cheer"
        const val DURATION = "duration"
        const val TEEN_PIECES = 50
        const val TEEN_CONFETTI_MS = 1_200L
    }
}
