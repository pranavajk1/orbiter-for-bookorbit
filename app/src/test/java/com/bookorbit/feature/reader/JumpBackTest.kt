package com.bookorbit.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JumpBackTest {
    private fun page(n: Int, total: Int = 400) = BookSpot("cfi-$n", n.toDouble() / total, n, total)

    @Test
    fun `a jump of three pages or fewer offers no way back`() {
        assertNull(JumpBack.afterJump(page(40), page(43)))
        assertNull(JumpBack.afterJump(page(40), page(37)))
    }

    @Test
    fun `a longer jump offers the way back to where the reader was`() {
        val point = JumpBack.afterJump(page(40), page(44))!!
        assertEquals(page(40), point.target)
        assertEquals(page(44), point.from)
        assertTrue(point.back)
    }

    @Test
    fun `without a page count the threshold is one percent of the book`() {
        val a = BookSpot("a", 0.400, 0, 0)
        assertNull(JumpBack.afterJump(a, BookSpot("b", 0.409, 0, 0)))
        assertTrue(JumpBack.afterJump(a, BookSpot("b", 0.42, 0, 0))!!.back)
    }

    @Test
    fun `scrubbing again while looking around keeps the original spot`() {
        val point = JumpBack.afterJump(page(40), page(200))!!
        assertEquals(page(40), JumpBack.origin(page(202), point))
        val again = JumpBack.settle(point, page(300), JumpBack.origin(page(202), point), null)!!
        assertEquals(page(40), again.target)
        assertEquals(page(300), again.from)
    }

    @Test
    fun `scrubbing after reading on from the landing starts from the new spot`() {
        val point = JumpBack.afterJump(page(40), page(200))!!
        assertEquals(page(220), JumpBack.origin(page(220), point))
    }

    @Test
    fun `a small nudge of the scrubber keeps the way back`() {
        val point = JumpBack.afterJump(page(40), page(200))!!
        // Read on well past the landing, then nudge the scrubber a page.
        val nudged = JumpBack.settle(point, page(251), JumpBack.origin(page(250), point), null)
        assertEquals(point, nudged)
    }

    @Test
    fun `the way back stays while paging, and goes once the reader gets there`() {
        val point = JumpBack.afterJump(page(40), page(200))!!
        assertEquals(point, JumpBack.settle(point, page(230), null, null))
        assertNull(JumpBack.settle(point, page(42), null, null))
    }

    @Test
    fun `taking it offers the way forward again`() {
        val point = JumpBack.afterJump(page(40), page(200))!!
        val forward = JumpBack.settle(point, page(40), null, point)!!
        assertFalse(forward.back)
        assertEquals(page(200), forward.target)
        assertEquals(page(40), forward.from)
        // Going forward again offers back once more.
        val back = JumpBack.settle(forward, page(200), null, forward)!!
        assertTrue(back.back)
        assertEquals(page(40), back.target)
    }

    @Test
    fun `the way forward goes once the reader reads on from where they went back to`() {
        val forward = JumpBack.afterReturn(JumpBack.afterJump(page(40), page(200))!!, page(40))
        assertEquals(forward, JumpBack.settle(forward, page(42), null, null))
        assertNull(JumpBack.settle(forward, page(44), null, null))
    }

    @Test
    fun `a return that doesn't get there leaves the point as it was`() {
        val point = JumpBack.afterJump(page(1), page(16))!!
        assertEquals(point, JumpBack.settle(point, page(16), null, point))
    }

    @Test
    fun `pages from a different layout are compared by fraction`() {
        // Font size changed: the page count moved, so page numbers can't be compared directly.
        assertTrue(JumpBack.near(BookSpot("a", 0.5, 200, 400), BookSpot("b", 0.505, 300, 600)))
        assertFalse(JumpBack.near(BookSpot("a", 0.5, 200, 400), BookSpot("b", 0.55, 330, 600)))
    }
}
