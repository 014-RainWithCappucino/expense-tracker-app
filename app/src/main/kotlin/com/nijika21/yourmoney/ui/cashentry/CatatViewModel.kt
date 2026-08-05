package com.nijika21.yourmoney.ui.cashentry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijika21.yourmoney.data.repository.LedgerRepository
import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.domain.money.Rupiah
import com.nijika21.yourmoney.domain.time.TimeProvider
import com.nijika21.yourmoney.ui.format.formatClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletChoice(val id: String, val nama: String)

data class CatatUiState(
    val nominalText: String = "",
    val nominalDisplay: String = Rupiah.format(0),
    val jenis: Jenis = Jenis.KELUAR,
    val keterangan: String = "",
    val catatan: String = "",
    val waktu: String = "",
    val wallets: List<WalletChoice> = emptyList(),
    val walletId: String? = null,
    val menyimpan: Boolean = false,
) {
    /** Zero is not a transaction. Everything else about the form is optional. */
    val bisaSimpan: Boolean
        get() = !menyimpan && walletId != null && (nominalText.toLongOrNull() ?: 0L) > 0L
}

/**
 * Screen 02 — the only place a transaction is created by hand.
 *
 * Draft fields live in [SavedStateHandle] rather than `remember`, so rotating the
 * phone or losing the process to a background kill mid-entry does not silently
 * discard a half-typed amount. §6.8 asks for this in the detail sheet; the same
 * reasoning applies with more force here, where there is more to lose.
 */
@HiltViewModel
class CatatViewModel @Inject constructor(
    private val ledger: LedgerRepository,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val nominal = savedState.getStateFlow(KEY_NOMINAL, "")
    private val jenis = savedState.getStateFlow(KEY_JENIS, Jenis.KELUAR.name)
    private val keterangan = savedState.getStateFlow(KEY_KETERANGAN, "")
    private val catatan = savedState.getStateFlow(KEY_CATATAN, "")
    private val walletId = savedState.getStateFlow<String?>(KEY_WALLET, null)
    private val menyimpan = savedState.getStateFlow(KEY_MENYIMPAN, false)

    private val tersimpan = Channel<Unit>(Channel.BUFFERED)

    /** One-shot, so a save navigates exactly once and not again on recomposition. */
    val saved: Flow<Unit> = tersimpan.receiveAsFlow()

    /** The form, as one value. Six separate flows would not fit `combine`. */
    private data class Draft(
        val nominal: String,
        val jenis: String,
        val keterangan: String,
        val catatan: String,
        val walletId: String?,
    )

    private val draft: Flow<Draft> =
        combine(nominal, jenis, keterangan, catatan, walletId) { n, j, k, c, w ->
            Draft(nominal = n, jenis = j, keterangan = k, catatan = c, walletId = w)
        }

    val uiState: StateFlow<CatatUiState> = combine(
        ledger.observeWallets(),
        draft,
        menyimpan,
    ) { wallets, form, saving ->
        val amount = form.nominal.toLongOrNull() ?: 0L

        CatatUiState(
            nominalText = form.nominal,
            nominalDisplay = Rupiah.format(amount),
            jenis = Jenis.valueOf(form.jenis),
            keterangan = form.keterangan,
            catatan = form.catatan,
            waktu = "Sekarang, ${formatClock(time.nowMillis(), time.zone())}",
            wallets = wallets.map { WalletChoice(it.wallet.id, it.wallet.nama) },
            // Cash is the default because this screen exists for cash. The other
            // wallets are still offered: a missed notification has to be
            // recordable by hand, or the ledger stops reconciling.
            walletId = form.walletId
                ?: wallets.firstOrNull { !it.wallet.terhubung }?.wallet?.id
                ?: wallets.firstOrNull()?.wallet?.id,
            menyimpan = saving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatatUiState(),
    )

    fun appendDigit(digit: Char) {
        val current = nominal.value
        // 12 digits is a hundred billion rupiah. Past that it is a typo, and the
        // cap keeps the display from overflowing its line.
        if (current.length >= 12) return
        if (current.isEmpty() && digit == '0') return
        savedState[KEY_NOMINAL] = current + digit
    }

    fun appendTripleZero() {
        val current = nominal.value
        if (current.isEmpty() || current.length > 9) return
        savedState[KEY_NOMINAL] = current + "000"
    }

    fun backspace() {
        savedState[KEY_NOMINAL] = nominal.value.dropLast(1)
    }

    fun setJenis(value: Jenis) {
        savedState[KEY_JENIS] = value.name
    }

    fun setKeterangan(value: String) {
        savedState[KEY_KETERANGAN] = value
    }

    fun setCatatan(value: String) {
        savedState[KEY_CATATAN] = value
    }

    fun setWallet(id: String) {
        savedState[KEY_WALLET] = id
    }

    fun simpan() {
        val state = uiState.value
        if (!state.bisaSimpan) return
        val walletId = state.walletId ?: return
        val amount = state.nominalText.toLongOrNull() ?: return

        savedState[KEY_MENYIMPAN] = true
        viewModelScope.launch {
            runCatching {
                ledger.catat(
                    jenis = state.jenis,
                    nominal = amount,
                    walletId = walletId,
                    // An empty label is normal — most cash spending has no name
                    // worth typing. "Tunai" is the honest fallback, not a guess.
                    keterangan = state.keterangan.trim().ifEmpty { "Tunai" },
                    catatan = state.catatan,
                )
            }.onSuccess {
                tersimpan.send(Unit)
            }.also {
                savedState[KEY_MENYIMPAN] = false
            }
        }
    }

    private companion object {
        const val KEY_NOMINAL = "nominal"
        const val KEY_JENIS = "jenis"
        const val KEY_KETERANGAN = "keterangan"
        const val KEY_CATATAN = "catatan"
        const val KEY_WALLET = "walletId"
        const val KEY_MENYIMPAN = "menyimpan"
    }
}
