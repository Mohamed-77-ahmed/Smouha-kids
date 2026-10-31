package com.example.tracker.ui

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Any screen size (CLAUDE.md 9): content at most [CONTENT_MAX_DP] wide and centered on wide
 * screens, dialogs at most [DIALOG_MAX_DP] wide, home in 2 columns from [TWO_COLUMNS_DP].
 */
object Responsive {
    const val CONTENT_MAX_DP = 600
    const val DIALOG_MAX_DP = 560
    const val TWO_COLUMNS_DP = 840

    /** Less than 640dp of height left for the content: pinned extras would crowd it out. */
    fun shortScreen(view: View): Boolean = view.resources.configuration.screenHeightDp < SHORT_SCREEN_DP

    private const val SHORT_SCREEN_DP = 640

    /**
     * Home dose cards narrower than [NARROW_CARD_DP] at the current font size (column width divided
     * by the font scale): the action moves under the name so nothing is squeezed.
     */
    fun narrowCards(view: View): Boolean {
        val config = view.resources.configuration
        return isNarrow(config.screenWidthDp, columns(config.screenWidthDp), config.fontScale)
    }

    private const val NARROW_CARD_DP = 340

    /** Home columns for the current window width. */
    fun homeColumns(view: View): Int = columns(view.resources.configuration.screenWidthDp)

    // Pure rules, unit-tested in ResponsiveTest.
    fun columns(widthDp: Int): Int = if (widthDp >= TWO_COLUMNS_DP) 2 else 1

    fun isNarrow(widthDp: Int, columns: Int, fontScale: Float): Boolean = widthDp / columns / fontScale < NARROW_CARD_DP

    /** Extra side padding (each side) so content is at most [maxPx] wide. */
    fun extraSidePadding(widthPx: Int, basePaddingPx: Int, maxPx: Int): Int = max(0, (widthPx - basePaddingPx - maxPx) / 2)

    /**
     * Keeps [view]'s content at most [maxDp] wide by growing its side padding equally on wide
     * screens; the padding from the layout stays the minimum. Re-checked on every size change.
     */
    fun limitWidth(view: View, maxDp: Int = CONTENT_MAX_DP) {
        val baseStart = view.paddingStart
        val baseEnd = view.paddingEnd
        val maxPx = (maxDp * view.resources.displayMetrics.density).roundToInt()
        fun apply(width: Int) {
            val extra = extraSidePadding(width, baseStart + baseEnd, maxPx)
            val start = baseStart + extra
            val end = baseEnd + extra
            if (view.paddingStart != start || view.paddingEnd != end) {
                view.setPaddingRelative(start, view.paddingTop, end, view.paddingBottom)
            }
        }
        view.addOnLayoutChangeListener { v, l, _, r, _, ol, _, or_, _ ->
            if (r - l != or_ - ol) v.post { apply(r - l) }
        }
        if (view.width > 0) apply(view.width)
    }

    /** Called from a dialog's onStart: never wider than [DIALOG_MAX_DP], phones unchanged. */
    fun limitDialogWidth(dialog: Dialog?) {
        val window = dialog?.window ?: return
        val metrics = window.context.resources.displayMetrics
        val maxPx = (DIALOG_MAX_DP * metrics.density).roundToInt()
        if (metrics.widthPixels > maxPx + (48 * metrics.density).roundToInt()) {
            window.setLayout(maxPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
