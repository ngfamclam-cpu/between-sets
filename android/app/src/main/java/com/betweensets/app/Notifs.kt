package com.betweensets.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.widget.RemoteViews

object Notifs {
    // Default importance (no sound): phones hide "silent" notifications from the lock screen.
    private const val CH_CONTROLS = "controls"
    private const val CH_OLD_RUNNING = "running"
    private const val CH_DONE = "rest_over"
    private const val ID_RUNNING = 1
    private const val ID_DONE = 2

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun createChannels(ctx: Context) {
        nm(ctx).deleteNotificationChannel(CH_OLD_RUNNING)
        nm(ctx).createNotificationChannel(
            NotificationChannel(CH_CONTROLS, ctx.getString(R.string.channel_controls), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
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

    /**
     * The always-there control panel in the notification shade and on the lock screen. Collapsed it
     * shows the presets (idle) or countdown + Pause/+15s/Stop (timing); expanded it shows all of them.
     * The buttons are broadcasts, so they work without unlocking.
     */
    @SuppressLint("MissingPermission")
    fun showOngoing(ctx: Context, s: TimerState) {
        if (!TimerStore.notifControls(ctx)) {
            nm(ctx).cancel(ID_RUNNING)
            return
        }
        createChannels(ctx)
        val now = System.currentTimeMillis()
        val small = RemoteViews(ctx.packageName, R.layout.notif_small)
        val big = RemoteViews(ctx.packageName, R.layout.notif_big)
        TimerViews.fill(ctx, small, s, compact = true)
        TimerViews.fill(ctx, big, s, compact = false)
        val title = when (s.status) {
            Status.RUNNING -> ctx.getString(R.string.notif_resting, formatClock(s.durationMs))
            Status.PAUSED -> ctx.getString(R.string.notif_paused, formatClock(s.leftMs(now)))
            Status.IDLE, Status.DONE -> ctx.getString(R.string.notif_idle)
        }
        val b = Notification.Builder(ctx, CH_CONTROLS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setColor(ctx.getColor(R.color.accent))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            // SECRET keeps it off the lock screen but still in the notification shade.
            .setVisibility(
                if (TimerStore.lockScreenControls(ctx)) Notification.VISIBILITY_PUBLIC
                else Notification.VISIBILITY_SECRET
            )
            .setContentIntent(TimerController.openAppIntent(ctx))
            // Android 14+ lets you swipe ongoing notifications away; put the controls straight back.
            .setDeleteIntent(TimerReceiver.pendingIntent(ctx, TimerReceiver.REFRESH))
            .setContentTitle(title)
            .setContentText(
                if (s.auto) ctx.getString(R.string.notif_auto, s.sets + 1)
                else ctx.getString(R.string.notif_set, s.sets + 1)
            )
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
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
