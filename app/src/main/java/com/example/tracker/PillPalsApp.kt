package com.example.tracker

import android.app.Application
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.reminders.AlarmScheduler
import com.example.tracker.reminders.Notifications
import com.example.tracker.ui.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

open class PillPalsApp : Application() {

    val database: AppDatabase by lazy { createDatabase() }
    val repository: TrackerRepository by lazy { TrackerRepository(database, DeviceClock) }
    val alarms: AlarmScheduler by lazy { AlarmScheduler(this, repository) }

    /** Work that must finish even when no screen is open (receivers, background saves). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** UI tests replace this with an in-memory DB, so they never touch the data on the device. */
    protected open fun createDatabase(): AppDatabase = AppDatabase.build(this)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            AppLanguage.syncWithSystem(this@PillPalsApp, repository)
            Notifications.ensureChannel(this@PillPalsApp, repository.settings().languageTag)
        }
    }

    /**
     * Back to a fresh install: all data, alarms, notifications and cache files are gone. The app
     * language on screen stays until the language is chosen again on the first screen.
     */
    suspend fun eraseAllData() {
        alarms.cancel(repository.eraseAllData())
        NotificationManagerCompat.from(this).cancelAll()
        withContext(Dispatchers.IO) { cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
    }
}

val Context.app: PillPalsApp get() = applicationContext as PillPalsApp

/** System clock that always uses the current default zone, so a timezone change is picked up. */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
}
