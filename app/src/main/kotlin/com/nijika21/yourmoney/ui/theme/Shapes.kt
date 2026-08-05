package com.nijika21.yourmoney.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class YourMoneyShapes(
    val card: Shape = RoundedCornerShape(20.dp),
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
