package com.nijika21.yourmoney.ui.dompet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nijika21.yourmoney.ui.components.CardGroup
import com.nijika21.yourmoney.ui.components.RowDivider
import com.nijika21.yourmoney.ui.components.ScreenHeader
import com.nijika21.yourmoney.ui.components.WalletRow
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Per-wallet detail. Currently just the Total + per-wallet list Home used to
 * show inline (§6 item 11, moved here verbatim, not redesigned) — balances,
 * per-wallet history and reconciliation still land here for real in M6.
 */
@Composable
fun DompetScreen(state: DompetUiState, modifier: Modifier = Modifier) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = dimens.screenPadding),
    ) {
        Spacer(Modifier.height(dimens.gapM))
        ScreenHeader(title = "Dompet")
        Spacer(Modifier.height(dimens.gapM))

        CardGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.cardPadding, vertical = dimens.gapM),
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
}
