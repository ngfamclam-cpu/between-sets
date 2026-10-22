package com.betweensets.app

import android.content.Context

object TimerStore {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("timer", Context.MODE_PRIVATE)

    fun load(ctx: Context): TimerState = prefs(ctx).run {
        TimerState(
            status = runCatching { Status.valueOf(getString("status", null) ?: "IDLE") }.getOrDefault(Status.IDLE),
            durationMs = getLong("duration", 90_000),
            endAt = getLong("endAt", 0),
            pausedLeftMs = getLong("pausedLeft", 0),
            sets = getInt("sets", 0),
            auto = getBoolean("auto", false),
        )
    }

    // commit(), not apply(): this often runs in a receiver whose process may be killed right after.
    fun save(ctx: Context, s: TimerState) {
        prefs(ctx).edit()
            .putString("status", s.status.name)
            .putLong("duration", s.durationMs)
            .putLong("endAt", s.endAt)
            .putLong("pausedLeft", s.pausedLeftMs)
            .putInt("sets", s.sets)
            .putBoolean("auto", s.auto)
            .commit()
    }

    /** Custom timers the user saved, in seconds. */
    fun customs(ctx: Context): List<Int> =
        (prefs(ctx).getString("customs", "") ?: "").split(',').mapNotNull { it.toIntOrNull() }

    fun setCustoms(ctx: Context, secs: List<Int>) {
        prefs(ctx).edit().putString("customs", secs.joinToString(",")).commit()
    }
}
