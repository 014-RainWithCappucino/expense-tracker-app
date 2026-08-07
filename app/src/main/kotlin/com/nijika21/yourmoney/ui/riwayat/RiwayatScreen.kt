package com.nijika21.yourmoney.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nijika21.yourmoney.ui.components.EmptyState
import com.nijika21.yourmoney.ui.components.ScreenHeader
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Full transaction history, its own tab as of #5's nav shell (`handoff.md`
 * §6 item 10). Home only ever shows "hari ini" — this is where the rest of
 * the ledger will live once M5 builds it for real.
 */
@Composable
fun RiwayatScreen(modifier: Modifier = Modifier) {
    val colors = YourMoneyTheme.colors
    val dimens = YourMoneyTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = dimens.screenPadding),
    ) {
        Spacer(Modifier.height(dimens.gapM))
        ScreenHeader(title = "Riwayat")
        EmptyState("Riwayat lengkap segera hadir.")
    }
}
