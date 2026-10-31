package com.example.tracker

import com.example.tracker.ui.Responsive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen-size rules (CLAUDE.md 9) for the sizes in the test matrix. */
class ResponsiveTest {

    @Test
    fun twoColumnsFrom840dp() {
        assertEquals(1, Responsive.columns(320)) // 480x800 @240
        assertEquals(1, Responsive.columns(411)) // 1080x2400 @420
        assertEquals(1, Responsive.columns(800)) // 1600x2560 @320
        assertEquals(1, Responsive.columns(839))
        assertEquals(2, Responsive.columns(840))
        assertEquals(2, Responsive.columns(841)) // 2208x1840 @420
    }

    @Test
    fun contentIsAtMost600dpAndCentered() {
        // 2.0 px per dp: 600dp = 1200px. A phone (800px wide) gets no extra padding.
        assertEquals(0, Responsive.extraSidePadding(widthPx = 800, basePaddingPx = 48, maxPx = 1200))
        // A tablet (1600px wide, 48px base): (1600 - 48 - 1200) / 2 = 176 on each side.
        assertEquals(176, Responsive.extraSidePadding(widthPx = 1600, basePaddingPx = 48, maxPx = 1200))
    }

    @Test
    fun narrowCardsMoveTheButtonUnderTheName() {
        assertFalse(Responsive.isNarrow(411, 1, 1.0f)) // normal phone, normal font: one row
        assertTrue(Responsive.isNarrow(411, 1, 1.5f)) // large font on a phone
        assertTrue(Responsive.isNarrow(320, 1, 1.0f)) // small phone
        assertFalse(Responsive.isNarrow(800, 1, 2.0f)) // tablet, biggest font: 400dp is enough
        assertTrue(Responsive.isNarrow(841, 2, 1.5f)) // unfolded, two columns, large font
    }
}
