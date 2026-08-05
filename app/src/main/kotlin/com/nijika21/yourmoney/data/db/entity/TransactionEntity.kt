package com.nijika21.yourmoney.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.domain.model.MatchState
import com.nijika21.yourmoney.domain.model.Sumber
import com.nijika21.yourmoney.domain.model.Transaction

/**
 * Table name is the SQL reserved word `transaction`, exactly as TDD §6.2
 * specifies. Room quotes it correctly, but any hand-written query must
 * backtick it — see [com.nijika21.yourmoney.data.db.dao.TransactionDao].
 */
@Entity(
    tableName = "transaction",
    indices = [
        Index(value = ["waktu"], orders = [Index.Order.DESC]),
        Index(value = ["walletId", "waktu"]),
        Index(value = ["walletTujuanId"]),
        Index(value = ["jenis"]),
        Index(value = ["groupId"]),
        Index(value = ["rawNotificationId"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val jenis: String,
    val nominal: Long,
    val waktu: Long,
    val walletId: String,
    val walletTujuanId: String? = null,
    val keterangan: String,
    val catatan: String? = null,
    val sumber: String,
    val parserId: String? = null,
    val parserVersion: Int? = null,
    val rawNotificationId: String? = null,
    val matchState: String? = null,
    val groupId: String? = null,
    val createdAt: Long,
    val editedAt: Long? = null,
    val deletedAt: Long? = null,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    jenis = Jenis.valueOf(jenis),
    nominal = nominal,
    waktu = waktu,
    walletId = walletId,
    walletTujuanId = walletTujuanId,
    keterangan = keterangan,
    catatan = catatan,
    sumber = Sumber.valueOf(sumber),
    parserId = parserId,
    parserVersion = parserVersion,
    rawNotificationId = rawNotificationId,
    matchState = matchState?.let { MatchState.valueOf(it) },
    groupId = groupId,
    createdAt = createdAt,
    editedAt = editedAt,
    deletedAt = deletedAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    jenis = jenis.name,
    nominal = nominal,
    waktu = waktu,
    walletId = walletId,
    walletTujuanId = walletTujuanId,
    keterangan = keterangan,
    catatan = catatan,
    sumber = sumber.name,
    parserId = parserId,
    parserVersion = parserVersion,
    rawNotificationId = rawNotificationId,
    matchState = matchState?.name,
    groupId = groupId,
    createdAt = createdAt,
    editedAt = editedAt,
    deletedAt = deletedAt,
)
