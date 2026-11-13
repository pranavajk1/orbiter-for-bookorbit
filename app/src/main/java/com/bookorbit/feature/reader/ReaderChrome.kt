package com.bookorbit.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookorbit.R
import com.bookorbit.ui.theme.Motion
import kotlin.math.roundToInt

/** The page's own colours, which the reader's bars wear instead of a fixed dark strip. */
data class ReaderChromeColors(val background: Color, val content: Color) {
    val muted: Color get() = content.copy(alpha = 0.62f)
    val hairline: Color get() = content.copy(alpha = 0.12f)

    companion object {
        fun of(settings: ReaderSettings) = ReaderChromeColors(
            background = themeBackgroundColor(settings.themeName, settings.isDark),
            content = themeForegroundColor(settings.themeName, settings.isDark),
        )
    }
}

/** The bar along the top: back, the chapter over the book's title, then search, highlights, contents, settings. */
@Composable
fun BoxScope.ReaderTopBar(
    visible: Boolean,
    title: String?,
    chapter: String?,
    colors: ReaderChromeColors,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onHighlights: () -> Unit,
    onContents: () -> Unit,
    onSettings: () -> Unit,
    onHeight: (Int) -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter()) + slideInVertically(Motion.enter()) { -it / 3 },
        exit = fadeOut(Motion.exit()) + slideOutVertically(Motion.exit()) { -it / 3 },
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        Column(Modifier.fillMaxWidth().background(colors.background).onSizeChanged { onHeight(it.height) }) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.content)
                }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        chapter ?: title.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (chapter != null && title != null && chapter != title) {
                        Text(title, style = MaterialTheme.typography.bodySmall, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                ChromeIcon(Icons.Outlined.Search, stringResource(R.string.search_in_book), colors, onSearch)
                ChromeIcon(Icons.Outlined.FormatQuote, stringResource(R.string.highlights), colors, onHighlights)
                ChromeIcon(Icons.AutoMirrored.Outlined.List, stringResource(R.string.contents), colors, onContents)
                ChromeIcon(Icons.Outlined.TextFields, stringResource(R.string.settings), colors, onSettings)
            }
            HorizontalDivider(color = colors.hairline)
        }
    }
}

/**
 * The bar along the bottom: the footer line (tap to change what it shows), and a scrubber with a
 * tick where each chapter starts, between previous- and next-chapter buttons. After a jump along
 * the scrubber, a button above the bar goes back to where the reader was, and a dot on the
 * scrubber marks that spot.
 */
@Composable
fun BoxScope.ReaderBottomBar(
    visible: Boolean,
    fraction: Double,
    sectionFractions: List<Double>,
    footer: Pair<String, String>,
    colors: ReaderChromeColors,
    returnPoint: ReturnPoint?,
    onSeek: (Double) -> Unit,
    onReturn: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onCycleFooter: () -> Unit,
    onHeight: (Int) -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter()) + slideInVertically(Motion.enter()) { it / 3 },
        exit = fadeOut(Motion.exit()) + slideOutVertically(Motion.exit()) { it / 3 },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Kept while it fades out, so the label doesn't vanish before the button does.
            var shown by remember { mutableStateOf(returnPoint) }
            if (returnPoint != null) shown = returnPoint
            AnimatedVisibility(
                visible = returnPoint != null,
                enter = fadeIn(Motion.enter()),
                exit = fadeOut(Motion.exit()),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                shown?.let { ReturnButton(it, colors, onReturn, Modifier.padding(bottom = 10.dp)) }
            }
            Column(Modifier.fillMaxWidth().background(colors.background).onSizeChanged { onHeight(it.height) }) {
                HorizontalDivider(color = colors.hairline)
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp)) {
                    FooterLine(footer, colors, onCycleFooter, Modifier.padding(horizontal = 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChromeIcon(Icons.Outlined.SkipPrevious, stringResource(R.string.reader_previous_chapter), colors, onPreviousChapter)
                        Scrubber(fraction, sectionFractions, returnPoint?.target?.fraction, colors, onSeek, Modifier.weight(1f))
                        ChromeIcon(Icons.Outlined.SkipNext, stringResource(R.string.reader_next_chapter), colors, onNextChapter)
                    }
                }
            }
        }
    }
}

/** "Back to page 42" (or "Forward to page 310", after going back), in the page's colours. */
@Composable
private fun ReturnButton(point: ReturnPoint, colors: ReaderChromeColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spot = point.target
    val label = when {
        spot.pageTotal > 0 && point.back -> stringResource(R.string.reader_back_to_page, spot.page + 1)
        spot.pageTotal > 0 -> stringResource(R.string.reader_forward_to_page, spot.page + 1)
        point.back -> stringResource(R.string.reader_back_to_percent, (spot.fraction * 100).roundToInt())
        else -> stringResource(R.string.reader_forward_to_percent, (spot.fraction * 100).roundToInt())
    }
    Row(
        modifier
            .clip(CircleShape)
            .background(colors.background)
            .border(1.dp, colors.content.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (point.back) Icons.AutoMirrored.Outlined.Undo else Icons.AutoMirrored.Outlined.Redo,
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.content)
    }
}

/** The footer on its own, quiet, while the bars are hidden, as a printed book's running foot. */
@Composable
fun BoxScope.ReaderRunningFoot(visible: Boolean, footer: Pair<String, String>, colors: ReaderChromeColors, onCycle: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter()),
        exit = fadeOut(Motion.exit()),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        FooterLine(
            footer,
            colors.copy(content = colors.content.copy(alpha = 0.55f)),
            onCycle,
            Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FooterLine(footer: Pair<String, String>, colors: ReaderChromeColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.reader_footer_mode)
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(footer.first, style = MaterialTheme.typography.labelMedium, color = colors.muted, maxLines = 1, modifier = Modifier.weight(1f))
        Text(footer.second, style = MaterialTheme.typography.labelMedium, color = colors.muted, maxLines = 1)
    }
}

/**
 * A slider over the whole book: a thin track in the page's text colour with a mark where each
 * chapter starts, and a round thumb. The thumb follows a drag; the book jumps when it's let go.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Scrubber(
    fraction: Double,
    sectionFractions: List<Double>,
    returnFraction: Double?,
    colors: ReaderChromeColors,
    onSeek: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val description = stringResource(R.string.reader_position)
    Box(modifier.height(40.dp), contentAlignment = Alignment.Center) {
        Slider(
            value = dragging ?: fraction.toFloat(),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek(it.toDouble()) }
                dragging = null
            },
            thumb = {
                Box(Modifier.size(16.dp).background(colors.content, CircleShape))
            },
            track = { state ->
                val value = state.value.coerceIn(0f, 1f)
                Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                    val y = size.height / 2
                    val stroke = 3.dp.toPx()
                    drawLine(colors.content.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                    drawLine(colors.content.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width * value, y), stroke, StrokeCap.Round)
                    // Chapter starts.
                    sectionFractions.drop(1).forEach { f ->
                        val x = f.toFloat().coerceIn(0f, 1f) * size.width
                        drawLine(colors.content.copy(alpha = 0.45f), Offset(x, y - 4.dp.toPx()), Offset(x, y + 4.dp.toPx()), 1.dp.toPx())
                    }
                    // Where the reader was before the jump, as the return button's target.
                    returnFraction?.let { f ->
                        val x = f.toFloat().coerceIn(0f, 1f) * size.width
                        drawCircle(colors.background, radius = 5.dp.toPx(), center = Offset(x, y))
                        drawCircle(colors.content, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).semantics { contentDescription = description },
        )
        dragging?.let {
            Text(
                "${(it * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = colors.content,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun ChromeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, colors: ReaderChromeColors, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = label, tint = colors.content) }
}

/** What the footer shows for [mode]: left and right text, as the web reader's footer modes. */
@Composable
fun footerText(mode: Int, position: ReadingPosition, percentage: Int, minutesRead: Long): Pair<String, String> {
    val percent = "$percentage%"
    return when (mode) {
        1 -> stringResource(R.string.reader_footer_reading, formatMinutes(minutesRead.toDouble())) to
            (if (position.minutesLeftInBook > 0) stringResource(R.string.reader_footer_left, formatMinutes(position.minutesLeftInBook)) else percent)
        2 -> (if (position.sectionTotal > 0) stringResource(R.string.reader_footer_chapter, position.section + 1, position.sectionTotal) else "") to
            (if (position.minutesLeftInSection > 0) stringResource(R.string.reader_footer_left_in_chapter, formatMinutes(position.minutesLeftInSection)) else percent)
        else -> (if (position.locationTotal > 0) stringResource(R.string.reader_footer_page, position.location + 1, position.locationTotal) else "") to percent
    }
}

@Composable
private fun formatMinutes(minutes: Double): String {
    val total = minutes.roundToInt()
    return when {
        total < 1 -> stringResource(R.string.reader_footer_less_than_minute)
        total < 60 -> "${total}m"
        else -> "${total / 60}h ${total % 60}m"
    }
}
