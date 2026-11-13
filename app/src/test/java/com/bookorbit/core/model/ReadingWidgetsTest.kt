package com.bookorbit.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingWidgetsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun from(body: String) = ReadingWidgets.from(json, json.decodeFromString<WidgetBatchResponse>(body))

    @Test
    fun `reads both widgets out of a batch`() {
        // As server 3.8.0 answers POST /dashboard/widgets/batch for these two widgets.
        val widgets = from(
            """{"items":[
              {"type":"reading-streak","data":{"currentStreak":2,"longestStreak":3,"lastSevenDays":[false,false,false,false,false,true,true]},"failed":false},
              {"type":"reading-goal","data":{"goalBooks":null,"completedBooks":2,"year":2026},"failed":false}
            ]}""",
        )
        assertEquals(ReadingStreakWidget(2, 3, listOf(false, false, false, false, false, true, true)), widgets.streak)
        assertEquals(ReadingGoalWidget(goalBooks = null, completedBooks = 2, year = 2026), widgets.goal)
    }

    @Test
    fun `a failed widget is null and leaves the other`() {
        val widgets = from(
            """{"items":[
              {"type":"reading-streak","data":null,"failed":true},
              {"type":"reading-goal","data":{"goalBooks":24,"completedBooks":17,"year":2026},"failed":false}
            ]}""",
        )
        assertNull(widgets.streak)
        assertEquals(24, widgets.goal?.goalBooks)
    }

    @Test
    fun `a missing or malformed widget is null`() {
        val widgets = from("""{"items":[{"type":"reading-goal","data":"nonsense","failed":false}]}""")
        assertNull(widgets.streak)
        assertNull(widgets.goal)
    }

    @Test
    fun `asks for the streak and the goal`() {
        assertEquals("""{"widgets":["reading-streak","reading-goal"]}""", json.encodeToString(WidgetBatchRequest.READING))
    }
}
