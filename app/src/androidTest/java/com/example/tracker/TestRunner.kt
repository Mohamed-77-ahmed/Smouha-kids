package com.example.tracker

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.example.tracker.data.db.AppDatabase

/** The app under test, with an in-memory DB: UI tests never read or change the device's real data. */
class TestApp : PillPalsApp() {
    override fun createDatabase(): AppDatabase = AppDatabase.inMemory(this)
}

class TestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestApp::class.java.name, context)
}
