package com.betweensets.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) TimerController.restore(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifs.createChannels(this)
        // Also puts the notification controls and widget back up.
        TimerController.restore(this)
        if (savedInstanceState == null) handleShortcut(intent)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent { TimerScreen() }
        Updater.start(this)
    }

    override fun onResume() {
        super.onResume()
        TimerController.current(this)
        Updater.checkNow(this)
    }

    override fun onStop() {
        super.onStop()
        Updater.installIfSilent(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcut(intent)
    }

    /** Long-press app shortcuts open betweensets://start/<seconds>. */
    private fun handleShortcut(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "betweensets" && uri.host == "start") {
            uri.lastPathSegment?.toIntOrNull()?.let { TimerController.start(this, it) }
        }
    }
}
