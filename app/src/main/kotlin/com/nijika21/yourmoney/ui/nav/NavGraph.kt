package com.nijika21.yourmoney.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nijika21.yourmoney.ui.diagnostics.DiagnosticsScreen
import com.nijika21.yourmoney.ui.diagnostics.DiagnosticsViewModel
import com.nijika21.yourmoney.ui.home.HomeScreen
import com.nijika21.yourmoney.ui.home.HomeViewModel

/**
 * Routes as plain constants. No arguments anywhere yet, and the transaction
 * detail surface is deliberately *not* a route — it is a sheet hosted by the
 * screen that opened it (§6.8), so annotating a row never loses the list behind
 * the scrim.
 *
 * The PIN gate (§4, M7) will sit in front of `Home` as the graph's start
 * destination. Every entry point has to funnel through it, which is why the graph
 * stays this flat: one gate, one path to test.
 */
object Destination {
    const val HOME = "home"
    const val CATAT = "catat"
    const val DIAGNOSTICS = "diagnostics"
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
    NavHost(
        navController = navController,
        startDestination = Destination.HOME,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Destination.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                state = state,
                onCatatTunai = { navController.navigate(Destination.CATAT) },
                onOpenDiagnostics = { navController.navigate(Destination.DIAGNOSTICS) },
                onOpenTransaction = { /* detail sheet lands next */ },
                modifier = Modifier.systemBarsPadding(),
            )
        }

        composable(Destination.DIAGNOSTICS) {
            val viewModel: DiagnosticsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Notification access is a Settings toggle with no result callback, so
            // coming back to the foreground is the only moment we can re-read it.
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
}
