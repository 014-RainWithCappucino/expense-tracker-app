package com.nijika21.yourmoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * A card that *contains* a set of related rows, with hairlines between them.
 *
 * Rows left loose on the background read as untidy rather than airy — they never
 * group into a list, and the eye has nothing to follow down the screen. Anything
 * that belongs together goes in one of these.
 */
@Composable
fun CardGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val shape = YourMoneyTheme.shapes.card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, shape)
            .border(YourMoneyTheme.dimens.hairline, colors.border, shape)
            .padding(horizontal = YourMoneyTheme.dimens.cardPadding),
        content = content,
    )
}

/** Hairline between rows inside a [CardGroup]. Never above the first or below the last. */
@Composable
fun RowDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(YourMoneyTheme.dimens.hairline)
            .background(YourMoneyTheme.colors.border),
    )
}

/** A padded card for content that is not a row list — the day's headline figure. */
@Composable
fun YmCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val shape = YourMoneyTheme.shapes.card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, shape)
            .border(YourMoneyTheme.dimens.hairline, colors.border, shape)
            .padding(YourMoneyTheme.dimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(YourMoneyTheme.dimens.gapXs),
        content = content,
    )
}

/**
 * Section label. Small text uses `textSecondary`, not `textMuted` — muted on the
 * near-black background is genuinely hard to read at caption size, and these are
 * the strings that say what the numbers mean.
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = YourMoneyTheme.typography.sectionTitle,
        color = YourMoneyTheme.colors.textSecondary,
        modifier = modifier.padding(
            top = YourMoneyTheme.dimens.gapM,
            bottom = YourMoneyTheme.dimens.gapXs,
        ),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = YourMoneyTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = YourMoneyTheme.dimens.touchTarget)
            .background(
                if (enabled) colors.accentLime else colors.cardElevated,
                YourMoneyTheme.shapes.button,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = YourMoneyTheme.typography.button,
            // Ink on lime. Greying the label instead of the fill would be
            // unreadable, so the fill carries the disabled state.
            color = if (enabled) colors.accentLimeInk else colors.textSecondary,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val shape = YourMoneyTheme.shapes.button

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = YourMoneyTheme.dimens.touchTarget)
            .background(colors.card, shape)
            .border(YourMoneyTheme.dimens.hairline, colors.borderStrong, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = YourMoneyTheme.typography.button, color = colors.textPrimary)
    }
}

/** Confirms something destructive. Never the lime button — that one means "save". */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = YourMoneyTheme.dimens.touchTarget)
            .background(colors.danger, YourMoneyTheme.shapes.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = YourMoneyTheme.typography.button,
            // The danger red is light, so it takes dark ink like the lime does.
            color = colors.accentLimeInk,
        )
    }
}

/** Low-weight text action. For things that must be reachable but not prominent. */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = YourMoneyTheme.typography.label,
        color = YourMoneyTheme.colors.textSecondary,
        modifier = modifier
            .heightIn(min = YourMoneyTheme.dimens.touchTarget)
            .clickable(onClick = onClick)
            .padding(
                horizontal = YourMoneyTheme.dimens.gapS,
                vertical = YourMoneyTheme.dimens.gapM,
            ),
    )
}

/**
 * Two-or-more mutually exclusive options inside one track.
 *
 * A shared track is what makes them read as *one choice with two answers*. Free
 * standing pills with a 999dp radius collapse into circles as soon as the label
 * is short, which is exactly what "Keluar" and "Masuk" did.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val dimens = YourMoneyTheme.dimens
    val track = YourMoneyTheme.shapes.button

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, track)
            .border(dimens.hairline, colors.border, track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(
                        if (selected) colors.accentLime else colors.surface,
                        YourMoneyTheme.shapes.cardSmall,
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = YourMoneyTheme.typography.button,
                    color = if (selected) colors.accentLimeInk else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A bottom bar holding the screen's primary action, pinned above the navigation
 * bar. Primary actions live here rather than mid-screen: on a 6.7" phone the top
 * two thirds are out of thumb reach one-handed, and this app gets used standing
 * up at a counter.
 */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val dimens = YourMoneyTheme.dimens

    Column(modifier.fillMaxWidth().background(colors.background)) {
        RowDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding, vertical = dimens.gapM),
            horizontalArrangement = Arrangement.spacedBy(dimens.gapM),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Screen title with an optional low-weight action on the right. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = type.screenTitle, color = colors.textPrimary)
            subtitle?.let {
                Text(it, style = type.rowMeta, color = colors.textSecondary)
            }
        }
        action?.invoke()
    }
}

/** Centred, quiet, and short. An empty day is normal, not an error. */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = YourMoneyTheme.typography.body,
        color = YourMoneyTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = YourMoneyTheme.dimens.gapXl),
    )
}
