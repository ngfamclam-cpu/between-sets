package com.betweensets.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerLogicTest {
    private val t0 = 1_000_000L

    @Test fun startRunsForTheChosenInterval() {
        val s = TimerLogic.start(TimerState(), 30_000, t0)
        assertEquals(Status.RUNNING, s.status)
        assertEquals(30_000, s.leftMs(t0))
        assertEquals(10_000, s.leftMs(t0 + 20_000))
    }

    @Test fun pauseKeepsTimeLeftAndResumeContinues() {
        val running = TimerLogic.start(TimerState(), 60_000, t0)
        val paused = TimerLogic.toggle(running, t0 + 15_000)
        assertEquals(Status.PAUSED, paused.status)
        assertEquals(45_000, paused.leftMs(t0 + 99_000))
        val resumed = TimerLogic.toggle(paused, t0 + 100_000)
        assertEquals(t0 + 145_000, resumed.endAt)
    }

    @Test fun plusFifteenMovesTheEnd() {
        val s = TimerLogic.addTime(TimerLogic.start(TimerState(), 30_000, t0), 15_000, t0 + 10_000)
        assertEquals(35_000, s.leftMs(t0 + 10_000))
        assertEquals(35_000, s.durationMs)
    }

    @Test fun finishCountsASetAndStops() {
        val s = TimerLogic.finish(TimerLogic.start(TimerState(), 90_000, t0), t0 + 90_000)!!
        assertEquals(Status.DONE, s.status)
        assertEquals(1, s.sets)
    }

    @Test fun finishWithAutoStartsTheNextRest() {
        val start = TimerLogic.start(TimerState(auto = true, sets = 2), 120_000, t0)
        val s = TimerLogic.finish(start, t0 + 120_200)!!
        assertEquals(Status.RUNNING, s.status)
        assertEquals(3, s.sets)
        assertEquals(t0 + 240_200, s.endAt)
    }

    @Test fun staleAlarmIsIgnored() {
        val s = TimerLogic.start(TimerState(), 90_000, t0)
        assertNull(TimerLogic.finish(s, t0 + 30_000))
        assertNull(TimerLogic.finish(TimerLogic.stop(s), t0 + 90_000))
    }

    @Test fun clockFormat() {
        assertEquals("1:30", formatClock(90_000))
        assertEquals("0:01", formatClock(200))
        assertEquals("0:00", formatClock(0))
        assertEquals("3:00", formatClock(180_000))
    }
}
