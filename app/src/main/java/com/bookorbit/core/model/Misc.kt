package com.bookorbit.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

@Serializable
data class AppInfo(
    val version: String,
    val updateAvailable: Boolean? = null,
    val latestVersion: String? = null,
    val bookDockPath: String? = null,
)

/** Catalog typeahead rows (`GET /metadata/{authors,genres,tags,languages}`) return `{ name }`. */
@Serializable
data class NamedResult(val name: String? = null)

/** The `reading-streak` dashboard widget. */
@Serializable
data class ReadingStreakWidget(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSevenDays: List<Boolean> = emptyList(),
)

/** The `reading-goal` dashboard widget. [goalBooks] is null until the user sets a yearly goal. */
@Serializable
data class ReadingGoalWidget(
    val goalBooks: Int? = null,
    val completedBooks: Int = 0,
    val year: Int = 0,
)

/**
 * `POST /dashboard/widgets/batch` body. The app asks for its widgets here rather than at
 * `GET /dashboard/widgets/{type}`: server 3.7+ lets only the official BookOrbit app use those (App
 * Attest), while the batch route, which the web dashboard uses, is open to any signed-in client.
 */
@Serializable
data class WidgetBatchRequest(val widgets: List<String>) {
    companion object {
        const val READING_STREAK = "reading-streak"
        const val READING_GOAL = "reading-goal"
        val READING = WidgetBatchRequest(listOf(READING_STREAK, READING_GOAL))
    }
}

/** One widget of a [WidgetBatchResponse]; [data] is null, and [failed] true, when that widget errored. */
@Serializable
data class WidgetBatchItem(
    val type: String,
    val data: JsonElement? = null,
    val failed: Boolean = false,
)

@Serializable
data class WidgetBatchResponse(val items: List<WidgetBatchItem> = emptyList())

/** The streak and yearly goal, each null when the server couldn't provide it. */
data class ReadingWidgets(
    val streak: ReadingStreakWidget? = null,
    val goal: ReadingGoalWidget? = null,
) {
    companion object {
        fun from(json: Json, response: WidgetBatchResponse): ReadingWidgets {
            fun data(type: String): JsonElement? =
                response.items.firstOrNull { it.type == type && !it.failed }?.data?.takeUnless { it is JsonNull }
            return ReadingWidgets(
                streak = data(WidgetBatchRequest.READING_STREAK)?.let { runCatching { json.decodeFromJsonElement(ReadingStreakWidget.serializer(), it) }.getOrNull() },
                goal = data(WidgetBatchRequest.READING_GOAL)?.let { runCatching { json.decodeFromJsonElement(ReadingGoalWidget.serializer(), it) }.getOrNull() },
            )
        }
    }
}

/** Scroller types accepted by `GET /dashboard/scrollers/:type`. */
object ScrollerType {
    const val RECENTLY_ADDED = "recently-added"
    const val CONTINUE_READING = "continue-reading"
    const val CONTINUE_LISTENING = "continue-listening"
    const val RANDOM = "random"
    const val SMART_SCOPE = "smart-scope"
}

/**
 * `GET /dashboard/scrollers/{type}` body: a bare `BookCard[]` before server 3.0, `{ books, total }`
 * since. Accepts both.
 */
object ScrollerBooks {
    fun decode(json: Json, body: JsonElement): List<BookCard> {
        val books = (body as? JsonObject)?.get("books") ?: body
        return json.decodeFromJsonElement(ListSerializer(BookCard.serializer()), books)
    }
}

/** `GET /user-statistics/summary`. */
@Serializable
data class UserStatsSummary(
    val trackedBooks: Int = 0,
    val startedBooks: Int = 0,
    val inProgressBooks: Int = 0,
    val completedBooks: Int = 0,
    val meanProgressPercent: Double = 0.0,
)

/** One day of `GET /user-statistics/daily-reading` (or reading-heatmap). [day] is `yyyy-mm-dd`. */
@Serializable
data class UserDailyReading(
    val day: String,
    val readingSeconds: Long = 0,
    val progressDelta: Double = 0.0,
    val eventsCount: Int = 0,
)

@Serializable
data class SourceSlice(val bucket: String, val readingSeconds: Long = 0)

/** `GET /user-statistics/reading-source-distribution`: where your reading time came from. */
@Serializable
data class SourceDistribution(
    val totalSeconds: Long = 0,
    val slices: List<SourceSlice> = emptyList(),
)

/** An audiobook bookmark (`/audiobooks/{bookId}/bookmarks`). [positionMs] is the position in the whole book. */
@Serializable
data class AudiobookBookmark(
    val id: String,
    val bookId: Int,
    val positionMs: Long,
    val chapterId: String? = null,
    val title: String = "",
    val note: String? = null,
)

@Serializable
data class CreateAudiobookBookmark(
    val clientId: String,
    val positionMs: Long,
    val title: String,
)
