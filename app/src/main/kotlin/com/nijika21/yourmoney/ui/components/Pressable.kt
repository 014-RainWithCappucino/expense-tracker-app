package com.nijika21.yourmoney.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Every tappable thing in the app goes through here.
 *
 * A flat surface that does not move when pressed reads as a picture of a button
 * rather than a button. This gives three things at once: a spring-loaded squeeze,
 * a wash of colour that fades in under the thumb, and a ripple — all of them
 * clipped to the caller's own shape.
 *
 * **This draws the surface itself — do not paint a background before it.** Two
 * bugs came out of letting the caller do that. Without a clip, the ripple and the
 * wash were drawn as rectangles over a rounded background and visibly overran
 * every corner. And because the squeeze only scales what comes *after* it in the
 * chain, a background painted before it stayed full size while the wash shrank,
 * leaving a bright ring around every pressed button. Both disappear once one
 * modifier owns [fill], [border], [shape] and the scale together.
 *
 * **[press] is the colour of the feedback**, used by both the wash and the ripple.
 * It defaults to the lime accent, which is correct on the dark surfaces. On a fill
 * that is already light — the lime primary button, the danger red — lime on lime is
 * invisible, so those pass the dark ink instead and the press reads as a darkening.
 *
 * Pass [interactionSource] when the caller needs to know the key is being *held*
 * rather than merely clicked — the keypad's backspace repeats while it is down.
 * Everything else can leave it null and get a private one.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    fill: Color = Color.Transparent,
    border: Color = Color.Unspecified,
    press: Color = Color.Unspecified,
    scaleDown: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val colors = YourMoneyTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val active = pressed && enabled

    val tint = if (press.isSpecified) press else colors.accentLime

    // A spring rather than a duration: the release should overshoot back to rest
    // slightly, which is what makes it feel physical instead of animated.
    val scale by animateFloatAsState(
        targetValue = if (active) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "pressScale",
    )

    // The ripple on its own is nearly invisible against these near-black fills —
    // it is a brief travelling edge and then nothing. The wash is what actually
    // says the key took the press: it holds for as long as the thumb is down.
    // Crossfaded at 180 ms like every other colour change in the app, so a fast
    // tap still registers as a flash rather than snapping on and off.
    val wash by animateColorAsState(
        targetValue = if (active) tint.copy(alpha = WASH_ALPHA) else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "pressWash",
    )

    // Order matters and is load-bearing. Fill and border first, each with the real
    // shape, so the stroke is not half-eaten by the clip that follows. Then the
    // clip, so the wash and the ripple stop at the same edge. All of it after the
    // scale, so the whole surface squeezes as one piece.
    scale(scale)
        .background(fill, shape)
        .then(
            if (border.isSpecified) {
                Modifier.border(YourMoneyTheme.dimens.hairline, border, shape)
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(wash)
        .clickable(
            interactionSource = source,
            indication = ripple(color = tint),
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * Strong enough to be unmistakable on `card` at arm's length, low enough that the
 * label on top never loses contrast. Tuned on the device, not on a monitor.
 */
private const val WASH_ALPHA = 0.18f
