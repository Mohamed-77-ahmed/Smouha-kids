package com.example.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs

/**
 * The launcher icon and the splash (CLAUDE.md 9, batch 6): the Koko rooster over a gradient of its own
 * sky blue, no monochrome layer (no clean silhouette exists), the old capsule files gone, and on the
 * splash the rooster with the app name as dark text that stays readable on the sky blue. The drawables,
 * the themes and the rendered name image are read, so the test checks what is really shipped.
 */
class IconContrastTest {

    private val contrast = ContrastTest()
    private val res = "src/main/res"

    private fun xml(path: String): Element =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
            .newDocumentBuilder().parse(File("$res/$path")).documentElement

    private fun tokens(): Map<String, Long> {
        val nodes = xml("values/colors.xml").getElementsByTagName("color")
        return (0 until nodes.length).associate { i ->
            val e = nodes.item(i) as Element
            val hex = e.textContent.trim().removePrefix("#")
            e.getAttribute("name") to (if (hex.length == 6) "FF$hex" else hex).toLong(16)
        }
    }

    private fun attributes(path: String, tag: String, attribute: String): List<String> {
        val nodes = xml(path).getElementsByTagName(tag)
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute(attribute) }
    }

    private fun rgb(c: Long) = Triple(((c shr 16) and 0xFF).toInt(), ((c shr 8) and 0xFF).toInt(), (c and 0xFF).toInt())

    @Test
    fun adaptiveIconsAreTheRoosterWithoutAMonochromeLayer() {
        for (icon in listOf("mipmap-anydpi/ic_launcher.xml", "mipmap-anydpi/ic_launcher_round.xml")) {
            assertEquals(listOf("@drawable/ic_launcher_koko_background"), attributes(icon, "background", "android:drawable"))
            assertEquals(listOf("@drawable/ic_launcher_koko"), attributes(icon, "foreground", "android:drawable"))
            assertEquals("$icon: no themed-icon layer", 0, xml(icon).getElementsByTagName("monochrome").length)
        }
        assertTrue("the rooster foreground", File("$res/drawable-nodpi/ic_launcher_koko.png").isFile)
    }

    @Test
    fun iconBackgroundIsTheSampledSkyGradient() {
        val gradient = xml("drawable/ic_launcher_koko_background.xml").getElementsByTagName("gradient").item(0) as Element
        assertEquals("@color/koko_icon_bg_top", gradient.getAttribute("android:startColor"))
        assertEquals("@color/koko_icon_bg_bottom", gradient.getAttribute("android:endColor"))
        assertEquals("top to bottom", "270", gradient.getAttribute("android:angle"))
        val t = tokens()
        for (name in listOf("koko_icon_bg_top", "koko_icon_bg_bottom", "koko_sky")) {
            val (r, g, b) = rgb(t.getValue(name))
            // Light: more than 13:1 against black means a relative luminance above 0.6.
            val light = contrast.ratio(t.getValue(name), 0xFF000000L) > 13.0
            assertTrue("$name is a light sky blue (#%06X)".format(t.getValue(name) and 0xFFFFFF), b >= r + 10 && b >= g && light)
        }
    }

    @Test
    fun theOldCapsuleLauncherFilesAreGone() {
        for (old in listOf("drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_background.xml", "drawable/bg_icon_badge.xml")) {
            assertFalse("$old must be removed", File("$res/$old").exists())
        }
        val legacy = File(res).listFiles { f -> f.isDirectory && f.name.startsWith("mipmap-") && f.name != "mipmap-anydpi" }.orEmpty()
            .flatMap { it.listFiles().orEmpty().toList() }.filter { it.name.startsWith("ic_launcher") }
        assertEquals("no old bitmap launcher icons", emptyList<File>(), legacy)
    }

    @Test
    fun splashShowsTheRoosterAndTheNameInDarkReadableText() {
        val t = tokens()
        for (dir in listOf("values", "values-v31")) {
            val items = xml("$dir/themes.xml").getElementsByTagName("item")
            val byName = (0 until items.length).map { items.item(it) as Element }
            fun value(name: String) = byName.single { it.getAttribute("name") == name }.textContent.trim()
            assertEquals("$dir background", "@color/koko_sky", value("windowSplashScreenBackground"))
            // The adaptive launcher icon itself: Android draws it as a round badge with its own sky gradient.
            assertEquals("$dir icon", "@mipmap/ic_launcher", value("windowSplashScreenAnimatedIcon"))
        }
        val r = contrast.ratio(t.getValue("splash_name_text"), t.getValue("koko_sky"))
        assertTrue("splash name on the sky blue = %.2f, needs 4.5".format(r), r >= 4.5)
        // The rendered name images use that color (scripts/render-splash-name.ps1 was run after a change).
        val (er, eg, eb) = rgb(t.getValue("splash_name_text"))
        for (png in listOf("drawable-nodpi/splash_name.png", "drawable-ar-nodpi/splash_name.png")) {
            val img = ImageIO.read(File("$res/$png"))
            var solid = 0
            for (y in 0 until img.height) for (x in 0 until img.width) {
                val p = img.getRGB(x, y)
                if ((p ushr 24) != 0xFF) continue
                solid++
                val ok = abs(((p shr 16) and 0xFF) - er) <= 8 && abs(((p shr 8) and 0xFF) - eg) <= 8 && abs((p and 0xFF) - eb) <= 8
                assertTrue("$png: a solid text pixel at $x,$y is not splash_name_text", ok)
            }
            assertTrue("$png has text", solid > 2_000)
        }
    }
}
