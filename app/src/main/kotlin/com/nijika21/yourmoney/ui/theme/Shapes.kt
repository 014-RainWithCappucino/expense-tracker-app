package com.nijika21.yourmoney.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class YourMoneyShapes(
    val card: Shape = RoundedCornerShape(20.dp),
    // A full-bleed row at the top or bottom edge of a CardGroup needs its own
    // matching corners — CardGroup clips to `card`, and a row's default
    // rectangular press wash gets hard-cropped by that clip otherwise.
    val cardTop: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    val cardBottom: Shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
    val cardSmall: Shape = RoundedCornerShape(14.dp),
    val button: Shape = RoundedCornerShape(16.dp),
    val chip: Shape = RoundedCornerShape(999.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val field: Shape = RoundedCornerShape(14.dp),
)

/**
 * Note: the design canvas's 38px "frame radius" and its drop shadow are
 * artefacts of the mockup frame and must not ship (TDD §7). They are
 * deliberately absent from this token set.
 */
val YourMoneyDefaultShapes = YourMoneyShapes()
