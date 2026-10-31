package com.example.tracker.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.tracker.R
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * Small motion helpers. Everything respects the system animation setting: with animations off
 * (animator duration scale 0) views jump to their end state and nothing waits on an animation.
 */
object Motion {

    fun animationsOn(): Boolean = ValueAnimator.areAnimatorsEnabled()

    /** Spring "press" feedback: shrinks a little while pressed, springs back on release. Never eats the touch. */
    @SuppressLint("ClickableViewAccessibility") // the touch is passed on, so clicks and accessibility stay intact
    fun springPress(view: View, target: View = view) {
        val x = spring(target, DynamicAnimation.SCALE_X)
        val y = spring(target, DynamicAnimation.SCALE_Y)
        view.setOnTouchListener { _, event ->
            val scale = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> PRESSED_SCALE
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> 1f
                else -> null
            }
            if (scale != null) {
                if (animationsOn()) {
                    x.animateToFinalPosition(scale)
                    y.animateToFinalPosition(scale)
                } else {
                    target.scaleX = 1f
                    target.scaleY = 1f
                }
            }
            false
        }
    }

    /** Springs [view] up into place from below (used by the celebration card). */
    fun springIn(view: View, fromDp: Float = 48f) {
        if (!animationsOn()) {
            view.translationY = 0f
            return
        }
        view.translationY = fromDp * view.resources.displayMetrics.density
        spring(view, DynamicAnimation.TRANSLATION_Y, SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
            .animateToFinalPosition(0f)
    }

    /** Pops [view] in with a bouncy spring from [fromScale] (the reward avatar). At full size with animations off. */
    fun springPop(view: View, fromScale: Float = 0.3f) {
        if (!animationsOn()) {
            view.scaleX = 1f
            view.scaleY = 1f
            return
        }
        view.scaleX = fromScale
        view.scaleY = fromScale
        listOf(DynamicAnimation.SCALE_X, DynamicAnimation.SCALE_Y).forEach {
            SpringAnimation(view, it).apply {
                spring = SpringForce(1f).setStiffness(SpringForce.STIFFNESS_LOW).setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
            }.start()
        }
    }

    /**
     * The mascot's idle motion: a few slow, small "breaths" (a few percent taller, from its feet),
     * then rest, or earlier on [stopIdle] or when the view leaves the window. Not endless: every
     * scale change is an accessibility event, and an endless stream keeps TalkBack and test tools
     * from ever seeing the screen settle. Nothing at all with animations off.
     */
    fun idle(view: View) {
        stopIdle(view)
        if (!animationsOn()) return
        view.pivotY = (if (view.height > 0) view.height else view.layoutParams.height).toFloat()
        val breath = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1f + IDLE_STRETCH),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1f - IDLE_STRETCH / 2),
        ).apply {
            duration = IDLE_MILLIS
            // In and out per breath, so it ends at rest (scale 1).
            repeatCount = IDLE_BREATHS * 2 - 1
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        view.setTag(R.id.idle_motion, breath)
        if (view.getTag(R.id.idle_detach) == null) {
            val onDetach = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) = stopIdle(v)
            }
            view.addOnAttachStateChangeListener(onDetach)
            view.setTag(R.id.idle_detach, onDetach)
        }
        breath.start()
    }

    fun stopIdle(view: View) {
        (view.getTag(R.id.idle_motion) as? Animator)?.cancel()
        view.setTag(R.id.idle_motion, null)
        view.scaleX = 1f
        view.scaleY = 1f
    }

    /** True while [idle] runs on [view] (tests). */
    fun isIdling(view: View): Boolean = (view.getTag(R.id.idle_motion) as? Animator)?.isStarted == true

    private fun spring(
        view: View,
        property: DynamicAnimation.ViewProperty,
        damping: Float = SpringForce.DAMPING_RATIO_LOW_BOUNCY,
    ) = SpringAnimation(view, property).apply {
        spring = SpringForce().setStiffness(SpringForce.STIFFNESS_MEDIUM).setDampingRatio(damping)
    }

    private const val PRESSED_SCALE = 0.94f
    private const val IDLE_STRETCH = 0.04f
    private const val IDLE_MILLIS = 1_600L
    const val IDLE_BREATHS = 3
}
