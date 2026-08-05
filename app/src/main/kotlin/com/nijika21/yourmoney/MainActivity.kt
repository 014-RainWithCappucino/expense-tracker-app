package com.nijika21.yourmoney

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijika21.yourmoney.platform.notification.TxNotificationListenerService
import com.nijika21.yourmoney.ui.diagnostics.DiagnosticsScreen
import com.nijika21.yourmoney.ui.diagnostics.DiagnosticsViewModel
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * M1 has no navigation graph yet — one screen, deliberately. The nav graph
 * arrives with M2 (Home) and grows a PIN gate in M7.
 *
 * `FLAG_SECURE` is *not* set here yet: it belongs to M7 (§6.1, §15), and
 * setting it now would block the screenshots that make M1's corpus review
 * easier to share.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: DiagnosticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            YourMoneyTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DiagnosticsScreen(
                    state = state,
                    onOpenListenerSettings = ::openListenerSettings,
                    onClearDiscovered = viewModel::clearDiscovered,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Notification access is a Settings toggle with no result callback,
        // so returning to the foreground is the only moment we can re-read it.
        viewModel.refreshListenerState()
    }

    private fun openListenerSettings() {
        val intent = Intent(TxNotificationListenerService.settingsIntentAction)
        runCatching { startActivity(intent) }.onFailure {
            // Some OEM skins ship without the standard settings activity.
            Toast.makeText(
                this,
                "Buka Pengaturan > Notifikasi > Akses notifikasi secara manual.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
