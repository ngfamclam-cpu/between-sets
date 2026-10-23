package com.betweensets.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the alarm plus every button on the widget and the notifications. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            FINISH -> {
                // Keep the process alive until the chime has finished playing.
                val pending = goAsync()
                if (TimerController.onAlarm(ctx)) Chime.play(ctx.applicationContext) { pending.finish() }
                else pending.finish()
            }
            START -> TimerController.start(ctx, intent.getIntExtra(EXTRA_SECONDS, 90))
            TOGGLE -> TimerController.toggle(ctx)
            PLUS -> TimerController.plus(ctx)
            STOP -> TimerController.stop(ctx)
            REFRESH -> Notifs.showOngoing(ctx, TimerController.current(ctx))
        }
    }

    companion object {
        const val FINISH = "com.betweensets.app.FINISH"
        const val START = "com.betweensets.app.START"
        const val TOGGLE = "com.betweensets.app.TOGGLE"
        const val PLUS = "com.betweensets.app.PLUS"
        const val STOP = "com.betweensets.app.STOP"
        const val REFRESH = "com.betweensets.app.REFRESH"
        const val EXTRA_SECONDS = "seconds"

        fun pendingIntent(ctx: Context, action: String, seconds: Int = 0): PendingIntent =
            PendingIntent.getBroadcast(
                ctx,
                action.hashCode() + seconds,
                Intent(ctx, TimerReceiver::class.java).setAction(action).putExtra(EXTRA_SECONDS, seconds),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // The app isn't open after a restart or update until you open it again.
            TimerStore.setAppOpen(ctx, false)
            TimerController.restore(ctx)
        }
    }
}
