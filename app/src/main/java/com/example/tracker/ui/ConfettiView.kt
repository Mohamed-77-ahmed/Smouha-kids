package com.example.tracker.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.graphics.withTranslation
import androidx.core.view.doOnLayout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Draws a one-shot confetti explosion. Call [burst] to play it. */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private class Piece(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Int,
        var rotation: Float,
        val spin: Float,
        val round: Boolean,
    )

    private val pieces = mutableListOf<Piece>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private var animator: ValueAnimator? = null
    private var fade = 1f

    /** Plays one explosion. Children get the full burst, teens a short one. */
    fun burst(pieceCount: Int = PIECE_COUNT, durationMs: Long = DURATION_MS) =
        doOnLayout { start(pieceCount, durationMs) }

    fun stop() {
        animator?.cancel()
        pieces.clear()
        invalidate()
    }

    private fun start(pieceCount: Int, durationMs: Long) {
        animator?.cancel()
        pieces.clear()
        val originX = width / 2f
        val originY = height * 0.4f
        repeat(pieceCount) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val speed = Random.nextFloat() * 900f + 300f
            pieces += Piece(
                x = originX,
                y = originY,
                vx = (cos(angle) * speed).toFloat() * density,
                vy = (sin(angle) * speed).toFloat() * density - 500f * density,
                size = (Random.nextFloat() * 8f + 6f) * density,
                color = COLORS.random(),
                rotation = Random.nextFloat() * 360f,
                spin = Random.nextFloat() * 720f - 360f,
                round = Random.nextInt(4) == 0,
            )
        }

        var lastTime = 0L
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val dt = (anim.currentPlayTime - lastTime) / 1000f
                lastTime = anim.currentPlayTime
                step(dt)
                fade = if (anim.animatedFraction < 0.7f) 1f else (1f - anim.animatedFraction) / 0.3f
                invalidate()
            }
            doOnEnd {
                pieces.clear()
                invalidate()
            }
            start()
        }
    }

    private fun step(dt: Float) {
        val gravity = 1400f * density
        pieces.forEach {
            it.vy += gravity * dt
            it.vx *= 1f - 1.2f * dt // air drag
            it.x += it.vx * dt
            it.y += it.vy * dt
            it.rotation += it.spin * dt
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pieces.forEach {
            paint.color = it.color
            paint.alpha = (fade * 255).toInt()
            canvas.withTranslation(it.x, it.y) {
                rotate(it.rotation)
                if (it.round) {
                    drawCircle(0f, 0f, it.size / 2.5f, paint)
                } else {
                    drawRect(-it.size / 2, -it.size / 4, it.size / 2, it.size / 4, paint)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    companion object {
        const val PIECE_COUNT = 140
        const val DURATION_MS = 2800L
        // Sampled from the Koko pictures (palette v2), plus the old pink.
        val COLORS = intArrayOf(
            0xFFF38E32.toInt(), // rooster orange
            0xFFFDC440.toInt(), // sunny yellow
            0xFF48B7D3.toInt(), // sky blue of the platform
            0xFFA7BC33.toInt(), // lime of the platform
            0xFFFF6B81.toInt(), // pink
            0xFFE86C2C.toInt(), // deep orange of the platform
        )
    }
}
