package com.nijika21.yourmoney.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.ui.components.CardGroup
import com.nijika21.yourmoney.ui.components.EmptyState
import com.nijika21.yourmoney.ui.components.RowDivider
import com.nijika21.yourmoney.ui.components.ScreenHeader
import com.nijika21.yourmoney.ui.components.SectionHeader
import com.nijika21.yourmoney.ui.components.TxRow
import com.nijika21.yourmoney.ui.components.YmCard
import com.nijika21.yourmoney.ui.components.pressable
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Screen 01, rebuilt after the first cut was judged cluttered.
 *
 * What changed and why:
 *
 * - **"Catat tunai" lives in the bottom nav now**, as the raised FAB between
 *   Riwayat and Dompet (§6 item 10) — not a button on this screen at all.
 * - **"Diagnostik" lost its equal billing.** It is a build tool, not a feature;
 *   it now sits behind a quiet gear icon beside the date, not a button the
 *   same size as the primary action.
 * - **Wallets moved to the Dompet tab entirely** (§6 item 11) — Dompet is a
 *   real nav destination now, so a duplicate summary here just repeated it.
 * - **One hero number.** "Total" was competing with "Keluar hari ini" at nearly
 *   the same weight; it is now a quiet line at the head of the wallet card.
 * - **The `≈` explanation shrank from three lines to one clause.** It was a
 *   paragraph of grey text on the screen opened most often.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenDiagnostics: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = dimens.screenPadding),
    ) {
        item {
            Spacer(Modifier.height(dimens.gapM))
            ScreenHeader(
                title = "Hari ini",
                subtitle = state.hari,
                action = { SettingsIconButton(onClick = onOpenDiagnostics) },
            )
            Spacer(Modifier.height(dimens.gapM))
        }

        item {
            YmCard {
                Text("Keluar", style = type.label, color = colors.textSecondary)
                Text(
                    state.keluarHariIni,
                    style = type.displayMoney,
                    color = colors.textPrimary,
                )
                if (state.masukHariIni != null || state.koreksiHariIni != null) {
                    Spacer(Modifier.height(dimens.gapXs))
                    Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapM)) {
                        state.masukHariIni?.let {
                            Text("Masuk $it", style = type.rowMeta, color = colors.masuk)
                        }
                        // Corrections stay beside the headline, never inside
                        // it — that is what keeps the number honest (§6.3).
                        state.koreksiHariIni?.let {
                            Text("Koreksi $it", style = type.rowMeta, color = colors.koreksi)
                        }
                    }
                }
            }
        }

        item { SectionHeader("Transaksi") }

        if (state.transaksiHariIni.isEmpty()) {
            item {
                EmptyState(
                    if (state.loading) {
                        "Membuka catatan…"
                    } else {
                        "Belum ada apa-apa hari ini."
                    },
                )
            }
        } else {
            item {
                CardGroup {
                    val shapes = YourMoneyTheme.shapes
                    val lastIndex = state.transaksiHariIni.lastIndex
                    state.transaksiHariIni.forEachIndexed { index, row ->
                        if (index > 0) RowDivider()
                        // Only the first/last row touches CardGroup's rounded
                        // clip; a middle row stays the default rectangle.
                        val rowShape = when {
                            lastIndex == 0 -> shapes.card
                            index == 0 -> shapes.cardTop
                            index == lastIndex -> shapes.cardBottom
                            else -> RectangleShape
                        }
                        TxRow(row = row, onClick = onOpenTransaction, shape = rowShape)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(dimens.gapL)) }
    }
}

/**
 * Touch target for the gear, matching every other row/button's minimum
 * (`dimens.touchTarget`) rather than shrinking to the glyph's own size.
 */
@Composable
private fun SettingsIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(YourMoneyTheme.dimens.touchTarget)
            .pressable(shape = YourMoneyTheme.shapes.cardSmall, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(tint = YourMoneyTheme.colors.textSecondary, dim = 28.dp)
    }
}

/**
 * The standard settings-gear silhouette. Round tooth circles fused into
 * scallops read as a flower, not a gear; 8 flat-topped teeth this close
 * together read as a saw blade. Landed on 6 teeth, a thin stroke, and low
 * protrusion past the body edge — a medium body circle with the teeth kept
 * closer to the ring rather than sticking out — chosen against an on-device
 * comparison of teeth count, stroke weight and body size (see `handoff.md`
 * §6 item 9 for the values). Same union/difference/stroke pipeline
 * throughout: body circle ∪ 6 trapezoid teeth (flat top, two softly-clipped
 * outer corners), minus the center hole, stroked rather than filled.
 */
@Composable
private fun GearIcon(tint: Color, modifier: Modifier = Modifier, dim: Dp = 22.dp) {
    Canvas(modifier.size(dim)) {
        val s = this.size.minDimension
        val center = Offset(s * 0.5f, s * 0.5f)
        val bodyRadius = s * 0.27f
        val holeRadius = s * 0.12f
        val teeth = 6

        val halfWidth = s * 0.11f
        // Overlaps well inside bodyRadius, so the tooth fuses cleanly into
        // the ring instead of pinching at the joint.
        val inner = s * 0.16f
        // Low protrusion past the body edge — teeth sit closer to the ring
        // rather than sticking out, per the "bury them a bit more" call.
        val outer = s * 0.315f
        val cornerCut = s * 0.04f

        var silhouette = Path().apply { addOval(Rect(center = center, radius = bodyRadius)) }

        repeat(teeth) { i ->
            val angle = (i * (2.0 * Math.PI / teeth)).toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)
            // Tooth built pointing "north" in local space, then each vertex
            // is rotated by hand into place — simpler and more predictable
            // here than composing a transform matrix for a boolean-op path.
            fun place(lx: Float, ly: Float) =
                Offset(center.x + lx * cosA - ly * sinA, center.y + lx * sinA + ly * cosA)

            val innerLeft = place(-halfWidth, -inner)
            val outerLeftStraight = place(-halfWidth, -(outer - cornerCut))
            val outerLeftCorner = place(-halfWidth, -outer)
            val topLeft = place(-halfWidth + cornerCut, -outer)
            val topRight = place(halfWidth - cornerCut, -outer)
            val outerRightCorner = place(halfWidth, -outer)
            val outerRightStraight = place(halfWidth, -(outer - cornerCut))
            val innerRight = place(halfWidth, -inner)

            val tooth = Path().apply {
                moveTo(innerLeft.x, innerLeft.y)
                lineTo(outerLeftStraight.x, outerLeftStraight.y)
                quadraticTo(outerLeftCorner.x, outerLeftCorner.y, topLeft.x, topLeft.y)
                lineTo(topRight.x, topRight.y)
                quadraticTo(outerRightCorner.x, outerRightCorner.y, outerRightStraight.x, outerRightStraight.y)
                lineTo(innerRight.x, innerRight.y)
                close()
            }
            val merged = Path()
            merged.op(silhouette, tooth, PathOperation.Union)
            silhouette = merged
        }

        val hole = Path().apply { addOval(Rect(center = center, radius = holeRadius)) }
        val gear = Path()
        gear.op(silhouette, hole, PathOperation.Difference)

        drawPath(
            gear,
            tint,
            style = Stroke(width = s * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
