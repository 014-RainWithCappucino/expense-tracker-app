package com.nijika21.yourmoney.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Every tappable thing in the app goes through here.
 *
 * A flat surface that does not move when pressed reads as a picture of a button
 * rather than a button — the first cut of these screens had no press feedback at
 * all, and felt dead under the thumb. This adds two things: a small spring-loaded
 * squeeze, and a ripple tinted to the app's accent instead of the Material grey.
 *
 * It lives in one place on purpose. Feedback applied per-screen drifts, and this
 * is the modifier every future page should reach for.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // A spring rather than a duration: the release should overshoot back to rest
    // slightly, which is what makes it feel physical instead of animated.
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "pressScale",
    )

    scale(scale).clickable(
        interactionSource = interactionSource,
        indication = ripple(color = rippleTint()),
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun rippleTint() = YourMoneyTheme.colors.accentLime
