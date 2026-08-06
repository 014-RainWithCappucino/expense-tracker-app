package com.nijika21.yourmoney.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme
import kotlinx.coroutines.delay

/**
 * The amount keypad for screen 02.
 *
 * A bespoke keypad rather than a numeric `TextField`, for reasons that came out of
 * the concept: the keys are large enough to hit one-handed while standing at a
 * warung, the system keyboard cannot show a "000" key, and no IME means no
 * keyboard animation shifting the layout on every entry.
 *
 * Every key edits at the caret in `AmountField`, not at the end of the number —
 * this used to be an append-only pad, and fixing a typo cost the whole amount.
 *
 * It sits pinned at the bottom of screen 02, directly under the amount it edits.
 * The screen hides it while the system IME is up, because two keyboards fighting
 * for the same strip of screen is how the first version became unusable.
 */
@Composable
fun Keypad(
    onDigit: (Char) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = YourMoneyTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.gapS),
    ) {
        for (row in listOf("123", "456", "789")) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapS)) {
                for (digit in row) {
                    Key(digit.toString(), Modifier.weight(1f)) { onDigit(digit) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapS)) {
            Key("000", Modifier.weight(1f), onClick = onTripleZero)
            Key("0", Modifier.weight(1f)) { onDigit('0') }
            BackspaceKey(Modifier.weight(1f), onClick = onBackspace)
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = YourMoneyTheme.colors

    Text(
        text = label,
        style = YourMoneyTheme.typography.titleMoney,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .height(KEY_HEIGHT)
            .pressable(
                shape = YourMoneyTheme.shapes.cardSmall,
                fill = colors.card,
                onClick = onClick,
            )
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/**
 * The delete key. Same fill and same height as every digit — it is part of the
 * grid, and giving it its own colour made the bottom row look broken.
 *
 * **Hold to keep deleting.** Fixing a six-digit typo one deliberate tap at a time
 * is the kind of small friction that makes a screen tiring, so holding the key
 * repeats it. It repeats *one digit at a time* and never turns into "clear the
 * field": the repeat is slow enough to stop on the digit you meant, and lifting
 * the thumb stops it immediately. A field that empties in one gesture would just
 * trade one annoyance for a worse one.
 *
 * The delete is driven by the press, not by `onClick`, so a hold does not fire an
 * extra deletion when the thumb finally lifts. A quick tap still deletes exactly
 * once — the first repeat is the tap.
 *
 * The glyph is drawn rather than imported: the standard backspace icon lives in
 * `material-icons-extended`, and pulling a whole icon library into the build for
 * one arrow is not a trade worth making.
 *
 * **Outlined and rounded, never solid.** The first attempt was a hard-cornered
 * tag with butt-ended strokes and it read as stiff — a wireframe rather than an
 * icon. Every corner including the point is a quadratic, the strokes are round
 * capped, and the whole thing sits in `textSecondary` rather than full white: it
 * is a delete key, not the loudest thing on the pad.
 */
@Composable
private fun BackspaceKey(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = YourMoneyTheme.colors

    val interactionSource = remember { MutableInteractionSource() }
    val held by interactionSource.collectIsPressedAsState()
    // The lambda can change identity between recompositions; without this the
    // running hold would keep calling the one captured when the press started.
    val deleteOnce by rememberUpdatedState(onClick)

    LaunchedEffect(held) {
        if (!held) return@LaunchedEffect
        deleteOnce()
        delay(HOLD_DELAY_MS)
        while (true) {
            deleteOnce()
            delay(HOLD_REPEAT_MS)
        }
    }

    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .pressable(
                shape = YourMoneyTheme.shapes.cardSmall,
                fill = colors.card,
                interactionSource = interactionSource,
                // Deliberately empty: the press above owns the deleting.
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 25.dp, height = 17.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 1.6.dp.toPx()
            // How far the point reaches in from the left. Shorter than half the
            // height, so the two diagonals stay long enough to read as edges.
            val nose = h * 0.52f
            val ink = colors.textSecondary

            // The tag: a rectangle with a point on the left, aimed at what it
            // removes. Softened at every corner, but only just — a larger radius
            // eats the diagonals and the whole glyph rounds off into a blob.
            val outline = roundedPolygon(
                points = listOf(
                    Offset(0f, h / 2f), // the point
                    Offset(nose, 0f),
                    Offset(w, 0f),
                    Offset(w, h),
                    Offset(nose, h),
                ),
                radius = h * 0.15f,
            )
            drawPath(
                outline,
                ink,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // The cross, centred in the rectangular part rather than in the whole
            // glyph — the point is a direction marker, not somewhere to put things.
            val boxLeft = nose
            val cx = (boxLeft + w) / 2f
            val arm = h * 0.21f
            drawLine(ink, Offset(cx - arm, h / 2f - arm), Offset(cx + arm, h / 2f + arm), stroke, StrokeCap.Round)
            drawLine(ink, Offset(cx + arm, h / 2f - arm), Offset(cx - arm, h / 2f + arm), stroke, StrokeCap.Round)
        }
    }
}

/**
 * A closed polygon whose every corner is rounded off with a quadratic.
 *
 * `StrokeJoin.Round` alone only rounds by half the stroke width, which at this
 * size is a fraction of a pixel and still reads as a sharp corner. Actually
 * curving the path is what makes the glyph look drawn rather than plotted.
 *
 * Each corner is pulled back along both of its edges by [radius] — clamped to
 * half the shorter edge so adjacent corners can never overlap and invert the
 * shape — and the original vertex becomes the control point.
 */
private fun roundedPolygon(points: List<Offset>, radius: Float): Path {
    val path = Path()
    val n = points.size

    for (i in 0 until n) {
        val corner = points[i]
        val prev = points[(i - 1 + n) % n]
        val next = points[(i + 1) % n]

        val toPrev = prev - corner
        val toNext = next - corner
        val inset = { edge: Offset ->
            val length = edge.getDistance()
            // A zero-length edge would divide by zero; treat it as no rounding.
            if (length == 0f) corner else corner + edge / length * minOf(radius, length / 2f)
        }

        val start = inset(toPrev)
        val end = inset(toNext)

        if (i == 0) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)
        path.quadraticTo(corner.x, corner.y, end.x, end.y)
    }

    path.close()
    return path
}

private val KEY_HEIGHT = 52.dp

/**
 * How long the thumb has to stay down before the repeat starts. Long enough that
 * a normal tap can never trip it, short enough not to feel stuck.
 */
private const val HOLD_DELAY_MS = 400L

/**
 * One digit every 120 ms once repeating — about eight a second, deliberately
 * slower than a stock keyboard's ~50 ms. An amount is four to six digits, so at
 * keyboard speed the whole field is gone before the thumb reacts, and "hold to
 * fix a typo" quietly becomes "hold to clear everything". This is the one number
 * to turn if the repeat ever feels wrong.
 */
private const val HOLD_REPEAT_MS = 120L
