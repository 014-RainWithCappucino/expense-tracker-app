package com.nijika21.yourmoney.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.nijika21.yourmoney.data.db.entity.DiscoveredSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveredSourceDao {

    /**
     * One statement, no read-modify-write: the listener callback can fire from
     * several notifications at once and a lost update here would undercount.
     */
    @Query(
        """
        INSERT INTO discovered_source (packageName, count, firstSeen, lastSeen)
        VALUES (:packageName, 1, :seenAt, :seenAt)
        ON CONFLICT(packageName) DO UPDATE SET
            count = count + 1,
            lastSeen = :seenAt
        """,
    )
    suspend fun record(packageName: String, seenAt: Long)

    @Query("SELECT * FROM discovered_source ORDER BY count DESC, lastSeen DESC")
    fun observeAll(): Flow<List<DiscoveredSourceEntity>>

    @Query("DELETE FROM discovered_source")
    suspend fun clear()
}
