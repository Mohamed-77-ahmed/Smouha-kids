package com.example.tracker.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.android.material.color.MaterialColors

/** Clean circle-then-check stroke animation for the adult reward. No cartoon. */
class CheckmarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 6f * density
        color = MaterialColors.getColor(this@CheckmarkView, androidx.appcompat.R.attr.colorPrimary)
    }
    private val oval = RectF()
    private val check = Path()
    private val partial = Path()
    private val measure = PathMeasure()
    private var progress = 1f
    private var animator: ValueAnimator? = null

    fun play() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 750
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun showDone() {
        animator?.cancel()
        progress = 1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = paint.strokeWidth
        oval.set(inset, inset, width - inset, height - inset)
        val circlePart = (progress / CIRCLE_SHARE).coerceAtMost(1f)
        canvas.drawArc(oval, -90f, 360f * circlePart, false, paint)
        if (progress <= CIRCLE_SHARE) return
        val w = width.toFloat()
        val h = height.toFloat()
        check.reset()
        check.moveTo(w * 0.28f, h * 0.52f)
        check.lineTo(w * 0.44f, h * 0.68f)
        check.lineTo(w * 0.73f, h * 0.36f)
        measure.setPath(check, false)
        partial.reset()
        val checkPart = (progress - CIRCLE_SHARE) / (1f - CIRCLE_SHARE)
        measure.getSegment(0f, measure.length * checkPart, partial, true)
        canvas.drawPath(partial, paint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val CIRCLE_SHARE = 0.55f
    }
}
