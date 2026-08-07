package com.nijika21.yourmoney.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.ui.components.RowDivider
import com.nijika21.yourmoney.ui.components.pressable
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Home, Riwayat, Dompet and one more slot whose job isn't decided yet — see
 * `handoff.md` §6 item 10. Deliberately 4 real tabs plus the Catat FAB rather
 * than 3: 3 tabs left the FAB in a lopsided 2-1 split no amount of layout math
 * made look right, so the fix was a genuine 5th destination instead.
 */
enum class NavTab(val label: String) {
    HOME("Home"),
    RIWAYAT("Riwayat"),
    DOMPET("Dompet"),
    LAINNYA("Lainnya"),
}

private val NAV_HEIGHT = 64.dp
private val FAB_SIZE = 60.dp
private val TAB_ICON_DIM = 22.dp

/**
 * The bar sits in normal flow; the Catat FAB is drawn as a sibling on top of
 * it, offset upward by exactly half its own height, so it straddles the bar's
 * top edge — half floating above the content behind it, half resting on the
 * bar. That's what makes it read as *raised* rather than merely centered.
 */
@Composable
fun YourMoneyBottomNav(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    onCatat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors

    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().background(colors.background)) {
            RowDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(NAV_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavTabItem(NavTab.HOME, selected == NavTab.HOME, { onSelect(NavTab.HOME) }, Modifier.weight(1f))
                NavTabItem(NavTab.RIWAYAT, selected == NavTab.RIWAYAT, { onSelect(NavTab.RIWAYAT) }, Modifier.weight(1f))
                // No icon here — the FAB floats above this slot and already
                // handles the tap. Just the label, so "+" doesn't read as
                // unexplained: every other slot names itself underneath too.
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.height(TAB_ICON_DIM))
                    Spacer(Modifier.height(2.dp))
                    Text("Catat", style = YourMoneyTheme.typography.caption, color = colors.textSecondary)
                }
                NavTabItem(NavTab.DOMPET, selected == NavTab.DOMPET, { onSelect(NavTab.DOMPET) }, Modifier.weight(1f))
                NavTabItem(NavTab.LAINNYA, selected == NavTab.LAINNYA, { onSelect(NavTab.LAINNYA) }, Modifier.weight(1f))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -(FAB_SIZE / 2))
                .size(FAB_SIZE)
                .pressable(
                    shape = CircleShape,
                    fill = colors.accentLime,
                    press = colors.accentLimeInk,
                    onClick = onCatat,
                ),
            contentAlignment = Alignment.Center,
        ) {
            PlusIcon(tint = colors.accentLimeInk)
        }
    }
}

@Composable
private fun NavTabItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val tint = if (selected) colors.accentLime else colors.textSecondary

    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (tab) {
            NavTab.HOME -> HomeIcon(tint)
            NavTab.RIWAYAT -> RiwayatIcon(tint)
            NavTab.DOMPET -> DompetIcon(tint)
            NavTab.LAINNYA -> LainnyaIcon(tint)
        }
        Spacer(Modifier.height(2.dp))
        Text(tab.label, style = YourMoneyTheme.typography.caption, color = tint)
    }
}
