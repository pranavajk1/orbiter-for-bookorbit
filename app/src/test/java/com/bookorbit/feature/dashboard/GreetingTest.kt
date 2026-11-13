package com.bookorbit.feature.dashboard

import com.bookorbit.R
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun `greeting follows the hour`() {
        assertEquals(R.string.home_good_evening, greetingFor(4))
        assertEquals(R.string.home_good_morning, greetingFor(5))
        assertEquals(R.string.home_good_morning, greetingFor(11))
        assertEquals(R.string.home_good_afternoon, greetingFor(12))
        assertEquals(R.string.home_good_afternoon, greetingFor(17))
        assertEquals(R.string.home_good_evening, greetingFor(18))
        assertEquals(R.string.home_good_evening, greetingFor(23))
        assertEquals(R.string.home_good_evening, greetingFor(0))
    }

    @Test
    fun `next refresh is the top of the next hour`() {
        assertEquals(3_600_000L, millisToNextHour(LocalTime.of(14, 0)))
        assertEquals(30 * 60_000L, millisToNextHour(LocalTime.of(14, 30)))
        assertEquals(1_500L, millisToNextHour(LocalTime.of(11, 59, 58, 500_000_000)))
    }

    @Test
    fun `refresh wraps past midnight and never spins`() {
        assertEquals(60_000L, millisToNextHour(LocalTime.of(23, 59)))
        assertEquals(1_000L, millisToNextHour(LocalTime.of(23, 59, 59, 999_000_000)))
    }
}
