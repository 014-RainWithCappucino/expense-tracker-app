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
            // Function key, not a value key, and styled as one — set in the same
            // heavy figures as the digits it would read as something typeable.
            Key("hapus", Modifier.weight(1f), function = true, onClick = onBackspace)
        }
    }
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier = Modifier,
    function: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = YourMoneyTheme.colors

    Text(
        text = label,
        style = if (function) {
            YourMoneyTheme.typography.label
        } else {
            YourMoneyTheme.typography.titleMoney
        },
        color = if (function) colors.textSecondary else colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .height(52.dp)
            .background(
                if (function) colors.surface else colors.card,
                YourMoneyTheme.shapes.cardSmall,
            )
            .clickable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
