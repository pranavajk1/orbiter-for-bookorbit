package com.bookorbit.feature.reader

/**
 * How far in from each side of the page a tap turns it, as a fraction of the width. Kept to a
 * strip along the edges so a tap meant to bring up the bars, or one that lands slightly off while
 * holding the phone, doesn't turn the page; everything between the strips toggles the bars.
 */
const val PAGE_TURN_EDGE = 0.2f
