package com.nijika21.yourmoney.ui.cashentry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijika21.yourmoney.data.repository.LedgerRepository
import com.nijika21.yourmoney.domain.model.Jenis
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
    /** Raw digits. The `Rp` and the dots are a visual layer over this. */
    val nominalText: String = "",
    /** Caret position within [nominalText], so a mistyped digit can be picked out. */
    val cursor: Int = 0,
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
    private val cursor = savedState.getStateFlow(KEY_CURSOR, 0)
    private val jenis = savedState.getStateFlow(KEY_JENIS, Jenis.KELUAR.name)
    private val keterangan = savedState.getStateFlow(KEY_KETERANGAN, "")
    private val catatan = savedState.getStateFlow(KEY_CATATAN, "")
    private val walletId = savedState.getStateFlow<String?>(KEY_WALLET, null)
    private val menyimpan = savedState.getStateFlow(KEY_MENYIMPAN, false)

    private val tersimpan = Channel<Unit>(Channel.BUFFERED)

    /** One-shot, so a save navigates exactly once and not again on recomposition. */
    val saved: Flow<Unit> = tersimpan.receiveAsFlow()

    /** The form, as one value. The separate flows would not fit `combine`. */
    private data class Draft(
        val nominal: String,
        val cursor: Int,
        val jenis: String,
        val keterangan: String,
        val catatan: String,
        val walletId: String?,
    )

    /** Amount and caret move together, so they collapse into one flow first. */
    private val amount: Flow<Pair<String, Int>> =
        combine(nominal, cursor) { text, at -> text to at.coerceIn(0, text.length) }

    private val draft: Flow<Draft> =
        combine(amount, jenis, keterangan, catatan, walletId) { a, j, k, c, w ->
            Draft(
                nominal = a.first,
                cursor = a.second,
                jenis = j,
                keterangan = k,
                catatan = c,
                walletId = w,
            )
        }

    val uiState: StateFlow<CatatUiState> = combine(
        ledger.observeWallets(),
        draft,
        menyimpan,
    ) { wallets, form, saving ->
        CatatUiState(
            nominalText = form.nominal,
            cursor = form.cursor,
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

    /**
     * All three edits act at the caret, not at the end of the string. Typing
     * `205000` when you meant `25000` should cost one tap and one backspace, not
     * a full retype — which is what an append-only keypad forced.
     */
    fun insertDigit(digit: Char) {
        val current = nominal.value
        // 12 digits is a hundred billion rupiah. Past that it is a typo, and the
        // cap keeps the display from overflowing its line.
        if (current.length >= MAX_DIGITS) return
        // A leading zero is never meaningful in an amount, and allowing one makes
        // "0" and "" two states that look identical.
        if (cursor.value == 0 && digit == '0') return
        insert(digit.toString())
    }

    fun insertTripleZero() {
        val current = nominal.value
        if (current.isEmpty()) return
        if (current.length + 3 > MAX_DIGITS) return
        insert("000")
    }

    fun backspace() {
        val current = nominal.value
        val at = caret(current)
        if (at == 0) return
        savedState[KEY_NOMINAL] = current.removeRange(at - 1, at)
        savedState[KEY_CURSOR] = at - 1
    }

    /** Where the user tapped. Clamped, because the field's length changes under it. */
    fun setCursor(index: Int) {
        savedState[KEY_CURSOR] = index.coerceIn(0, nominal.value.length)
    }

    private fun insert(digits: String) {
        val current = nominal.value
        val at = caret(current)
        savedState[KEY_NOMINAL] = current.substring(0, at) + digits + current.substring(at)
        savedState[KEY_CURSOR] = at + digits.length
    }

    private fun caret(current: String): Int = cursor.value.coerceIn(0, current.length)

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
                    // worth typing — and it is stored empty rather than filled
                    // with "Tunai", which only repeated the wallet name back in
                    // the row. The list supplies a title at display time.
                    keterangan = state.keterangan.trim(),
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
        const val MAX_DIGITS = 12
        const val KEY_NOMINAL = "nominal"
        const val KEY_CURSOR = "cursor"
        const val KEY_JENIS = "jenis"
        const val KEY_KETERANGAN = "keterangan"
        const val KEY_CATATAN = "catatan"
        const val KEY_WALLET = "walletId"
        const val KEY_MENYIMPAN = "menyimpan"
    }
}
