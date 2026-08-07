package com.nijika21.yourmoney.ui.nav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Hand-drawn line icons for the bottom nav, one file, so its visual weight
 * stays consistent without pulling in a Material icon font the rest of the
 * app never adopted. Every icon shares the same stroke width and canvas
 * fractions, so they read as one family at nav-tab size.
 */
private const val STROKE_FRACTION = 0.09f

/**
 * Filled, not stroked — the one glyph in this set that gets solid treatment,
 * because a thin house outline collapsed into an ambiguous pentagon at
 * nav-tab size. The roof overhangs the walls (eave tips sit outside the wall
 * x-positions) and a door is cut out of the base by painting it in the bar's
 * own background colour — this icon is only ever drawn on that background,
 * inside [YourMoneyBottomNav].
 */
@Composable
fun HomeIcon(tint: Color, modifier: Modifier = Modifier, dim: Dp = 22.dp) {
    val bg = YourMoneyTheme.colors.background
    Canvas(modifier.size(dim)) {
        val s = this.size.minDimension
        val house = Path().apply {
            moveTo(s * 0.50f, s * 0.10f)
            lineTo(s * 0.90f, s * 0.52f)
            lineTo(s * 0.76f, s * 0.52f)
            lineTo(s * 0.76f, s * 0.88f)
            lineTo(s * 0.24f, s * 0.88f)
            lineTo(s * 0.24f, s * 0.52f)
            lineTo(s * 0.10f, s * 0.52f)
            close()
        }
        drawPath(house, tint)
        drawRoundRect(
            bg,
            topLeft = Offset(s * 0.42f, s * 0.62f),
            size = Size(s * 0.16f, s * 0.26f),
            cornerRadius = CornerRadius(s * 0.03f, s * 0.03f),
        )
    }
}

@Composable
fun RiwayatIcon(tint: Color, modifier: Modifier = Modifier, dim: Dp = 22.dp) {
    Canvas(modifier.size(dim)) {
        val s = this.size.minDimension
        val stroke = Stroke(
            width = s * STROKE_FRACTION,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val center = Offset(s * 0.5f, s * 0.5f)
        drawCircle(tint, radius = s * 0.36f, center = center, style = stroke)
        drawLine(tint, center, Offset(s * 0.5f, s * 0.32f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, center, Offset(s * 0.68f, s * 0.5f), stroke.width, cap = StrokeCap.Round)
    }
}

/**
 * A body close to square, not the wide ~1.46:1 rectangle this used to be —
 * that read as a phone lying on its side rather than a wallet. The
 * fold-over flap peeking above the top edge is what actually disambiguates
 * it: a plain rounded rect with a dot could be almost anything, but a
 * rectangle with a smaller rectangle folded over its top only reads one way.
 */
@Composable
fun DompetIcon(tint: Color, modifier: Modifier = Modifier, dim: Dp = 22.dp) {
    Canvas(modifier.size(dim)) {
        val s = this.size.minDimension
        val stroke = Stroke(
            width = s * STROKE_FRACTION,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val left = s * 0.18f
        val top = s * 0.34f
        val right = s * 0.82f
        val bottom = s * 0.84f
        drawRoundRect(
            tint,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(s * 0.07f, s * 0.07f),
            style = stroke,
        )
        drawRoundRect(
            tint,
            topLeft = Offset(s * 0.32f, s * 0.18f),
            size = Size(s * 0.36f, s * 0.20f),
            cornerRadius = CornerRadius(s * 0.05f, s * 0.05f),
            style = stroke,
        )
        drawCircle(tint, radius = s * 0.045f, center = Offset(right - s * 0.13f, (top + bottom) / 2f))
    }
}

/**
 * A filled sparkle rather than a stroked shape — the one tab in the nav whose
 * job isn't decided yet gets the one icon that stands apart visually too.
 */
@Composable
fun LainnyaIcon(tint: Color, modifier: Modifier = Modifier, dim: Dp = 22.dp) {
    Canvas(modifier.size(dim)) {
        val s = this.size.minDimension
        val cx = s * 0.5f
        val cy = s * 0.5f
        val outer = s * 0.40f
        val inner = s * 0.13f
        val path = Path().apply {
            moveTo(cx, cy - outer)
            lineTo(cx + inner, cy - inner)
            lineTo(cx + outer, cy)
            lineTo(cx + inner, cy + inner)
            lineTo(cx, cy + outer)
            lineTo(cx - inner, cy + inner)
            lineTo(cx - outer, cy)
            lineTo(cx - inner, cy - inner)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
fun PlusIcon(tint: Color, modifier: Modifier = Modifier, dim: Dp = 24.dp) {
    Canvas(modifier.size(dim)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.11f, cap = StrokeCap.Round)
        val cx = s * 0.5f
        val cy = s * 0.5f
        val r = s * 0.32f
        drawLine(tint, Offset(cx, cy - r), Offset(cx, cy + r), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(cx - r, cy), Offset(cx + r, cy), stroke.width, cap = StrokeCap.Round)
    }
}
