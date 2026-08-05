package com.nijika21.yourmoney.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

val LocalYourMoneyColors: ProvidableCompositionLocal<YourMoneyColors> =
    staticCompositionLocalOf { YourMoneyDarkColors }

val LocalYourMoneyTypography: ProvidableCompositionLocal<YourMoneyTypography> =
    staticCompositionLocalOf { YourMoneyDefaultTypography }

val LocalYourMoneyDimens: ProvidableCompositionLocal<YourMoneyDimens> =
    staticCompositionLocalOf { YourMoneyDefaultDimens }

val LocalYourMoneyShapes: ProvidableCompositionLocal<YourMoneyShapes> =
    staticCompositionLocalOf { YourMoneyDefaultShapes }

/**
 * The app's theme. Material 3 is a base only (TDD §7): we deliberately do not
 * adopt `MaterialTheme.colorScheme`, because its naming does not match the
 * design's token set and the translation layer would be pure noise. Screens
 * read [YourMoneyTheme] instead.
 */
@Composable
fun YourMoneyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalYourMoneyColors provides YourMoneyDarkColors,
        LocalYourMoneyTypography provides YourMoneyDefaultTypography,
        LocalYourMoneyDimens provides YourMoneyDefaultDimens,
        LocalYourMoneyShapes provides YourMoneyDefaultShapes,
        content = content,
    )
}

/** Accessor object, so call sites read `YourMoneyTheme.colors.accentLime`. */
object YourMoneyTheme {
    val colors: YourMoneyColors
        @Composable @ReadOnlyComposable get() = LocalYourMoneyColors.current

    val typography: YourMoneyTypography
        @Composable @ReadOnlyComposable get() = LocalYourMoneyTypography.current

    val dimens: YourMoneyDimens
        @Composable @ReadOnlyComposable get() = LocalYourMoneyDimens.current

    val shapes: YourMoneyShapes
        @Composable @ReadOnlyComposable get() = LocalYourMoneyShapes.current
}

/** Convenience for the common "body text in the muted colour" pairing. */
@Composable
@ReadOnlyComposable
fun mutedBody(): TextStyle =
    YourMoneyTheme.typography.body.copy(color = YourMoneyTheme.colors.textMuted)
