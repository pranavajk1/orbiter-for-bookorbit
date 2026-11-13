package com.bookorbit.feature.reader

import kotlin.math.abs

/** A place in the book: [cfi] to go back to, and where it sits ([fraction], [page] of [pageTotal]) to measure and label it. */
data class BookSpot(val cfi: String, val fraction: Double, val page: Int, val pageTotal: Int)

/**
 * The way back after a jump along the scrubber, as Kindle keeps one: a "Back to page N" button over
 * the scrubber and a dot on it where the reader was. [target] is where the button goes; [from] is
 * where the jump landed. Taking it swaps the two, so a mistaken "back" can be undone as well
 * ([back] is false then, and the button reads "Forward to page N").
 */
data class ReturnPoint(val target: BookSpot, val from: BookSpot, val back: Boolean)

/** The rules for when a [ReturnPoint] is offered, carried over, swapped and dropped. */
object JumpBack {
    /** A jump of this many pages or fewer is just paging; it isn't worth a way back. */
    const val THRESHOLD_PAGES = 3

    /** The same distance, for a book whose page count isn't known yet, as a fraction of the book. */
    private const val THRESHOLD_FRACTION = 0.01

    /** True when [a] and [b] are within [THRESHOLD_PAGES] of each other. */
    fun near(a: BookSpot, b: BookSpot): Boolean =
        if (a.pageTotal > 0 && a.pageTotal == b.pageTotal) {
            abs(a.page - b.page) <= THRESHOLD_PAGES
        } else {
            abs(a.fraction - b.fraction) <= THRESHOLD_FRACTION
        }

    /**
     * Where a new jump from [current] should lead back to. Scrubbing again while still looking
     * around where the last jump landed keeps the original spot, so back goes to where the reader
     * was reading rather than to the last place they glanced at.
     */
    fun origin(current: BookSpot, existing: ReturnPoint?): BookSpot =
        if (existing != null && existing.back && near(current, existing.from)) existing.target else current

    /** The way back once a jump from [origin] lands at [landed], or null if it only moved a page or three. */
    fun afterJump(origin: BookSpot, landed: BookSpot): ReturnPoint? =
        if (near(origin, landed)) null else ReturnPoint(target = origin, from = landed, back = true)

    /** After taking [taken] and landing at [landed]: the way to where it was taken from. */
    fun afterReturn(taken: ReturnPoint, landed: BookSpot): ReturnPoint =
        ReturnPoint(target = taken.from, from = landed, back = !taken.back)

    /**
     * Whether [point] still stands once the reader has paged to [current]. It goes once they reach
     * its target on their own. A way back stays until then (the point is not to lose their place);
     * a way forward, offered after going back, goes once they read on from where they went back to.
     */
    fun onMove(point: ReturnPoint, current: BookSpot): ReturnPoint? = when {
        near(current, point.target) -> null
        !point.back && !near(current, point.from) -> null
        else -> point
    }

    /**
     * The return point once the page settles at [landed]: after a scrubber jump from [jumpedFrom],
     * after taking [taken], or otherwise after ordinary paging with [current] standing.
     */
    fun settle(current: ReturnPoint?, landed: BookSpot, jumpedFrom: BookSpot?, taken: ReturnPoint?): ReturnPoint? = when {
        // A small nudge of the scrubber doesn't throw away a way back that's already there.
        jumpedFrom != null -> afterJump(jumpedFrom, landed) ?: current?.let { onMove(it, landed) }
        // A return that didn't get there (the page can fail to open a spot by its CFI) changes nothing.
        taken != null -> if (near(landed, taken.target)) afterReturn(taken, landed) else current
        else -> current?.let { onMove(it, landed) }
    }
}
