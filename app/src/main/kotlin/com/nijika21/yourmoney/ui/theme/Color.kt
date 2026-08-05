package com.nijika21.yourmoney.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Design tokens, named to match the design handoff one-to-one (TDD §7).
 *
 * ⚠ PROVISIONAL VALUES. §7 requires these to be a deterministic oklch → sRGB
 * conversion of the design bundle's tokens, with the source oklch kept in a
 * comment beside each. That bundle (`docs/design-handoff/`) is not in the repo,
 * so the values below are a hand-built dark palette standing in for it.
 *
 * When the bundle lands: replace the numbers here and nothing else. Every
 * screen reads these through [LocalYourMoneyColors], so no other file changes.
 * Do NOT paste the "≈ hex" approximations from the handoff — §7 is explicit
 * that a wrong conversion on the lime accent shows on every primary button.
 */
@Immutable
data class YourMoneyColors(
    // surface ramp, darkest → lightest
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val borderStrong: Color,

    // text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    // accent
    val accentLime: Color,
    val accentLimeInk: Color,
    val accentLimeDim: Color,

    // money semantics — direction is carried by `jenis`, never by sign (§2.3),
    // so these are keyed to the semantic role, not to a positive/negative number.
    val masuk: Color,
    val keluar: Color,
    val pindah: Color,
    val koreksi: Color,

    // status
    val warning: Color,
    val danger: Color,
    val success: Color,
)

/** The one palette. The app is dark-only by design — there is no light variant. */
val YourMoneyDarkColors = YourMoneyColors(
    background = Color(0xFF0B0D0C),
    surface = Color(0xFF121614),
    card = Color(0xFF171C19),
    cardElevated = Color(0xFF1E2421),
    border = Color(0xFF262E29),
    borderStrong = Color(0xFF35403A),

    textPrimary = Color(0xFFEDF2EF),
    textSecondary = Color(0xFF9BA8A1),
    textMuted = Color(0xFF6B7873),

    accentLime = Color(0xFFC8F751),
    accentLimeInk = Color(0xFF0B0D0C),
    accentLimeDim = Color(0xFF5C7229),

    masuk = Color(0xFF5BD98A),
    keluar = Color(0xFFFF6B6B),
    pindah = Color(0xFF6FA8FF),
    koreksi = Color(0xFFC08BFF),

    warning = Color(0xFFFFC24B),
    danger = Color(0xFFFF6B6B),
    success = Color(0xFF5BD98A),
)
