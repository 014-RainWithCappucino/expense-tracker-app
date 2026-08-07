package com.nijika21.yourmoney.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nijika21.yourmoney.ui.cashentry.CatatScreen
import com.nijika21.yourmoney.ui.cashentry.CatatViewModel
import com.nijika21.yourmoney.ui.diagnostics.DiagnosticsScreen
import com.nijika21.yourmoney.ui.detail.TransactionSheetHost
import com.nijika21.yourmoney.ui.detail.TransactionSheetViewModel
import com.nijika21.yourmoney.ui.diagnostics.DiagnosticsViewModel
import com.nijika21.yourmoney.ui.dompet.DompetScreen
import com.nijika21.yourmoney.ui.dompet.DompetViewModel
import com.nijika21.yourmoney.ui.home.HomeScreen
import com.nijika21.yourmoney.ui.home.HomeViewModel
import com.nijika21.yourmoney.ui.lainnya.LainnyaScreen
import com.nijika21.yourmoney.ui.riwayat.RiwayatScreen

/**
 * Routes as plain constants. No arguments anywhere yet, and the transaction
 * detail surface is deliberately *not* a route — it is a sheet hosted by the
 * screen that opened it (§6.8), so annotating a row never loses the list behind
 * the scrim.
 *
 * [HOME], [RIWAYAT], [DOMPET] and [LAINNYA] are the bottom nav's 4 real tabs
 * (`handoff.md` §6 item 10); [CATAT] is reached from the nav's raised FAB, not
 * a tab, and [DIAGNOSTICS] is reached from Home's `•••`, so neither shows the
 * bar (§6 item 9).
 *
 * The PIN gate (§4, M7) will sit in front of `Home` as the graph's start
 * destination. Every entry point has to funnel through it, which is why the graph
 * stays this flat: one gate, one path to test.
 */
object Destination {
    const val HOME = "home"
    const val RIWAYAT = "riwayat"
    const val DOMPET = "dompet"
    const val LAINNYA = "lainnya"
    const val CATAT = "catat"
    const val DIAGNOSTICS = "diagnostics"

    val bottomNavRoutes = setOf(HOME, RIWAYAT, DOMPET, LAINNYA)
}

/**
 * [onOpenListenerSettings] is passed in rather than owned by a ViewModel:
 * launching Settings needs a real Activity context, and routing it through the
 * application context would mean FLAG_ACTIVITY_NEW_TASK plus a task-stack quirk
 * on return, for one button.
 */
@Composable
fun YourMoneyNavGraph(
    onOpenListenerSettings: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destination.HOME,
            modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
            composable(Destination.HOME) {
                val viewModel: HomeViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                val sheetViewModel: TransactionSheetViewModel = hiltViewModel()
                val sheetState by sheetViewModel.state.collectAsStateWithLifecycle()
                val catatanDraft by sheetViewModel.draft.collectAsStateWithLifecycle()

                HomeScreen(
                    state = state,
                    onOpenDiagnostics = { navController.navigate(Destination.DIAGNOSTICS) },
                    onOpenTransaction = sheetViewModel::open,
                    modifier = Modifier.statusBarsPadding(),
                )

                // Hosted by the screen, not a route: the list stays visible behind
                // the scrim, so annotating one of two identical charges is
                // unambiguous.
                TransactionSheetHost(
                    state = sheetState,
                    catatanDraft = catatanDraft,
                    onCatatan = sheetViewModel::setCatatan,
                    onDismiss = sheetViewModel::dismiss,
                    onHapus = sheetViewModel::hapus,
                )
            }

            composable(Destination.RIWAYAT) {
                RiwayatScreen()
            }

            composable(Destination.DOMPET) {
                val viewModel: DompetViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DompetScreen(state = state)
            }

            composable(Destination.LAINNYA) {
                LainnyaScreen()
            }

            composable(Destination.CATAT) {
                val viewModel: CatatViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // One-shot, not state: a saved entry must pop exactly once, and a
                // "saved" boolean in the state object would pop again on every
                // recomposition after it.
                LaunchedEffect(Unit) {
                    viewModel.saved.collect { navController.popBackStack() }
                }

                CatatScreen(
                    state = state,
                    onDigit = viewModel::insertDigit,
                    onTripleZero = viewModel::insertTripleZero,
                    onBackspace = viewModel::backspace,
                    onCursor = viewModel::setCursor,
                    onJenis = viewModel::setJenis,
                    onKeterangan = viewModel::setKeterangan,
                    onTanggal = viewModel::setTanggal,
                    onWaktu = viewModel::setWaktu,
                    onWallet = viewModel::setWallet,
                    onSimpan = viewModel::simpan,
                    onBatal = { navController.popBackStack() },
                    modifier = Modifier.systemBarsPadding(),
                )
            }

            composable(Destination.DIAGNOSTICS) {
                val viewModel: DiagnosticsViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // Notification access is a Settings toggle with no result
                // callback, so coming back to the foreground is the only moment
                // we can re-read it.
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshListenerState()
                }

                DiagnosticsScreen(
                    state = state,
                    onOpenListenerSettings = onOpenListenerSettings,
                    onClearDiscovered = viewModel::clearDiscovered,
                )
            }
        }

        // Hidden on Catat and Diagnostics — neither is a tab, and Catat's own
        // keypad already competes for thumb reach at the bottom of the screen.
        if (currentRoute in Destination.bottomNavRoutes) {
            val selectedTab = when (currentRoute) {
                Destination.RIWAYAT -> NavTab.RIWAYAT
                Destination.DOMPET -> NavTab.DOMPET
                Destination.LAINNYA -> NavTab.LAINNYA
                else -> NavTab.HOME
            }
            YourMoneyBottomNav(
                selected = selectedTab,
                onSelect = { tab ->
                    val route = when (tab) {
                        NavTab.HOME -> Destination.HOME
                        NavTab.RIWAYAT -> Destination.RIWAYAT
                        NavTab.DOMPET -> Destination.DOMPET
                        NavTab.LAINNYA -> Destination.LAINNYA
                    }
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            // Tabs behave like tabs: switching away and back
                            // restores scroll/state instead of rebuilding it,
                            // and never piles up a back-stack of past tabs.
                            popUpTo(Destination.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onCatat = { navController.navigate(Destination.CATAT) },
            )
        }
    }
}
