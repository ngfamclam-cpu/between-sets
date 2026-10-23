package com.betweensets.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

/** Home screen widget: live countdown, Pause/+15s/Stop, and the five preset plates. */
class TimerWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        updateAll(ctx, TimerController.current(ctx))
    }

    companion object {
        fun updateAll(ctx: Context, s: TimerState) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TimerWidget::class.java))
            if (ids.isEmpty()) return
            val rv = RemoteViews(ctx.packageName, R.layout.widget_timer)
            TimerViews.fill(ctx, rv, s, compact = false)
            rv.setOnClickPendingIntent(R.id.time_area, TimerController.openAppIntent(ctx))
            mgr.updateAppWidget(ids, rv)
        }
    }
}
