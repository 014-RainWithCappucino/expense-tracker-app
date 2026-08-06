package com.nijika21.yourmoney.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nijika21.yourmoney.ui.components.BottomActionBar
import com.nijika21.yourmoney.ui.components.CardGroup
import com.nijika21.yourmoney.ui.components.EmptyState
import com.nijika21.yourmoney.ui.components.PrimaryButton
import com.nijika21.yourmoney.ui.components.RowDivider
import com.nijika21.yourmoney.ui.components.ScreenHeader
import com.nijika21.yourmoney.ui.components.SectionHeader
import com.nijika21.yourmoney.ui.components.TextAction
import com.nijika21.yourmoney.ui.components.TxRow
import com.nijika21.yourmoney.ui.components.WalletRow
import com.nijika21.yourmoney.ui.components.YmCard
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Screen 01, rebuilt after the first cut was judged cluttered.
 *
 * What changed and why:
 *
 * - **"Catat tunai" moved to a pinned bottom bar.** It was sitting mid-screen,
 *   out of thumb reach, which is the wrong place for the one thing done daily.
 * - **"Diagnostik" lost its equal billing.** It is a build tool, not a feature;
 *   it belongs as quiet text beside the date, not as a button the same size as
 *   the primary action.
 * - **Wallets moved into a single card with dividers.** Loose rows on the
 *   background with big gaps between them never read as a list.
 * - **One hero number.** "Total" was competing with "Keluar hari ini" at nearly
 *   the same weight; it is now a quiet line at the head of the wallet card.
 * - **The `≈` explanation shrank from three lines to one clause.** It was a
 *   paragraph of grey text on the screen opened most often.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onCatatTunai: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding),
        ) {
            item {
                Spacer(Modifier.height(dimens.gapM))
                ScreenHeader(
                    title = "Hari ini",
                    subtitle = state.hari,
                    action = { TextAction("Diagnostik", onOpenDiagnostics) },
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

            item { SectionHeader("Dompet") }

            item {
                CardGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.gapM),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Total", style = type.label, color = colors.textSecondary)
                            Text(
                                if (state.saldoAwalKosong) {
                                    "saldo awal belum diisi"
                                } else {
                                    "perkiraan, belum dicocokkan"
                                },
                                style = type.caption,
                                color = colors.textSecondary,
                            )
                        }
                        Text(
                            state.totalSaldo,
                            style = type.titleMoney,
                            color = colors.textPrimary,
                        )
                    }
                    state.wallets.forEach { wallet ->
                        RowDivider()
                        WalletRow(wallet)
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
                        state.transaksiHariIni.forEachIndexed { index, row ->
                            if (index > 0) RowDivider()
                            TxRow(row = row, onClick = onOpenTransaction)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(dimens.gapL)) }
        }

        BottomActionBar {
            PrimaryButton(
                text = "Catat tunai",
                onClick = onCatatTunai,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
