package com.nijika21.yourmoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            // Written out rather than an icon: this screen has no icon set yet,
            // and a mystery glyph on a money keypad is worse than a word.
            Key("hapus", Modifier.weight(1f), onClick = onBackspace)
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
            .height(56.dp)
            .background(colors.card, YourMoneyTheme.shapes.cardSmall)
            .clickable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
