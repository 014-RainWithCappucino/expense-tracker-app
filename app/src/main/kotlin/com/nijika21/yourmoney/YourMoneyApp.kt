package com.nijika21.yourmoney

import android.app.Application
import android.util.Log
import com.nijika21.yourmoney.data.repository.LedgerRepository
import com.nijika21.yourmoney.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class YourMoneyApp : Application() {

    @Inject lateinit var ledgerRepository: LedgerRepository

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        // Off the main thread and not awaited: the wallet set is only needed by
        // the time a screen renders, and blocking startup on a DB open would cost
        // the cold-start budget in §10 for nothing.
        scope.launch {
            runCatching { ledgerRepository.seedWalletsIfEmpty() }
                .onFailure { Log.e("YM.App", "Wallet seed failed", it) }
        }
    }
}
