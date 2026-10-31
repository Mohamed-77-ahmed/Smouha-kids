package com.example.tracker.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.tracker.R
import com.example.tracker.databinding.ActivityAboutBinding

/**
 * About (CLAUDE.md 1): the hospital's name, the version, what the app is, the disclaimer (a reminder
 * only, no medical advice) and that the data stays on the phone. Opens without the PIN.
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val b = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        b.toolbar.setNavigationOnClickListener { finish() }
        Responsive.limitWidth(b.content)
        b.version.text = versionLabel(this)
    }

    companion object {
        /** "Version 1.2.0 (3)", read from the installed package (also shown in parent settings). */
        fun versionLabel(context: Context): String {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            return context.getString(R.string.app_version, info.versionName, PackageInfoCompat.getLongVersionCode(info))
        }
    }
}
