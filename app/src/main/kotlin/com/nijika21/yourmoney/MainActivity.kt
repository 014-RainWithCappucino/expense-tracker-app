package com.nijika21.yourmoney

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nijika21.yourmoney.platform.notification.TxNotificationListenerService
import com.nijika21.yourmoney.ui.nav.YourMoneyNavGraph
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * One activity, one nav graph. The PIN gate becomes the graph's start destination
 * in M7 — every entry point funnels through it, so there is exactly one shape of
 * graph and one path to test (§4).
 *
 * `FLAG_SECURE` is still not set here: it belongs to M7 (§6.1), and setting it now
 * would block the screenshots that make reviewing the captured corpus possible.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            YourMoneyTheme {
                YourMoneyNavGraph(onOpenListenerSettings = ::openListenerSettings)
            }
        }
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
