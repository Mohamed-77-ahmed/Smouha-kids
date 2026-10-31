package com.example.tracker

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.databinding.ViewCelebrationBinding
import com.example.tracker.domain.AgeGroup
import com.example.tracker.ui.AboutActivity
import com.example.tracker.ui.Celebration
import com.example.tracker.ui.Mascots
import com.example.tracker.ui.Motion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The reward and streak-ended overlay (CLAUDE.md 9, 6b; batch 6): CHILD and TEEN rewards show the
 * celebration avatar, ADULT keeps its checkmark; the CHILD streak-ended screen keeps the vector
 * mascot (sleepy, then happy); the idle motion stops when the overlay closes and never runs with
 * animations off, and the overlay still closes by itself.
 */
@RunWith(AndroidJUnit4::class)
class CelebrationUiTest {

    private val instr = InstrumentationRegistry.getInstrumentation()

    private fun reward(group: AgeGroup) = ConfirmOutcome.Confirmed(
        patientId = 1, patientName = "Yasmin", ageGroup = group, gender = null, medicineName = "Panadol",
        dosePoints = 10, onTimeBonus = 5, dosePaid = true, perfectDayBonus = 0, milestone = null,
        milestoneBonus = 0, liveStreak = 1, totalPoints = 15, dayComplete = false,
    )

    private fun <T> onMain(f: () -> T): T {
        var v: Any? = null
        instr.runOnMainSync { v = f() }
        @Suppress("UNCHECKED_CAST")
        return v as T
    }

    /** The overlay on a plain screen (About), so no data or PIN is involved. */
    private fun withOverlay(block: (Celebration, ViewCelebrationBinding) -> Unit) {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            lateinit var c: Celebration
            lateinit var b: ViewCelebrationBinding
            scenario.onActivity { a ->
                b = ViewCelebrationBinding.inflate(a.layoutInflater, a.findViewById<ViewGroup>(android.R.id.content), true)
                c = Celebration(b)
            }
            block(c, b)
        }
    }

    private fun art(b: ViewCelebrationBinding): Any? = onMain { b.mascotArt.tag }

    private fun shell(cmd: String): String =
        instr.uiAutomation.executeShellCommand(cmd).use { pfd ->
            java.io.FileInputStream(pfd.fileDescriptor).bufferedReader().readText().trim()
        }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end) {
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    /** The avatar view shows exactly the celebration picture. */
    private fun showsAvatar(b: ViewCelebrationBinding): Boolean = onMain {
        val shown = (b.avatarArt.drawable as? BitmapDrawable)?.bitmap
        val avatar = BitmapFactory.decodeResource(b.avatarArt.resources, R.drawable.avatar_celebration, BitmapFactory.Options().apply { inScaled = false })
        b.avatarArt.isVisible && shown != null && shown.sameAs(avatar)
    }

    @Test
    fun childRewardShowsTheCelebrationAvatarAndClosesAfterThreeSeconds() = withOverlay { c, b ->
        val shownAt = onMain {
            c.showReward(reward(AgeGroup.CHILD))
            SystemClock.uptimeMillis()
        }
        // One trip to the main thread (it is busy with confetti on a slow emulator), then the checks.
        val seen = onMain {
            listOf(b.mascotArt.isVisible, b.mascot.isVisible, b.checkmark.isVisible, b.avatarArt.contentDescription == b.avatarArt.context.getString(R.string.celebration_avatar_desc))
        }
        assertEquals("no mascot, no emoji badge, no checkmark, the avatar's description", listOf(false, false, false, true), seen)
        assertTrue("the celebration avatar for a child", showsAvatar(b))
        // Without a sound the reward stays 3 s, counted from when it was shown: there at 2.6 s, gone at 3.6 s.
        fun sleepUntil(uptime: Long) = (uptime - SystemClock.uptimeMillis()).let { if (it > 0) Thread.sleep(it) }
        sleepUntil(shownAt + 2_600)
        val (showing, at) = onMain { c.isShowing to SystemClock.uptimeMillis() - shownAt }
        if (at < 2_950) assertTrue("still showing at $at ms", showing)
        sleepUntil(shownAt + 3_600)
        assertFalse(onMain { c.isShowing })
        assertFalse("no idle motion left running", onMain { Motion.isIdling(b.mascotArt) })
    }

    @Test
    fun teenGetsTheAvatarAndAdultKeepsItsCalmStyle() = withOverlay { c, b ->
        onMain { c.showReward(reward(AgeGroup.TEEN)) }
        assertTrue("the celebration avatar for a teen", showsAvatar(b))
        assertFalse("no emoji badge any more", onMain { b.mascot.isVisible })
        assertFalse(onMain { b.mascotArt.isVisible })
        onMain { c.showReward(reward(AgeGroup.ADULT)) }
        assertFalse("no avatar for an adult", onMain { b.avatarArt.isVisible })
        assertFalse(onMain { b.mascotArt.isVisible })
        assertFalse(onMain { b.mascot.isVisible })
        assertTrue(onMain { b.checkmark.isVisible })
        onMain { c.showStreakBroken("Yasmin", AgeGroup.ADULT, null, 4, 6) }
        assertFalse(onMain { b.avatarArt.isVisible })
        assertFalse(onMain { b.mascotArt.isVisible })
        assertFalse(onMain { Motion.isIdling(b.mascotArt) })
    }

    @Test
    fun childStreakEndSleepsThenWakesUpHappy() = withOverlay { c, b ->
        onMain { c.showStreakBroken("Yasmin", AgeGroup.CHILD, null, 4, 6) }
        assertEquals(Mascots.ART_SLEEPY, art(b))
        assertFalse("the streak-ended screen keeps the mascot, not the avatar", onMain { b.avatarArt.isVisible })
        Thread.sleep(1_900)
        assertEquals(Mascots.ART_HAPPY, art(b))
        assertTrue("still showing before 3 s", onMain { c.isShowing })
    }

    @Test
    fun aNewOverlayCancelsThePendingWakeUp() = withOverlay { c, b ->
        onMain { c.showStreakBroken("Yasmin", AgeGroup.CHILD, null, 4, 6) }
        onMain { c.showReward(reward(AgeGroup.CHILD)) }
        Thread.sleep(1_900)
        assertFalse("the streak-ended wake-up must not bring the mascot onto the reward", onMain { b.mascotArt.isVisible })
        assertTrue(showsAvatar(b))
    }

    @Test
    fun emptyStateMascotBreathesAFewTimesThenRests() {
        val app = ApplicationProvider.getApplicationContext<PillPalsApp>()
        runBlocking {
            withContext(Dispatchers.IO) { app.database.clearAllTables() }
            app.repository.setLanguage("en")
            app.repository.setPin("1234")
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var mascot: android.view.View
            waitUntil("empty state shown") {
                var shown = false
                scenario.onActivity { shown = it.findViewById<android.view.View>(R.id.emptyState).isVisible }
                shown
            }
            scenario.onActivity { mascot = it.findViewById(R.id.emptyMascot) }
            // The breathing was started for the empty state, read from the view rather than caught
            // mid-breath: launch() and onActivity() wait for an idle main thread, which a busy
            // emulator may only reach after all the breaths (seen once in a full suite run).
            val breath = onMain { mascot.getTag(R.id.idle_motion) as? android.animation.Animator }
            assertEquals("the idle motion was started", Motion.animationsOn(), breath != null)
            // 3 breaths of 1.6 s in and 1.6 s out: a finite animation, never an endless one.
            if (breath != null) assertEquals(Motion.IDLE_BREATHS * 3_200L, onMain { breath.totalDuration })
            // Then it must have stopped by itself, exactly at rest.
            Thread.sleep(Motion.IDLE_BREATHS * 3_200L + 1_500L)
            assertFalse("the idle motion must not run forever", onMain { Motion.isIdling(mascot) })
            assertEquals(1f, onMain { mascot.scaleY }, 0.001f)
        }
    }

    @Test
    fun animationsOffNoIdleMotionAndTheOverlayStillCloses() {
        val before = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 0")
            waitUntil("animations off in the app") { !ValueAnimator.areAnimatorsEnabled() }
            withOverlay { c, b ->
                onMain { c.showReward(reward(AgeGroup.CHILD)) }
                assertTrue(showsAvatar(b))
                // No spring: the avatar is at full size right away.
                instr.waitForIdleSync()
                assertEquals(1f, onMain { b.avatarArt.scaleX }, 0f)
                assertEquals(1f, onMain { b.avatarArt.scaleY }, 0f)
                onMain { c.showStreakBroken("Yasmin", AgeGroup.CHILD, null, 4, 6) }
                assertFalse(onMain { Motion.isIdling(b.mascotArt) })
                instr.waitForIdleSync()
                assertEquals(1f, onMain { b.mascotArt.scaleY }, 0f)
                Thread.sleep(3_600)
                assertFalse(onMain { c.isShowing })
            }
        } finally {
            if (before == "null" || before.isEmpty()) shell("settings delete global animator_duration_scale")
            else shell("settings put global animator_duration_scale $before")
            // The app process learns the restored scale a moment later; the next test must not start before.
            if (before != "0" && before != "0.0") waitUntil("animations back on") { ValueAnimator.areAnimatorsEnabled() }
        }
    }
}
