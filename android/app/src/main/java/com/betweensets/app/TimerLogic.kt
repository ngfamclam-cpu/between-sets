package com.betweensets.app

import kotlin.math.ceil

enum class Status { IDLE, RUNNING, PAUSED, DONE }

/** The whole timer, small enough to live in SharedPreferences. Times are wall-clock ms. */
data class TimerState(
    val status: Status = Status.IDLE,
    val durationMs: Long = 90_000,
    val endAt: Long = 0,
    val pausedLeftMs: Long = 0,
    val sets: Int = 0,
    val auto: Boolean = false,
) {
    fun leftMs(now: Long): Long = when (status) {
        Status.RUNNING -> (endAt - now).coerceAtLeast(0)
        Status.PAUSED -> pausedLeftMs
        Status.IDLE -> durationMs
        Status.DONE -> 0
    }
}

/** Pure state changes, so they can be unit tested without a phone. */
object TimerLogic {
    const val PLUS_MS = 15_000L

    /** An alarm that fires this early is treated as stale (the timer was changed after it was set). */
    private const val EARLY_SLACK_MS = 1_500L

    fun start(s: TimerState, durationMs: Long, now: Long) =
        s.copy(status = Status.RUNNING, durationMs = durationMs, endAt = now + durationMs, pausedLeftMs = 0)

    fun pause(s: TimerState, now: Long) =
        if (s.status != Status.RUNNING) s
        else s.copy(status = Status.PAUSED, pausedLeftMs = (s.endAt - now).coerceAtLeast(0))

    /** Pause when running, otherwise resume or (re)start the current interval. */
    fun toggle(s: TimerState, now: Long) = when (s.status) {
        Status.RUNNING -> pause(s, now)
        Status.PAUSED -> s.copy(status = Status.RUNNING, endAt = now + s.pausedLeftMs)
        Status.IDLE, Status.DONE -> start(s, s.durationMs, now)
    }

    fun addTime(s: TimerState, ms: Long, now: Long) = when (s.status) {
        Status.RUNNING -> {
            val end = s.endAt + ms
            s.copy(endAt = end, durationMs = maxOf(s.durationMs, end - now))
        }
        Status.PAUSED -> {
            val left = s.pausedLeftMs + ms
            s.copy(pausedLeftMs = left, durationMs = maxOf(s.durationMs, left))
        }
        Status.IDLE -> s.copy(durationMs = s.durationMs + ms)
        Status.DONE -> s
    }

    fun stop(s: TimerState) = s.copy(status = Status.IDLE, pausedLeftMs = 0)

    /** The alarm fired. Returns null when it is stale and should be ignored. */
    fun finish(s: TimerState, now: Long): TimerState? {
        if (s.status != Status.RUNNING || now < s.endAt - EARLY_SLACK_MS) return null
        val sets = s.sets + 1
        return if (s.auto) s.copy(sets = sets, endAt = now + s.durationMs)
        else s.copy(status = Status.DONE, sets = sets)
    }
}

/** 90_000 -> "1:30". Rounds up, so a timer shows 0:00 only when it is really over. */
fun formatClock(ms: Long): String {
    val total = ceil(ms.coerceAtLeast(0) / 1000.0).toLong()
    return "%d:%02d".format(total / 60, total % 60)
}

/** The five rest intervals, coloured like Olympic bumper plates. */
val PRESETS = listOf(30, 60, 90, 120, 180)
