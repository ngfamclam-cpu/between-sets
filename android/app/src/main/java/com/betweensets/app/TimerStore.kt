package com.betweensets.app

import android.content.Context

enum class NotifMode { APP_OPEN, ALWAYS, OFF }

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

    /** Settings: when the control panel shows in the notification shade, and whether also on the lock screen. */
    fun notifMode(ctx: Context): NotifMode = prefs(ctx).run {
        getString("notifMode", null)?.let { runCatching { NotifMode.valueOf(it) }.getOrNull() }
            // Before this setting existed there was only an on/off switch.
            ?: if (contains("notifControls") && !getBoolean("notifControls", true)) NotifMode.OFF else NotifMode.APP_OPEN
    }

    fun setNotifMode(ctx: Context, mode: NotifMode) {
        prefs(ctx).edit().putString("notifMode", mode.name).commit()
    }

    fun lockScreenControls(ctx: Context) = prefs(ctx).getBoolean("lockControls", true)

    /** True from opening the app until it's swiped out of recent apps (see ControlsService). */
    fun appOpen(ctx: Context) = prefs(ctx).getBoolean("appOpen", false)

    fun setAppOpen(ctx: Context, open: Boolean) {
        prefs(ctx).edit().putBoolean("appOpen", open).commit()
    }

    fun setLockScreenControls(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("lockControls", on).commit()
    }

    fun sound(ctx: Context): SoundChoice = prefs(ctx).run {
        val key = getString("soundKey", null) ?: return SoundChoice.DEFAULT
        SoundChoice(key, getString("soundLabel", null) ?: "Custom sound")
    }

    fun setSound(ctx: Context, choice: SoundChoice) {
        prefs(ctx).edit().putString("soundKey", choice.key).putString("soundLabel", choice.label).commit()
    }

    /** Custom timers the user saved, in seconds. */
    fun customs(ctx: Context): List<Int> =
        (prefs(ctx).getString("customs", "") ?: "").split(',').mapNotNull { it.toIntOrNull() }

    fun setCustoms(ctx: Context, secs: List<Int>) {
        prefs(ctx).edit().putString("customs", secs.joinToString(",")).commit()
    }
}
