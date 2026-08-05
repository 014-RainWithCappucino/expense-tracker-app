package com.nijika21.yourmoney.di

import android.content.Context
import androidx.room.Room
import com.nijika21.yourmoney.data.db.DatabaseKeyProvider
import com.nijika21.yourmoney.data.db.YourMoneyDatabase
import com.nijika21.yourmoney.data.db.dao.DailyCashStatusDao
import com.nijika21.yourmoney.data.db.dao.DiscoveredSourceDao
import com.nijika21.yourmoney.data.db.dao.RawNotificationDao
import com.nijika21.yourmoney.data.db.dao.TransactionDao
import com.nijika21.yourmoney.data.db.dao.WalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKeyProvider(@ApplicationContext context: Context): DatabaseKeyProvider =
        DatabaseKeyProvider(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): YourMoneyDatabase {
        System.loadLibrary("sqlcipher")

        val passphrase = keyProvider.passphrase()
        return Room.databaseBuilder(context, YourMoneyDatabase::class.java, YourMoneyDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            // No fallbackToDestructiveMigration, ever. This database is the
            // only copy of the ledger; a migration bug must fail loudly rather
            // than silently wipe it.
            .build()
    }

    @Provides fun provideWalletDao(db: YourMoneyDatabase): WalletDao = db.walletDao()

    @Provides fun provideTransactionDao(db: YourMoneyDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideRawNotificationDao(db: YourMoneyDatabase): RawNotificationDao =
        db.rawNotificationDao()

    @Provides
    fun provideDailyCashStatusDao(db: YourMoneyDatabase): DailyCashStatusDao =
        db.dailyCashStatusDao()

    @Provides
    fun provideDiscoveredSourceDao(db: YourMoneyDatabase): DiscoveredSourceDao =
        db.discoveredSourceDao()
}
