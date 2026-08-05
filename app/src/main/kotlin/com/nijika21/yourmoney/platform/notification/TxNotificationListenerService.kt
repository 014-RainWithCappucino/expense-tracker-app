package com.nijika21.yourmoney.platform.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.nijika21.yourmoney.data.repository.CaptureRepository
import com.nijika21.yourmoney.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The capture half of M1 (TDD §3.3).
 *
 * The contract for [onNotificationPosted] is that it returns in well under a
 * millisecond: it runs on a shared system thread, and blocking it degrades
 * notification delivery for the whole device. So this method only reads the
 * extras and hands the write to an application-scoped coroutine. No parsing,
 * no I/O, no allocation of consequence happens here.
 */
@AndroidEntryPoint
class TxNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var captureRepository: CaptureRepository

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener connected; capture is live.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Worth a log: on OEM skins this is the symptom of the app being
        // battery-killed, which is exactly what setup step 4 exists to prevent.
        Log.w(TAG, "Listener disconnected — capture is DOWN until it reconnects.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName == packageName) return

        val extras = notification.notification?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val pkg = notification.packageName
        val key = notification.key
        val postTime = notification.postTime

        scope.launch {
            runCatching {
                captureRepository.capture(
                    packageName = pkg,
                    sbnKey = key,
                    title = title,
                    text = text,
                    bigText = bigText,
                    postTime = postTime,
                )
            }.onFailure { Log.e(TAG, "Capture failed for $pkg", it) }
        }
    }

    /**
     * Not overridden on purpose. A removed notification tells us nothing the
     * ledger cares about, and reacting to it would only add work on the same
     * latency-sensitive thread.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    companion object {
        private const val TAG = "YM.Listener"

        /**
         * Whether the user has granted notification access. There is no
         * permission to request — it is a Settings toggle — so setup step 3
         * deep-links to [settingsIntentAction] and then re-checks this.
         */
        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, TxNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            return flat.split(':').any {
                ComponentName.unflattenFromString(it) == component
            }
        }

        const val settingsIntentAction: String =
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
    }
}
