package com.example.tracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * The real adaptive launcher icon, drawn by Android (CLAUDE.md 9, batch 6): every pixel of the rooster
 * and its props stays inside the 66dp safe zone, there is no themed-icon layer (D36), and the icon
 * reads under a circle and a squircle mask. The masked renders are saved for the evidence
 * (`files/icon-renders/`).
 */
@RunWith(AndroidJUnit4::class)
class LauncherIconTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val icon get() = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable

    /** Pixels per dp of the 108dp layer. */
    private val k = 4

    /** Draws a 108dp layer with its top-left corner at [offset] pixels. */
    private fun draw(layer: Drawable, offset: Int, into: Canvas) {
        layer.setBounds(offset, offset, offset + 108 * k, offset + 108 * k)
        layer.draw(into)
    }

    @Test
    fun roosterStaysInsideThe66dpSafeZone() {
        val size = 108 * k
        val back = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        draw(icon.background, 0, Canvas(back))
        val full = back.copy(Bitmap.Config.ARGB_8888, true)
        draw(icon.foreground, 0, Canvas(full))
        // "Ink" = where the foreground visibly changes the sky blue (the rooster, its props, the platform);
        // the crop's own sky blue and its faded border do not count.
        val center = size / 2f
        var ink = 0
        var farthest = 0f
        for (y in 0 until size) for (x in 0 until size) {
            val a = full.getPixel(x, y)
            val b = back.getPixel(x, y)
            val diff = max(abs(Color.red(a) - Color.red(b)), max(abs(Color.green(a) - Color.green(b)), abs(Color.blue(a) - Color.blue(b))))
            if (diff <= 48) continue
            ink++
            farthest = maxOf(farthest, hypot(x + 0.5f - center, y + 0.5f - center) / k)
        }
        assertTrue("the rooster is drawn ($ink pixels)", ink > 10_000)
        assertTrue("farthest rooster pixel is $farthest dp from the center, the safe zone radius is 33 dp", farthest <= 33f)
    }

    @Test
    fun noThemedIconLayer() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        assertNull("no monochrome layer (no clean silhouette, D36)", icon.monochrome)
    }

    @Test
    fun circleAndSquircleMasksShowTheRoosterOnTheSkyBlue() {
        val viewport = 72 * k
        val masks = mapOf("circle" to circle(viewport), "squircle" to squircle(viewport))
        val dir = File(context.getExternalFilesDir(null), "icon-renders").apply { mkdirs() }
        for ((name, mask) in masks) {
            val out = masked(mask, viewport)
            File(dir, "icon_$name.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            // Outside the mask: nothing. The center: the rooster's warm orange chest. Near the left edge
            // inside the mask: the opaque sky blue (more blue than red).
            assertEquals("$name corner", 0, Color.alpha(out.getPixel(2, 2)))
            val chest = out.getPixel(viewport / 2, viewport / 2)
            assertTrue("$name center is the rooster (#%08X)".format(chest), Color.red(chest) > Color.blue(chest) + 60)
            val edge = out.getPixel(4 * k, viewport / 2)
            assertEquals("$name sky opaque", 255, Color.alpha(edge))
            assertTrue("$name edge is sky blue (#%08X)".format(edge), Color.blue(edge) > Color.red(edge) + 10)
        }
    }

    /** Both layers at 108dp, centered on the 72dp viewport, cut by [mask] (as launchers do). */
    private fun masked(mask: Path, viewport: Int): Bitmap {
        val layers = Bitmap.createBitmap(viewport, viewport, Bitmap.Config.ARGB_8888)
        Canvas(layers).also {
            draw(icon.background, -18 * k, it)
            draw(icon.foreground, -18 * k, it)
        }
        val out = Bitmap.createBitmap(viewport, viewport, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawPath(mask, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
        canvas.drawBitmap(layers, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) })
        return out
    }

    private fun circle(viewport: Int) = Path().apply {
        addCircle(viewport / 2f, viewport / 2f, viewport / 2f, Path.Direction.CW)
    }

    /** The AOSP "squircle" icon shape (IconShapeSquircleOverlay), on a 100 x 100 grid. */
    private fun squircle(viewport: Int) = Path().apply {
        val s = viewport / 100f
        moveTo(50 * s, 0f)
        cubicTo(10 * s, 0f, 0f, 10 * s, 0f, 50 * s)
        cubicTo(0f, 90 * s, 10 * s, 100 * s, 50 * s, 100 * s)
        cubicTo(90 * s, 100 * s, 100 * s, 90 * s, 100 * s, 50 * s)
        cubicTo(100 * s, 10 * s, 90 * s, 0f, 50 * s, 0f)
        close()
    }
}
