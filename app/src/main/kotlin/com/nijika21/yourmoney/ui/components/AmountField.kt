package com.nijika21.yourmoney.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.domain.money.Rupiah
import com.nijika21.yourmoney.ui.format.RupiahOffsets
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * The amount on screen 02: formatted rupiah with a caret you can place by tapping.
 *
 * **Why this is hand-built rather than a `BasicTextField`.** The obvious version —
 * a read-only text field with a `VisualTransformation` doing the grouping — was
 * tried and failed on the device: tapping it desynced the field's internal edit
 * buffer from the hoisted state and silently rewrote the value (`212000` became
 * `12108` on a single tap, before any key was pressed). A text field also drags
 * in focus and IME behaviour that this screen explicitly does not want, since the
 * keypad below is the only way to type here.
 *
 * So: draw the text, map the tap to a digit index, draw the caret. Every piece is
 * owned, nothing can rewrite the value, and no keyboard can appear.
 */
@Composable
fun AmountField(
    digits: String,
    cursor: Int,
    onCursor: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val empty = digits.isEmpty()
    val formatted = if (empty) Rupiah.format(0) else Rupiah.format(digits.toLong())

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    // A caret that does not blink reads as a rendering artefact. Held fully on
    // for most of the cycle so it is visibly a cursor, not a flicker.
    val blink = rememberInfiniteTransition(label = "caret")
    val caretAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1f at 0 using LinearEasing
                1f at 550 using LinearEasing
                0f at 650 using LinearEasing
                0f at 1000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "caretAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(digits) {
                detectTapGestures { tap ->
                    val result = layout ?: return@detectTapGestures
                    val formattedIndex = result.getOffsetForPosition(tap)
                    onCursor(RupiahOffsets.formattedToRaw(formattedIndex, digits.length))
                }
            },
    ) {
        Text(
            text = formatted,
            style = YourMoneyTheme.typography.displayMoney,
            color = if (empty) colors.textMuted else colors.textPrimary,
            maxLines = 1,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    val result = layout ?: return@drawWithContent
                    // No digits, nothing to place a caret between.
                    if (empty) return@drawWithContent

                    val at = RupiahOffsets.rawToFormatted(
                        cursor.coerceIn(0, digits.length),
                        digits.length,
                    )
                    val rect = result.getCursorRect(at.coerceIn(0, formatted.length))
                    drawLine(
                        color = colors.accentLime,
                        start = Offset(rect.left, rect.top),
                        end = Offset(rect.left, rect.bottom),
                        strokeWidth = 2.dp.toPx(),
                        alpha = caretAlpha,
                    )
                },
        )
    }
}
