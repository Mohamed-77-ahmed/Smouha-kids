package com.example.tracker.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The course-complete card's content (batch 6, R6): the video box at the video's aspect ratio and
 * the text under it, together at most [MAX_SHARE] of the space the screen gives (width and height),
 * in portrait, landscape and on tablets. A large font takes room from the video, never from the text.
 * Children: the video box first, the text second.
 */
class CourseEndLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val box = getChildAt(0)
        val text = getChildAt(1)
        val maxW = (MeasureSpec.getSize(widthMeasureSpec) * MAX_SHARE).roundToInt() - paddingLeft - paddingRight
        val maxH = (MeasureSpec.getSize(heightMeasureSpec) * MAX_SHARE).roundToInt() - paddingTop - paddingBottom
        val density = resources.displayMetrics.density
        val minVideo = (MIN_VIDEO_DP * density).roundToInt()
        // The text may be wider than a narrow (portrait) video, so it never needs many short lines;
        // the card around the video has the video's own edge color, so the extra width blends in.
        val textMin = (TEXT_MIN_WIDTH_DP * density).roundToInt().coerceAtMost(maxW)
        fun videoWidth(h: Int) = (h * VIDEO_ASPECT).roundToInt()
        fun textWidth(h: Int) = max(videoWidth(h), textMin).coerceAtMost(maxW)
        fun fits(h: Int) = h + textHeight(text, textWidth(h)) <= maxH
        // The tallest video that leaves room for the whole text: a larger video only makes the text
        // wider (fewer lines), so the fit is monotonic and a binary search finds it.
        val highest = min(maxH, (maxW / VIDEO_ASPECT).toInt()).coerceAtLeast(minVideo)
        var lo = minVideo
        var hi = highest
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (fits(mid)) lo = mid else hi = mid - 1
        }
        val h = lo
        val textW = textWidth(h)
        val textH = textHeight(text, textW)
        box.measure(exactly(videoWidth(h)), exactly(h))
        text.measure(exactly(textW), exactly(textH))
        setMeasuredDimension(max(videoWidth(h), textW) + paddingLeft + paddingRight, h + textH + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val box = getChildAt(0)
        val text = getChildAt(1)
        val inner = measuredWidth - paddingLeft - paddingRight
        val boxLeft = paddingLeft + (inner - box.measuredWidth) / 2
        box.layout(boxLeft, paddingTop, boxLeft + box.measuredWidth, paddingTop + box.measuredHeight)
        val top = paddingTop + box.measuredHeight
        val textLeft = paddingLeft + (inner - text.measuredWidth) / 2
        text.layout(textLeft, top, textLeft + text.measuredWidth, top + text.measuredHeight)
    }

    private fun textHeight(text: android.view.View, width: Int): Int {
        text.measure(exactly(width), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        return text.measuredHeight
    }

    private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size.coerceAtLeast(0), MeasureSpec.EXACTLY)

    companion object {
        /** The video's width to height (avatar_end.mp4 is 480 x 854). */
        const val VIDEO_ASPECT = 480f / 854f
        const val MAX_SHARE = 0.8f
        private const val MIN_VIDEO_DP = 96
        private const val TEXT_MIN_WIDTH_DP = 300
    }
}
