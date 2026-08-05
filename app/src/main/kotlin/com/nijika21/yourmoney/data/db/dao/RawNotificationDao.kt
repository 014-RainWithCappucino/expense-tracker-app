package com.nijika21.yourmoney.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nijika21.yourmoney.data.db.entity.RawNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(raw: RawNotificationEntity): Long

    /** §3.6: same sbn key + identical content is a re-post, not a new event. */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM raw_notification
            WHERE sbnKey = :sbnKey AND contentHash = :contentHash
        )
        """,
    )
    suspend fun existsBySbnKeyAndHash(sbnKey: String, contentHash: String): Boolean

    @Query("SELECT * FROM raw_notification ORDER BY postTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RawNotificationEntity>>

    @Query(
        """
        SELECT * FROM raw_notification
        WHERE parseState = :state
        ORDER BY postTime DESC
        LIMIT :limit
        """,
    )
    fun observeByState(state: String, limit: Int): Flow<List<RawNotificationEntity>>

    @Query("SELECT COUNT(*) FROM raw_notification")
    fun observeCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM raw_notification WHERE packageName = :packageName")
    suspend fun countForPackage(packageName: String): Long

    @Query("SELECT * FROM raw_notification WHERE id = :id")
    suspend fun byId(id: String): RawNotificationEntity?

    @Query("UPDATE raw_notification SET parseState = :state, parserId = :parserId WHERE id = :id")
    suspend fun markState(id: String, state: String, parserId: String?)

    /** Retention purge (§3.3). Raws are the only rows ever hard-deleted. */
    @Query("DELETE FROM raw_notification WHERE receivedAt < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long): Int
}
