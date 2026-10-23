package com.betweensets.app

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

/**
 * Fills the countdown and buttons in the widget and the notification layouts. They all use the
 * same view ids, and every button sends a broadcast, so it works from the lock screen without unlocking.
 */
object TimerViews {
    private val PRESET_IDS = listOf(R.id.p30, R.id.p60, R.id.p90, R.id.p120, R.id.p180)

    /** [compact]: the one-row collapsed notification, which shows presets when idle and controls when timing. */
    fun fill(ctx: Context, rv: RemoteViews, s: TimerState, compact: Boolean) {
        val now = System.currentTimeMillis()
        val running = s.status == Status.RUNNING

        // A Chronometer counts down on its own, so the view stays live without the app running.
        rv.setViewVisibility(R.id.chrono, if (running) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.time, if (running) View.GONE else View.VISIBLE)
        if (running) {
            rv.setChronometer(R.id.chrono, SystemClock.elapsedRealtime() + (s.endAt - now), null, true)
            rv.setChronometerCountDown(R.id.chrono, true)
        } else {
            rv.setChronometer(R.id.chrono, SystemClock.elapsedRealtime(), null, false)
            rv.setTextViewText(R.id.time, formatClock(s.leftMs(now)))
        }

        rv.setTextViewText(
            R.id.status,
            when (s.status) {
                Status.IDLE -> ctx.getString(R.string.w_ready, s.sets)
                Status.RUNNING -> ctx.getString(if (s.auto) R.string.w_auto else R.string.w_resting, s.sets)
                Status.PAUSED -> ctx.getString(R.string.w_paused, s.sets)
                Status.DONE -> ctx.getString(R.string.w_done, s.sets)
            },
        )
        rv.setTextViewText(
            R.id.btn_toggle,
            ctx.getString(
                when (s.status) {
                    Status.RUNNING -> R.string.pause
                    Status.PAUSED -> R.string.resume
                    Status.IDLE -> R.string.start
                    Status.DONE -> R.string.go_again
                }
            ),
        )

        if (compact) {
            val timing = running || s.status == Status.PAUSED
            rv.setViewVisibility(R.id.time_area, if (timing) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.row_controls, if (timing) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.row_presets, if (timing) View.GONE else View.VISIBLE)
        }

        rv.setOnClickPendingIntent(R.id.btn_toggle, TimerReceiver.pendingIntent(ctx, TimerReceiver.TOGGLE))
        rv.setOnClickPendingIntent(R.id.btn_plus, TimerReceiver.pendingIntent(ctx, TimerReceiver.PLUS))
        rv.setOnClickPendingIntent(R.id.btn_stop, TimerReceiver.pendingIntent(ctx, TimerReceiver.STOP))
        PRESET_IDS.zip(PRESETS).forEach { (id, sec) ->
            rv.setOnClickPendingIntent(id, TimerReceiver.pendingIntent(ctx, TimerReceiver.START, sec))
        }
    }
}
