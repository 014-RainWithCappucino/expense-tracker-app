package com.nijika21.yourmoney.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nijika21.yourmoney.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tx: TransactionEntity)

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("SELECT * FROM `transaction` WHERE id = :id")
    suspend fun byId(id: String): TransactionEntity?

    @Query(
        """
        SELECT * FROM `transaction`
        WHERE deletedAt IS NULL
        ORDER BY waktu DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM `transaction` WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Long>

    /**
     * Counts soft-deleted rows too, deliberately: the seeder uses this to decide
     * whether the ledger has ever been written to, and a deleted row still means
     * it has.
     */
    @Query("SELECT COUNT(*) FROM `transaction`")
    suspend fun count(): Long

    /**
     * Half-open window, `[from, until)` — see `DayWindow`. An inclusive upper
     * bound needs a "23:59:59.999" and then a midnight transaction lands in two
     * days or neither.
     */
    @Query(
        """
        SELECT * FROM `transaction`
        WHERE deletedAt IS NULL AND waktu >= :from AND waktu < :until
        ORDER BY waktu DESC
        """,
    )
    fun observeBetween(from: Long, until: Long): Flow<List<TransactionEntity>>

    /** Drives the detail sheet, so an edit made inside it is reflected live. */
    @Query("SELECT * FROM `transaction` WHERE id = :id")
    fun observeById(id: String): Flow<TransactionEntity?>

    /**
     * Soft delete (§6.4). Nothing in the ledger is ever physically removed —
     * corrections are a first-class concept and the audit trail is the point.
     */
    @Query("UPDATE `transaction` SET deletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    /** The note is user-owned; a parser re-run must never touch it (§6.8). */
    @Query("UPDATE `transaction` SET catatan = :catatan, editedAt = :now WHERE id = :id")
    suspend fun updateCatatan(id: String, catatan: String?, now: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM `transaction` WHERE rawNotificationId = :rawId)")
    suspend fun existsForRaw(rawId: String): Boolean
}
