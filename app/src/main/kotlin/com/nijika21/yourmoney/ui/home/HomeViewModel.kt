package com.nijika21.yourmoney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijika21.yourmoney.data.repository.LedgerRepository
import com.nijika21.yourmoney.domain.ledger.PeriodTotals
import com.nijika21.yourmoney.domain.model.Transaction
import com.nijika21.yourmoney.domain.money.Rupiah
import com.nijika21.yourmoney.domain.time.TimeProvider
import com.nijika21.yourmoney.domain.time.dayWindow
import com.nijika21.yourmoney.ui.components.TxRowUi
import com.nijika21.yourmoney.ui.components.WalletRowUi
import com.nijika21.yourmoney.ui.format.formatClock
import com.nijika21.yourmoney.ui.format.formatDay
import com.nijika21.yourmoney.ui.format.formatRowAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Home's state. Money and dates arrive already formatted (§4) — a composable that
 * formats its own currency is how the `≈`, the U+2212 and the tabular figures
 * drift apart between screens.
 */
data class HomeUiState(
    val hari: String = "",
    val keluarHariIni: String = Rupiah.format(0),
    val masukHariIni: String? = null,
    val koreksiHariIni: String? = null,
    val wallets: List<WalletRowUi> = emptyList(),
    val totalSaldo: String = Rupiah.approx(0),
    val transaksiHariIni: List<TxRowUi> = emptyList(),
    val loading: Boolean = true,
) {
    val hariKosong: Boolean get() = transaksiHariIni.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ledger: LedgerRepository,
    private val time: TimeProvider,
) : ViewModel() {

    /**
     * The window is resolved when collection starts, not when the ViewModel is
     * built — `flow { emitAll(…) }` is what defers it. Because the state is
     * `WhileSubscribed`, every return to the foreground recomputes "hari ini",
     * so a phone left overnight does not keep showing yesterday.
     *
     * A rollover *while the screen is open* is not handled here on purpose: that
     * is the day-roller in M4, which has to fire whether or not Home is visible.
     */
    private val todayTransactions: Flow<List<Transaction>> = flow {
        val window = time.dayWindow()
        emitAll(ledger.observeBetween(window.from, window.until))
    }

    val uiState: StateFlow<HomeUiState> = combine(
        ledger.observeWallets(),
        todayTransactions,
    ) { wallets, transaksi ->
        val totals = PeriodTotals.of(transaksi)
        val zone = time.zone()

        HomeUiState(
            hari = formatDay(time.nowMillis(), zone),
            keluarHariIni = Rupiah.format(totals.keluar),
            // Hidden when zero. A "Masuk Rp0" line every day is noise on the one
            // screen that has to be readable at a glance.
            masukHariIni = totals.masuk.takeIf { it != 0L }?.let { Rupiah.signed(it) },
            koreksiHariIni = totals.koreksi.takeIf { it != 0L }?.let { Rupiah.signed(it) },
            wallets = wallets.map { entry ->
                WalletRowUi(
                    id = entry.wallet.id,
                    nama = entry.wallet.nama,
                    saldo = Rupiah.approx(entry.saldo),
                    terhubung = entry.wallet.terhubung,
                )
            },
            totalSaldo = Rupiah.approx(wallets.sumOf { it.saldo }),
            transaksiHariIni = transaksi.map { tx ->
                val walletName = wallets.firstOrNull { it.wallet.id == tx.walletId }
                    ?.wallet?.nama
                    ?: tx.walletId
                TxRowUi(
                    id = tx.id,
                    keterangan = tx.keterangan,
                    walletAndTime = "$walletName · ${formatClock(tx.waktu, zone)}",
                    catatan = tx.catatan,
                    amount = formatRowAmount(tx.jenis, tx.nominal),
                    jenis = tx.jenis,
                )
            },
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
