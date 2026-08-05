package com.nijika21.yourmoney.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.nijika21.yourmoney.data.db.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

/** Projection for the computed-balance query. Balance is never stored (§6.3). */
data class WalletBalanceRow(
    val id: String,
    val nama: String,
    val jenis: String,
    val terhubung: Boolean,
    val saldoAwal: Long,
    val urutan: Int,
    val packageHint: String?,
    val saldo: Long,
)

@Dao
interface WalletDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(wallets: List<WalletEntity>)

    @Upsert
    suspend fun upsert(wallet: WalletEntity)

    @Query("SELECT * FROM wallet ORDER BY urutan ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallet WHERE id = :id")
    suspend fun byId(id: String): WalletEntity?

    @Query("SELECT COUNT(*) FROM wallet")
    suspend fun count(): Long

    @Query("SELECT id FROM wallet")
    suspend fun allIds(): List<String>

    /**
     * Only ever called while the transaction table is empty — see
     * `LedgerRepository.seedWalletsIfEmpty`. A wallet with history behind it is
     * never deleted; §6.4 makes everything else in this app a soft delete.
     */
    @Query("DELETE FROM wallet")
    suspend fun deleteAll()

    /**
     * The balance rule from TDD §6.3, verbatim. `transaction` is a SQL
     * reserved word, hence the backticks.
     *
     * At a few thousand rows a year this aggregate is sub-millisecond; a
     * cached balance column would be a correctness liability for no gain.
     */
    @Query(
        """
        SELECT w.id, w.nama, w.jenis, w.terhubung, w.saldoAwal, w.urutan, w.packageHint,
          w.saldoAwal
          + IFNULL((SELECT SUM(CASE t.jenis
                WHEN 'MASUK' THEN t.nominal
                WHEN 'KOREKSI_NAIK' THEN t.nominal
                ELSE -t.nominal END)
              FROM `transaction` t
              WHERE t.walletId = w.id AND t.deletedAt IS NULL), 0)
          + IFNULL((SELECT SUM(t.nominal) FROM `transaction` t
              WHERE t.walletTujuanId = w.id
                AND t.jenis = 'PINDAH_DOMPET' AND t.deletedAt IS NULL), 0)
          AS saldo
        FROM wallet w
        ORDER BY w.urutan ASC
        """,
    )
    fun observeWithBalances(): Flow<List<WalletBalanceRow>>
}
