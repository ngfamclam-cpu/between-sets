package com.betweensets.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon

object Notifs {
    private const val CH_RUNNING = "running"
    private const val CH_DONE = "rest_over"
    private const val ID_RUNNING = 1
    private const val ID_DONE = 2

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun createChannels(ctx: Context) {
        nm(ctx).createNotificationChannel(
            NotificationChannel(CH_RUNNING, ctx.getString(R.string.channel_running), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        // The chime itself is played by Chime on the alarm stream; this channel only pops up and buzzes.
        nm(ctx).createNotificationChannel(
            NotificationChannel(CH_DONE, ctx.getString(R.string.channel_done), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 120, 300, 120, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private fun action(ctx: Context, label: Int, action: String) = Notification.Action.Builder(
        Icon.createWithResource(ctx, R.drawable.ic_stat_timer),
        ctx.getString(label),
        TimerReceiver.pendingIntent(ctx, action),
    ).build()

    /** The lock-screen notification: live countdown plus Pause, +15s and Stop. */
    @SuppressLint("MissingPermission")
    fun showOngoing(ctx: Context, s: TimerState) {
        if (s.status == Status.IDLE || s.status == Status.DONE) {
            nm(ctx).cancel(ID_RUNNING)
            return
        }
        createChannels(ctx)
        val now = System.currentTimeMillis()
        val running = s.status == Status.RUNNING
        val b = Notification.Builder(ctx, CH_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setColor(ctx.getColor(R.color.accent))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(TimerController.openAppIntent(ctx))
            .setContentText(
                if (s.auto) ctx.getString(R.string.notif_auto, s.sets + 1)
                else ctx.getString(R.string.notif_set, s.sets + 1)
            )
            .addAction(action(ctx, if (running) R.string.pause else R.string.resume, TimerReceiver.TOGGLE))
            .addAction(action(ctx, R.string.plus15, TimerReceiver.PLUS))
            .addAction(action(ctx, R.string.stop, TimerReceiver.STOP))
        if (running) {
            b.setContentTitle(ctx.getString(R.string.notif_resting, formatClock(s.durationMs)))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(s.endAt)
                .setShowWhen(true)
        } else {
            b.setContentTitle(ctx.getString(R.string.notif_paused, formatClock(s.leftMs(now))))
                .setShowWhen(false)
        }
        nm(ctx).notify(ID_RUNNING, b.build())
    }

    @SuppressLint("MissingPermission")
    fun showChime(ctx: Context, s: TimerState) {
        createChannels(ctx)
        val b = Notification.Builder(ctx, CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setColor(ctx.getColor(R.color.accent))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(TimerController.openAppIntent(ctx))
            .setContentTitle(ctx.getString(R.string.done_title, s.sets))
        if (s.auto) {
            b.setContentText(ctx.getString(R.string.done_auto, formatClock(s.durationMs)))
        } else {
            b.setContentText(ctx.getString(R.string.done_manual))
                .addAction(action(ctx, R.string.go_again, TimerReceiver.TOGGLE))
        }
        nm(ctx).notify(ID_DONE, b.build())
    }

    fun cancelChime(ctx: Context) = nm(ctx).cancel(ID_DONE)
}
