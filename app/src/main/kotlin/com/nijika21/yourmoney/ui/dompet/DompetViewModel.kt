package com.nijika21.yourmoney.ui.dompet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijika21.yourmoney.data.repository.LedgerRepository
import com.nijika21.yourmoney.domain.money.Rupiah
import com.nijika21.yourmoney.ui.components.WalletRowUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The wallet summary Home used to show inline (§6 item 11) — moved here
 * verbatim once Dompet became a real nav destination, so the two screens
 * stopped repeating each other.
 */
data class DompetUiState(
    val wallets: List<WalletRowUi> = emptyList(),
    val totalSaldo: String = Rupiah.approx(0),
    /** Same caveat as it carried on Home: every wallet still opens at zero. */
    val saldoAwalKosong: Boolean = false,
)

@HiltViewModel
class DompetViewModel @Inject constructor(
    private val ledger: LedgerRepository,
) : ViewModel() {

    val uiState: StateFlow<DompetUiState> = ledger.observeWallets()
        .map { wallets ->
            DompetUiState(
                wallets = wallets.map { entry ->
                    WalletRowUi(
                        id = entry.wallet.id,
                        nama = entry.wallet.nama,
                        saldo = Rupiah.approx(entry.saldo),
                        terhubung = entry.wallet.terhubung,
                    )
                },
                totalSaldo = Rupiah.approx(wallets.sumOf { it.saldo }),
                saldoAwalKosong = wallets.isNotEmpty() && wallets.all { it.wallet.saldoAwal == 0L },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DompetUiState(),
        )
}
