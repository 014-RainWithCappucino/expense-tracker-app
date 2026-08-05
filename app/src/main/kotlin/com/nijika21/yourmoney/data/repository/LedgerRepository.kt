package com.nijika21.yourmoney.data.repository

import com.nijika21.yourmoney.data.db.dao.TransactionDao
import com.nijika21.yourmoney.data.db.dao.WalletDao
import com.nijika21.yourmoney.data.db.entity.toDomain
import com.nijika21.yourmoney.data.db.entity.toEntity
import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.domain.model.Sumber
import com.nijika21.yourmoney.domain.model.Transaction
import com.nijika21.yourmoney.domain.model.Wallet
import com.nijika21.yourmoney.domain.model.WalletJenis
import com.nijika21.yourmoney.domain.model.WalletWithBalance
import com.nijika21.yourmoney.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The read and write side of the ledger.
 *
 * Every read is a Room `Flow` (§4) — no in-memory mirror of DB state exists
 * anywhere, and balances come from the SQL aggregate rather than being stored, so
 * a wrong balance is impossible by construction rather than by discipline.
 */
@Singleton
class LedgerRepository @Inject constructor(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val time: TimeProvider,
) {

    fun observeWallets(): Flow<List<WalletWithBalance>> =
        walletDao.observeWithBalances().map { rows ->
            rows.map { row ->
                WalletWithBalance(
                    wallet = Wallet(
                        id = row.id,
                        nama = row.nama,
                        jenis = WalletJenis.valueOf(row.jenis),
                        terhubung = row.terhubung,
                        saldoAwal = row.saldoAwal,
                        urutan = row.urutan,
                        packageHint = row.packageHint,
                    ),
                    saldo = row.saldo,
                )
            }
        }

    fun observeBetween(from: Long, until: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(from, until).map { rows -> rows.map { it.toDomain() } }

    fun observeRecent(limit: Int = 50): Flow<List<Transaction>> =
        transactionDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    fun observeTransaction(id: String): Flow<Transaction?> =
        transactionDao.observeById(id).map { it?.toDomain() }

    /**
     * Records a hand-logged transaction — the cash half of the product (screen
     * 02). [sumber] is always MANUAL here; OTOMATIS rows only ever come from a
     * parser in M3.
     */
    suspend fun catat(
        jenis: Jenis,
        nominal: Long,
        walletId: String,
        keterangan: String,
        catatan: String? = null,
        waktu: Long = time.nowMillis(),
        walletTujuanId: String? = null,
    ): Transaction {
        val now = time.nowMillis()
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            jenis = jenis,
            nominal = nominal,
            waktu = waktu,
            walletId = walletId,
            walletTujuanId = walletTujuanId,
            keterangan = keterangan,
            catatan = catatan?.trim()?.takeIf { it.isNotEmpty() },
            sumber = Sumber.MANUAL,
            createdAt = now,
        )
        transactionDao.insert(transaction.toEntity())
        return transaction
    }

    /**
     * Note-only write (§6.8). Deliberately its own query rather than a full row
     * update: a re-parse rewrites `keterangan`, and the note must survive that
     * untouched. Sharing an update path between the two is how notes get wiped.
     *
     * `editedAt` moves only when the trimmed text actually differs, so opening
     * and closing the sheet without typing does not dirty the row.
     */
    suspend fun setCatatan(id: String, catatan: String?) {
        val current = transactionDao.byId(id) ?: return
        val cleaned = catatan?.trim()?.takeIf { it.isNotEmpty() }
        if (cleaned == current.catatan) return
        transactionDao.updateCatatan(id, cleaned, time.nowMillis())
    }

    /** Soft delete (§6.4). The row stays for the audit trail and stops moving money. */
    suspend fun hapus(id: String) = transactionDao.softDelete(id, time.nowMillis())

    /**
     * Puts the user's five wallets in place on first run.
     *
     * Checked on every start rather than in a Room `onCreate` callback, because
     * the database on the phone was created by M1 and would never fire `onCreate`
     * again — an `onCreate`-only seed would leave the existing install with no
     * wallets and no way to get any until M8's setup wizard.
     *
     * `saldoAwal` is 0 for all five. That is not a placeholder: balance is
     * derived (§6.3), so a wrong opening balance would silently misstate every
     * figure afterwards. Reconcile (06) and Dompet (07) are where the real
     * opening numbers get entered, by the one person who knows them.
     */
    suspend fun seedWalletsIfEmpty() {
        if (walletDao.count() > 0) return
        walletDao.insertAll(defaultWallets.map { it.toEntity() })
    }

    private companion object {
        /**
         * Two GoPay wallets, one package. `com.gojek.gopay` cannot say *which*
         * balance moved, so a captured GoPay notification is inherently ambiguous
         * between these two — M3 has to ask rather than guess. Recorded here
         * because the hint field makes it look decided when it is not.
         */
        val defaultWallets = listOf(
            Wallet(
                id = "bca",
                nama = "BCA",
                jenis = WalletJenis.BANK,
                terhubung = true,
                saldoAwal = 0,
                urutan = 0,
                packageHint = "com.bca",
            ),
            Wallet(
                id = "gopay-1",
                nama = "GoPay 1",
                jenis = WalletJenis.EWALLET,
                terhubung = true,
                saldoAwal = 0,
                urutan = 1,
                packageHint = "com.gojek.gopay",
            ),
            Wallet(
                id = "gopay-2",
                nama = "GoPay 2",
                jenis = WalletJenis.EWALLET,
                terhubung = true,
                saldoAwal = 0,
                urutan = 2,
                packageHint = "com.gojek.gopay",
            ),
            Wallet(
                id = "ovo",
                nama = "OVO",
                jenis = WalletJenis.EWALLET,
                terhubung = true,
                saldoAwal = 0,
                urutan = 3,
                packageHint = "ovo.id",
            ),
            Wallet(
                id = "tunai",
                nama = "Tunai",
                jenis = WalletJenis.CASH,
                terhubung = false,
                saldoAwal = 0,
                urutan = 4,
                packageHint = null,
            ),
        )
    }
}
