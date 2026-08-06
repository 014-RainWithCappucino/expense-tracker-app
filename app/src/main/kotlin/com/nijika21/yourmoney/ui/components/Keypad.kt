package com.nijika21.yourmoney.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * The amount keypad for screen 02.
 *
 * A bespoke keypad rather than a numeric `TextField`, for three reasons that all
 * came out of the concept: the keys are large enough to hit one-handed while
 * standing at a warung, the system keyboard cannot show a "000" key, and there is
 * no cursor to place — digits only ever append or drop off the end. It also means
 * no keyboard animation shifting the layout on every entry.
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
            .background(colors.card, YourMoneyTheme.shapes.cardSmall)
            .pressable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/**
 * The delete key. Same fill and same height as every digit — it is part of the
 * grid, and giving it its own colour made the bottom row look broken.
 *
 * The glyph is drawn rather than imported: the standard backspace icon lives in
 * `material-icons-extended`, and pulling a whole icon library into the build for
 * one arrow is not a trade worth making.
 */
@Composable
private fun BackspaceKey(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = YourMoneyTheme.colors

    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .background(colors.card, YourMoneyTheme.shapes.cardSmall)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 26.dp, height = 18.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 1.6.dp.toPx()
            val nose = h / 2f

            // The tag: a rectangle with a point on the left, pointing at what it
            // removes.
            val outline = Path().apply {
                moveTo(0f, nose)
                lineTo(nose, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(nose, h)
                close()
            }
            drawPath(outline, colors.textPrimary, style = Stroke(width = stroke))

            // The cross inside it.
            val pad = h * 0.3f
            val left = nose + pad
            val right = w - pad
            drawLine(colors.textPrimary, Offset(left, pad), Offset(right, h - pad), stroke)
            drawLine(colors.textPrimary, Offset(right, pad), Offset(left, h - pad), stroke)
        }
    }
}

private val KEY_HEIGHT = 52.dp
