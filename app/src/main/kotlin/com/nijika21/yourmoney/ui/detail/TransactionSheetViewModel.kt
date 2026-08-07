package com.nijika21.yourmoney.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijika21.yourmoney.data.repository.LedgerRepository
import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.domain.model.Sumber
import com.nijika21.yourmoney.domain.time.TimeProvider
import com.nijika21.yourmoney.ui.format.displayKeterangan
import com.nijika21.yourmoney.ui.format.formatDateTime
import com.nijika21.yourmoney.ui.format.formatRowAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionSheetState(
    val id: String? = null,
    val keterangan: String = "",
    val amount: String = "",
    val jenis: Jenis = Jenis.KELUAR,
    val walletNama: String = "",
    val walletTujuanNama: String? = null,
    val waktu: String = "",
    val otomatis: Boolean = false,
    val sudahDiubah: Boolean = false,
) {
    val terbuka: Boolean get() = id != null
}

/**
 * The transaction detail sheet (§6.8, §14).
 *
 * It is not a navigation destination. A sheet keeps the list visible behind the
 * scrim, which is what removes the misattribution risk a full-screen push
 * creates — two Indomaret charges on the same day are indistinguishable once the
 * list is gone.
 *
 * The open row's id and the note draft both live in [SavedStateHandle], so a
 * rotation or a background kill mid-typing does not discard either.
 */
@HiltViewModel
class TransactionSheetViewModel @Inject constructor(
    private val ledger: LedgerRepository,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val openId = savedState.getStateFlow<String?>(KEY_OPEN_ID, null)

    /**
     * Live draft, which is what the field shows — not the stored value.
     *
     * Collected separately from [state] so the note field updates the instant
     * [setCatatan] runs. Folding this into the [combine] below would make every
     * keystroke wait on `transaction`/`wallets` to re-settle too, which is
     * exactly the extra hop that made the placeholder linger over the first
     * character typed.
     */
    val draft: StateFlow<String> = savedState.getStateFlow(KEY_DRAFT, "")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val transaction = openId.flatMapLatest { id ->
        if (id == null) flowOf(null) else ledger.observeTransaction(id)
    }

    val state: StateFlow<TransactionSheetState> = combine(
        transaction,
        ledger.observeWallets(),
    ) { tx, wallets ->
        if (tx == null) return@combine TransactionSheetState()

        fun namaOf(walletId: String?) =
            wallets.firstOrNull { it.wallet.id == walletId }?.wallet?.nama

        TransactionSheetState(
            id = tx.id,
            keterangan = displayKeterangan(tx.keterangan, tx.jenis),
            amount = formatRowAmount(tx.jenis, tx.nominal),
            jenis = tx.jenis,
            walletNama = namaOf(tx.walletId) ?: tx.walletId,
            walletTujuanNama = namaOf(tx.walletTujuanId),
            waktu = formatDateTime(tx.waktu, time.zone()),
            otomatis = tx.sumber == Sumber.OTOMATIS,
            sudahDiubah = tx.editedAt != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionSheetState(),
    )

    fun open(id: String) {
        savedState[KEY_OPEN_ID] = id
        // The draft seeds from what is stored, read off the DB rather than passed
        // in from the tapped row, so the sheet can never show a stale note.
        viewModelScope.launch {
            savedState[KEY_DRAFT] = ledger.observeTransaction(id).first()?.catatan.orEmpty()
        }
    }

    fun setCatatan(value: String) {
        savedState[KEY_DRAFT] = value
    }

    /**
     * Notes commit on dismiss with no Simpan button (§6.8): there is no invalid
     * state and nothing destructive, and an explicit save creates the failure mode
     * "typed a note, swiped down, lost it".
     *
     * Amount, wallet and delete are the opposite — they move balances, so they
     * keep an explicit confirm.
     */
    fun dismiss() {
        val id = openId.value
        val text = draft.value
        savedState[KEY_OPEN_ID] = null
        savedState[KEY_DRAFT] = ""
        if (id != null) {
            // setCatatan in the repository compares before writing, so closing the
            // sheet without typing does not stamp editedAt.
            viewModelScope.launch { ledger.setCatatan(id, text) }
        }
    }

    fun hapus() {
        val id = openId.value ?: return
        savedState[KEY_OPEN_ID] = null
        savedState[KEY_DRAFT] = ""
        viewModelScope.launch { ledger.hapus(id) }
    }

    private companion object {
        const val KEY_OPEN_ID = "openId"
        const val KEY_DRAFT = "catatanDraft"
    }
}
