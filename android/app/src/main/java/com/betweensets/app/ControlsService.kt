package com.betweensets.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * "While app is open" mode: holds the control panel as its foreground notification from opening the
 * app until it's swiped out of recent apps (onTaskRemoved). Background, lock screen and other apps
 * all count as open.
 */
class ControlsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DETACH) {
            // Switching to "Always": leave the notification up as an ordinary one.
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
            return START_NOT_STICKY
        }
        TimerStore.setAppOpen(this, true)
        val n = Notifs.buildControls(this, TimerController.current(this))
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifs.ID_CONTROLS, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifs.ID_CONTROLS, n)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        TimerStore.setAppOpen(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        TimerStore.setAppOpen(this, false)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_DETACH = "com.betweensets.app.DETACH"

        /** Only call while the app is on screen: Android refuses to start it from the background. */
        fun start(ctx: Context) {
            if (TimerStore.notifMode(ctx) != NotifMode.APP_OPEN) return
            TimerStore.setAppOpen(ctx, true)
            runCatching { ctx.startForegroundService(Intent(ctx, ControlsService::class.java)) }
        }

        fun detach(ctx: Context) {
            runCatching { ctx.startService(Intent(ctx, ControlsService::class.java).setAction(ACTION_DETACH)) }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ControlsService::class.java))
        }
    }
}
