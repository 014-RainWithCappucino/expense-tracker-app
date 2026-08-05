package com.nijika21.yourmoney.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * ⚠ PROVISIONAL FONT. §7 calls for Plus Jakarta Sans as a *bundled* variable
 * font (not the google-fonts runtime downloader — offline start matters).
 * The .ttf is not in the repo yet, so this falls back to the system sans.
 *
 * To finish: drop `plus_jakarta_sans_variable.ttf` into `res/font/`, then
 * change [AppFontFamily] only. Weights below already match the variable
 * axis range the design uses (400/500/600/700/800).
 */
private val AppFontFamily = FontFamily.SansSerif

/**
 * `tnum` on every money glyph (§7). Without it, digits change width as the
 * amount changes and every column in Riwayat/Dompet jitters while scrolling.
 */
private const val TABULAR = "tnum"

@Immutable
data class YourMoneyTypography(
    val displayMoney: TextStyle,
    val titleMoney: TextStyle,
    val bodyMoney: TextStyle,
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val rowTitle: TextStyle,
    val rowMeta: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
)

val YourMoneyDefaultTypography = YourMoneyTypography(
    displayMoney = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        fontFeatureSettings = TABULAR,
    ),
    titleMoney = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = TABULAR,
    ),
    bodyMoney = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR,
    ),
    screenTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    sectionTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    rowTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    rowMeta = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    body = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    label = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    button = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    caption = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
)
