package com.betweensets.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Every change to the timer goes through here, from the app, the widget, the notification
 * buttons and the alarm. It saves the state, then syncs the alarm, notification and widgets.
 */
object TimerController {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    private fun now() = System.currentTimeMillis()

    fun current(ctx: Context): TimerState = TimerStore.load(ctx).also { _state.value = it }

    private fun apply(ctx: Context, next: TimerState) {
        TimerStore.save(ctx, next)
        _state.value = next
        syncAlarm(ctx, next)
        Notifs.showOngoing(ctx, next)
        TimerWidget.updateAll(ctx, next)
    }

    fun start(ctx: Context, seconds: Int) {
        apply(ctx, TimerLogic.start(current(ctx), seconds * 1000L, now()))
        Notifs.cancelChime(ctx)
    }
    fun toggle(ctx: Context) {
        apply(ctx, TimerLogic.toggle(current(ctx), now()))
        Notifs.cancelChime(ctx)
    }
    fun plus(ctx: Context) = apply(ctx, TimerLogic.addTime(current(ctx), TimerLogic.PLUS_MS, now()))
    fun stop(ctx: Context) {
        apply(ctx, TimerLogic.stop(current(ctx)))
        Notifs.cancelChime(ctx)
    }
    fun setAuto(ctx: Context, on: Boolean) = apply(ctx, current(ctx).copy(auto = on))
    fun clearSets(ctx: Context) = apply(ctx, current(ctx).copy(sets = 0))

    /** The alarm fired. Returns true when a rest really ended and the chime should play. */
    fun onAlarm(ctx: Context): Boolean {
        val next = TimerLogic.finish(current(ctx), now()) ?: return false
        apply(ctx, next)
        Notifs.showChime(ctx, next)
        return true
    }

    /** After a reboot or app update the alarm is gone, so set everything up again. */
    fun restore(ctx: Context) {
        val s = current(ctx)
        if (s.status == Status.RUNNING && s.endAt <= now()) onAlarm(ctx) else apply(ctx, s)
    }

    private fun syncAlarm(ctx: Context, s: TimerState) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val op = TimerReceiver.pendingIntent(ctx, TimerReceiver.FINISH)
        am.cancel(op)
        if (s.status != Status.RUNNING) return
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // Exact alarms switched off in settings: still ring, maybe a little late.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.endAt, op)
        } else {
            // An alarm clock alarm is never delayed by Doze, so it rings on time with the phone locked.
            am.setAlarmClock(AlarmManager.AlarmClockInfo(s.endAt, openAppIntent(ctx)), op)
        }
    }

    fun openAppIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
