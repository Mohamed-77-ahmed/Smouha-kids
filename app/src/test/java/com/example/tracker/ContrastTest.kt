package com.example.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow

/**
 * WCAG contrast of every text/background pair the screens use (CLAUDE.md 9), read from the real
 * color tokens in values/colors.xml and values-night/colors.xml. Normal text needs 4.5:1, icons,
 * borders and the level bar 3:1. On the gradient every stop (start, center, end) is checked, and
 * the frosted "glass" boxes are checked blended over each stop.
 */
class ContrastTest {

    private fun tokens(dir: String): Map<String, Long> {
        val file = File("src/main/res/$dir/colors.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("color")
        return (0 until nodes.length).associate { i ->
            val e = nodes.item(i)
            val hex = e.textContent.trim().removePrefix("#")
            e.attributes.getNamedItem("name").nodeValue to (if (hex.length == 6) "FF$hex" else hex).toLong(16)
        }
    }

    private fun channel(c: Long, shift: Int) = ((c shr shift) and 0xFF) / 255.0
    private fun linear(v: Double) = if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    private fun luminance(c: Long) =
        0.2126 * linear(channel(c, 16)) + 0.7152 * linear(channel(c, 8)) + 0.0722 * linear(channel(c, 0))

    fun ratio(a: Long, b: Long): Double {
        val (l1, l2) = luminance(a) to luminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    /** [over] (with alpha) painted on the opaque [under]. */
    private fun blend(over: Long, under: Long): Long {
        val a = ((over shr 24) and 0xFF) / 255.0
        fun mix(shift: Int) = ((channel(over, shift) * a + channel(under, shift) * (1 - a)) * 255).toLong()
        return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    private val surfaces = listOf("surface", "surface_lowest", "surface_low", "surface_container", "surface_high", "surface_highest", "surface_variant")
    private val gradientStops = listOf("gradient_start", "gradient_center", "gradient_end")

    /** (text, background) pairs that must reach 4.5:1. */
    private val pairs: List<Pair<String, String>> = buildList {
        listOf("primary", "secondary", "tertiary", "error").forEach { add("on_$it" to it) }
        listOf("primary_container", "secondary_container", "tertiary_container", "error_container", "warning_container").forEach { add("on_$it" to it) }
        add("on_background" to "background")
        for (bg in surfaces) {
            listOf("on_surface", "on_surface_variant", "error", "success", "points", "streak", "primary").forEach { add(it to bg) }
        }
        listOf("surface", "surface_lowest", "surface_low").forEach { bg ->
            listOf("secondary", "tertiary", "warning").forEach { add(it to bg) }
        }
        gradientStops.forEach { add("on_gradient" to it) }
    }

    /** (icon or border, background) pairs that must reach 3:1: outlines (chips, outlined buttons,
     *  empty PIN dots), the warning row's icon and border, the level bar on the header. */
    private val uiPairs: List<Pair<String, String>> = buildList {
        surfaces.forEach { add("outline" to it) }
        add("warning" to "warning_container")
        gradientStops.forEach { add("progress_on_gradient" to it) }
    }

    private fun failures(dir: String): List<String> {
        val t = tokens(dir)
        val plain = pairs.mapNotNull { (fg, bg) ->
            val r = ratio(t.getValue(fg), t.getValue(bg))
            if (r < 4.5) "$dir: $fg on $bg = %.2f".format(r) else null
        }
        val ui = uiPairs.mapNotNull { (fg, bg) ->
            val r = ratio(t.getValue(fg), t.getValue(bg))
            if (r < 3.0) "$dir: $fg on $bg = %.2f (3:1 needed)".format(r) else null
        }
        val glass = gradientStops.flatMap { stop ->
            val box = blend(t.getValue("glass"), t.getValue(stop))
            listOfNotNull(
                ratio(t.getValue("on_gradient"), box).let { if (it < 4.5) "$dir: on_gradient on glass over $stop = %.2f".format(it) else null },
                ratio(t.getValue("progress_on_gradient"), box).let { if (it < 3.0) "$dir: progress_on_gradient on glass over $stop = %.2f".format(it) else null },
            )
        }
        return plain + ui + glass
    }

    /** The tokens checked above are the ones painted: every gradient drawable uses all three stops. */
    @Test
    fun gradientDrawablesUseTheCheckedStops() {
        val drawables = File("src/main/res/drawable").listFiles { f -> f.name.endsWith(".xml") }!!
            .filter { it.readText().contains("<gradient") && it.readText().contains("@color/gradient_") }
        val names = drawables.map { it.name }.sorted()
        assertEquals(listOf("bg_celebration.xml", "bg_fab.xml", "bg_header.xml", "bg_header_full.xml", "bg_lock_circle.xml", "bg_status_scrim.xml"), names)
        for (f in drawables) {
            val g = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f).getElementsByTagName("gradient").item(0)
            fun attr(name: String) = g.attributes.getNamedItem("android:$name")?.nodeValue
            val prefix = if (f.name == "bg_celebration.xml") "@color/gradient_scrim_" else "@color/gradient_"
            assertEquals(f.name, listOf("${prefix}start", "${prefix}center", "${prefix}end"), listOf(attr("startColor"), attr("centerColor"), attr("endColor")))
        }
    }

    /** The celebration backdrop is the same gradient, only see-through. */
    @Test
    fun scrimStopsAreTheGradientStops() {
        for (dir in listOf("values", "values-night")) {
            val t = tokens(dir)
            for (stop in listOf("start", "center", "end")) {
                assertEquals("$dir $stop", t.getValue("gradient_$stop") and 0xFFFFFFL, t.getValue("gradient_scrim_$stop") and 0xFFFFFFL)
            }
        }
    }

    @Test
    fun lightPaletteMeetsContrast() {
        val f = failures("values")
        assertTrue(f.joinToString("\n"), f.isEmpty())
    }

    @Test
    fun darkPaletteMeetsContrast() {
        val f = failures("values-night")
        assertTrue(f.joinToString("\n"), f.isEmpty())
    }

    /** The course-complete card (batch 6): white text on the video's navy, the close icon visible on any frame. */
    @Test
    fun courseEndCardIsReadableOnAnyFrame() {
        val t = tokens("values")
        assertTrue(ratio(t.getValue("on_course_card"), t.getValue("course_card")) >= 4.5)
        // The icon's dark round patch over the brightest and the darkest possible frame.
        for (frame in listOf(0xFFFFFFFFL, 0xFF000000L)) {
            val patch = blend(t.getValue("course_close_bg"), frame)
            val r = ratio(t.getValue("on_course_card"), patch)
            assertTrue("close icon on its patch over ${frame.toString(16)}: %.2f".format(r), r >= 3.0)
        }
        // The same card in dark mode: the video's colors do not change.
        val night = tokens("values-night").keys
        assertTrue(listOf("course_card", "on_course_card", "course_close_bg").none { it in night })
    }

    @Test
    fun ratioMatchesKnownValues() {
        // Black on white is 21:1, white on white 1:1 (sanity check of the formula).
        assertTrue(ratio(0xFF000000, 0xFFFFFFFF) in 20.99..21.01)
        assertTrue(ratio(0xFFFFFFFF, 0xFFFFFFFF) in 0.99..1.01)
    }
}
