package com.nijika21.yourmoney.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nijika21.yourmoney.domain.capture.DiscoveredSource
import com.nijika21.yourmoney.domain.model.CashDayStatus
import com.nijika21.yourmoney.domain.model.DailyCashStatus

@Entity(tableName = "daily_cash_status", indices = [Index(value = ["status"])])
data class DailyCashStatusEntity(
    /** ISO local date, `YYYY-MM-DD`. */
    @PrimaryKey val date: String,
    val status: String,
    val answeredAt: Long? = null,
)

fun DailyCashStatusEntity.toDomain(): DailyCashStatus = DailyCashStatus(
    date = date,
    status = CashDayStatus.valueOf(status),
    answeredAt = answeredAt,
)

fun DailyCashStatus.toEntity(): DailyCashStatusEntity = DailyCashStatusEntity(
    date = date,
    status = status.name,
    answeredAt = answeredAt,
)

@Entity(tableName = "monthly_summary")
data class MonthlySummaryEntity(
    /** `YYYY-MM`. */
    @PrimaryKey val yearMonth: String,
    val payloadJson: String,
    val pdfUri: String? = null,
    val generatedAt: Long,
)

@Entity(tableName = "reconcile_event", indices = [Index(value = ["walletId", "waktu"])])
data class ReconcileEventEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val waktu: Long,
    val saldoPrediksi: Long,
    val saldoAsli: Long,
    val selisih: Long,
    val transactionId: String? = null,
)

/**
 * Package names only, never content — see
 * [com.nijika21.yourmoney.domain.capture.DiscoveryLog]. This is what turns the
 * guessed package list in `SourceRegistry` into verified fact, without
 * breaking the §3.3 promise that non-whitelisted content is never stored.
 */
@Entity(tableName = "discovered_source")
data class DiscoveredSourceEntity(
    @PrimaryKey val packageName: String,
    val count: Long,
    val firstSeen: Long,
    val lastSeen: Long,
)

fun DiscoveredSourceEntity.toDomain(): DiscoveredSource = DiscoveredSource(
    packageName = packageName,
    count = count,
    firstSeen = firstSeen,
    lastSeen = lastSeen,
)
