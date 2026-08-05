package com.nijika21.yourmoney.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nijika21.yourmoney.data.db.entity.DailyCashStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCashStatusDao {

    @Upsert
    suspend fun upsert(row: DailyCashStatusEntity)

    @Query("SELECT * FROM daily_cash_status WHERE date = :date")
    suspend fun byDate(date: String): DailyCashStatusEntity?

    @Query("SELECT * FROM daily_cash_status WHERE status = :status ORDER BY date ASC")
    fun observeByStatus(status: String): Flow<List<DailyCashStatusEntity>>

    @Query("SELECT * FROM daily_cash_status ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyCashStatusEntity>>
}
