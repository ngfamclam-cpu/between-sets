package com.betweensets.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

/** Home screen widget: live countdown, Pause/+15s/Stop, and the five preset plates. */
class TimerWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        updateAll(ctx, TimerController.current(ctx))
    }

    companion object {
        private val PRESET_IDS = listOf(R.id.p30, R.id.p60, R.id.p90, R.id.p120, R.id.p180)

        fun updateAll(ctx: Context, s: TimerState) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TimerWidget::class.java))
            if (ids.isEmpty()) return
            mgr.updateAppWidget(ids, views(ctx, s))
        }

        private fun views(ctx: Context, s: TimerState): RemoteViews {
            val now = System.currentTimeMillis()
            val running = s.status == Status.RUNNING
            val rv = RemoteViews(ctx.packageName, R.layout.widget_timer)

            // A Chronometer counts down on its own, so the widget stays live without the app running.
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

            rv.setOnClickPendingIntent(R.id.time_area, TimerController.openAppIntent(ctx))
            rv.setOnClickPendingIntent(R.id.btn_toggle, TimerReceiver.pendingIntent(ctx, TimerReceiver.TOGGLE))
            rv.setOnClickPendingIntent(R.id.btn_plus, TimerReceiver.pendingIntent(ctx, TimerReceiver.PLUS))
            rv.setOnClickPendingIntent(R.id.btn_stop, TimerReceiver.pendingIntent(ctx, TimerReceiver.STOP))
            PRESET_IDS.zip(PRESETS).forEach { (id, sec) ->
                rv.setOnClickPendingIntent(id, TimerReceiver.pendingIntent(ctx, TimerReceiver.START, sec))
            }
            return rv
        }
    }
}
