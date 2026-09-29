package com.erosketarakoa.app.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BargainSchedulerTest {

    private fun at(hour: Int, minute: Int): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun targetLaterToday_delayIsWithinTheSameDay() {
        val now = at(8, 0)
        val delay = BargainScheduler.initialDelayMillis(minuteOfDay = 9 * 60, now = now)
        assertEquals(TimeUnit.HOURS.toMillis(1), delay)
    }

    @Test
    fun targetAlreadyPassed_rollsToTomorrow() {
        val now = at(10, 0)
        val delay = BargainScheduler.initialDelayMillis(minuteOfDay = 9 * 60, now = now)
        // 23h until 09:00 next day.
        assertEquals(TimeUnit.HOURS.toMillis(23), delay)
    }

    @Test
    fun targetEqualsNow_rollsToTomorrow() {
        val now = at(9, 0)
        val delay = BargainScheduler.initialDelayMillis(minuteOfDay = 9 * 60, now = now)
        assertEquals(TimeUnit.HOURS.toMillis(24), delay)
    }
}
