package com.nijika21.yourmoney.ui.lainnya

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
 * The nav's 5th tab, added purely to keep the Catat FAB in a true 2-2 split
 * (`handoff.md` §6 item 10) — its purpose is genuinely undecided. Candidates
 * so far are a user-notes tab and a minigame tab; this stays a real, visible
 * "coming soon" screen rather than a disabled placeholder until one is picked.
 */
@Composable
fun LainnyaScreen(modifier: Modifier = Modifier) {
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
        ScreenHeader(title = "Lainnya")
        EmptyState("Segera hadir.")
    }
}
