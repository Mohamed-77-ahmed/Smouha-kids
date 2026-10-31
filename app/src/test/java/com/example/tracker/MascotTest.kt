package com.example.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The vector mascot (CLAUDE.md 9): two expressions (the cheering one left with the celebration avatar,
 * batch 6), palette tokens only (no raw colors), the face readable on both body halves and the body
 * visible on the overlay card in light and dark mode.
 */
class MascotTest {

    private val expressions = listOf("mascot_happy", "mascot_sleepy")

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun tokens(dir: String): Map<String, Long> {
        val nodes = parse(File("src/main/res/$dir/colors.xml")).getElementsByTagName("color")
        return (0 until nodes.length).associate { i ->
            val e = nodes.item(i) as Element
            val hex = e.textContent.trim().removePrefix("#")
            e.getAttribute("name") to (if (hex.length == 6) "FF$hex" else hex).toLong(16)
        }
    }

    private val light = tokens("values")
    private val dark = light + tokens("values-night")
    private fun ratio(a: Long, b: Long) = ContrastTest().ratio(a, b)

    private fun colorsUsed(name: String): List<String> {
        val nodes = parse(File("src/main/res/drawable/$name.xml")).getElementsByTagName("path")
        return (0 until nodes.length).flatMap { i ->
            val e = nodes.item(i) as Element
            listOf("android:fillColor", "android:strokeColor").map { e.getAttribute(it) }.filter { it.isNotEmpty() }
        }
    }

    private fun faceOf(name: String): String {
        val nodes = parse(File("src/main/res/drawable/$name.xml")).getElementsByTagName("path")
        return (0 until nodes.length).joinToString("|") { (nodes.item(it) as Element).getAttribute("android:pathData") }
    }

    @Test
    fun vectorExpressionsWithPaletteTokensOnly() {
        for (name in expressions) {
            val used = colorsUsed(name)
            assertTrue("$name draws something", used.isNotEmpty())
            for (c in used) {
                assertTrue("$name uses $c, only @color/mascot_* tokens allowed", c.startsWith("@color/mascot_"))
                assertTrue("$name: token $c exists", light.containsKey(c.removePrefix("@color/")))
            }
        }
        // Different expressions, not one picture twice.
        val faces = expressions.map(::faceOf)
        assertEquals(expressions.size, faces.toSet().size)
    }

    @Test
    fun faceIsReadableAndTheBodyStandsOutOnTheCard() {
        val line = light.getValue("mascot_line")
        val top = light.getValue("mascot_top")
        val bottom = light.getValue("mascot_bottom")
        assertTrue("outline on top half", ratio(line, top) >= 4.5)
        assertTrue("outline on bottom half", ratio(line, bottom) >= 4.5)
        for ((mode, palette) in listOf("light" to light, "dark" to dark)) {
            val card = palette.getValue("surface_lowest")
            val best = maxOf(ratio(line, card), ratio(top, card))
            assertTrue("$mode: mascot vs card ${"%.2f".format(best)}", best >= 3.0)
        }
        // The mascot keeps its colors in dark mode (no night values).
        assertTrue(tokens("values-night").keys.none { it.startsWith("mascot_") })
    }
}
