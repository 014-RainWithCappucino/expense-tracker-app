package com.nijika21.yourmoney.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nijika21.yourmoney.data.db.dao.DailyCashStatusDao
import com.nijika21.yourmoney.data.db.dao.DiscoveredSourceDao
import com.nijika21.yourmoney.data.db.dao.RawNotificationDao
import com.nijika21.yourmoney.data.db.dao.TransactionDao
import com.nijika21.yourmoney.data.db.dao.WalletDao
import com.nijika21.yourmoney.data.db.entity.DailyCashStatusEntity
import com.nijika21.yourmoney.data.db.entity.DiscoveredSourceEntity
import com.nijika21.yourmoney.data.db.entity.MonthlySummaryEntity
import com.nijika21.yourmoney.data.db.entity.RawNotificationEntity
import com.nijika21.yourmoney.data.db.entity.ReconcileEventEntity
import com.nijika21.yourmoney.data.db.entity.TransactionEntity
import com.nijika21.yourmoney.data.db.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        RawNotificationEntity::class,
        DailyCashStatusEntity::class,
        MonthlySummaryEntity::class,
        ReconcileEventEntity::class,
        DiscoveredSourceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class YourMoneyDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun dailyCashStatusDao(): DailyCashStatusDao
    abstract fun discoveredSourceDao(): DiscoveredSourceDao

    companion object {
        const val NAME = "yourmoney.db"
    }
}
