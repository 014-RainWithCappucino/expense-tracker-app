package com.nijika21.yourmoney.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class YourMoneyDimens(
    val screenPadding: Dp = 20.dp,
    val cardPadding: Dp = 16.dp,
    val rowVertical: Dp = 14.dp,
    val gapXs: Dp = 4.dp,
    val gapS: Dp = 8.dp,
    val gapM: Dp = 12.dp,
    val gapL: Dp = 20.dp,
    val gapXl: Dp = 32.dp,
    val hairline: Dp = 1.dp,
    /** Minimum touch target. Rows below this are a bug, not a style choice. */
    val touchTarget: Dp = 48.dp,
)

val YourMoneyDefaultDimens = YourMoneyDimens()
