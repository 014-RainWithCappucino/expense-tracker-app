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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.nijika21.yourmoney.ui.components.PrimaryButton
import com.nijika21.yourmoney.ui.components.SecondaryButton
import com.nijika21.yourmoney.ui.components.SectionHeader
import com.nijika21.yourmoney.ui.components.TxRow
import com.nijika21.yourmoney.ui.components.WalletRow
import com.nijika21.yourmoney.ui.components.YmCard
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Screen 01. The question it answers, in one glance from the lock screen up:
 * *how much went out today, and is my money roughly where I think it is.*
 *
 * Built from TDD prose rather than the design bundle, which is still missing
 * (§8). The spacing and type all come from the theme tokens, so when the bundle
 * lands this screen inherits it without edits.
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.gapS),
    ) {
        item {
            Spacer(Modifier.height(dimens.gapL))
            Text(state.hari, style = type.label, color = colors.textMuted)
        }

        item {
            YmCard {
                Text("Keluar hari ini", style = type.label, color = colors.textSecondary)
                Text(
                    state.keluarHariIni,
                    style = type.displayMoney,
                    color = colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapM)) {
                    state.masukHariIni?.let {
                        Text("Masuk $it", style = type.rowMeta, color = colors.masuk)
                    }
                    state.koreksiHariIni?.let {
                        // Corrections are shown, never folded into the figure
                        // above — that is what keeps the headline honest (§6.3).
                        Text("Koreksi $it", style = type.rowMeta, color = colors.koreksi)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapM)) {
                PrimaryButton(
                    text = "Catat tunai",
                    onClick = onCatatTunai,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Diagnostik",
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionHeader("Dompet")
            Text(
                "Total ${state.totalSaldo}",
                style = type.titleMoney,
                color = colors.textPrimary,
            )
            Text(
                "Perkiraan — dihitung dari transaksi yang tercatat, belum dicocokkan " +
                    "dengan saldo asli.",
                style = type.caption,
                color = colors.textMuted,
            )
        }

        items(state.wallets, key = { it.id }) { wallet -> WalletRow(wallet) }

        item { SectionHeader("Hari ini") }

        if (state.hariKosong) {
            item {
                Text(
                    if (state.loading) {
                        "Membuka catatan…"
                    } else {
                        "Belum ada transaksi hari ini."
                    },
                    style = type.body,
                    color = colors.textMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimens.gapL),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(state.transaksiHariIni, key = { it.id }) { row ->
                TxRow(row = row, onClick = onOpenTransaction)
            }
        }

        item { Spacer(Modifier.height(dimens.gapXl)) }
    }
}
