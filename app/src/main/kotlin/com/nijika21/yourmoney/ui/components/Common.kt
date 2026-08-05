package com.nijika21.yourmoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/** The one card shape used across every screen: surface, hairline, rounded. */
@Composable
fun YmCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val dimens = YourMoneyTheme.dimens
    val shape = YourMoneyTheme.shapes.card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, shape)
            .border(dimens.hairline, colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(dimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.gapS),
        content = content,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Spacer(Modifier.height(YourMoneyTheme.dimens.gapM))
        Text(
            text.uppercase(),
            style = YourMoneyTheme.typography.sectionTitle,
            color = YourMoneyTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(YourMoneyTheme.dimens.gapXs))
    }
}

/**
 * The lime button. Disabled state dims the fill rather than greying the label,
 * because the label is ink on lime and grey-on-lime is unreadable.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = YourMoneyTheme.colors
    val dimens = YourMoneyTheme.dimens

    Text(
        text = text,
        style = YourMoneyTheme.typography.button,
        color = if (enabled) colors.accentLimeInk else colors.textMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchTarget)
            .background(
                if (enabled) colors.accentLime else colors.cardElevated,
                YourMoneyTheme.shapes.button,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/** Bordered, no fill. For the second action on a screen, never the first. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val dimens = YourMoneyTheme.dimens
    val shape = YourMoneyTheme.shapes.button

    Text(
        text = text,
        style = YourMoneyTheme.typography.button,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchTarget)
            .background(colors.card, shape)
            .border(dimens.hairline, colors.borderStrong, shape)
            .clickable(onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
