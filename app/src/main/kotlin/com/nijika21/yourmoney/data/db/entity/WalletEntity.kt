package com.nijika21.yourmoney.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nijika21.yourmoney.domain.model.Wallet
import com.nijika21.yourmoney.domain.model.WalletJenis

@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey val id: String,
    val nama: String,
    val jenis: String,
    val terhubung: Boolean,
    val saldoAwal: Long,
    val urutan: Int,
    @ColumnInfo(defaultValue = "NULL") val packageHint: String? = null,
)

fun WalletEntity.toDomain(): Wallet = Wallet(
    id = id,
    nama = nama,
    jenis = WalletJenis.valueOf(jenis),
    terhubung = terhubung,
    saldoAwal = saldoAwal,
    urutan = urutan,
    packageHint = packageHint,
)

fun Wallet.toEntity(): WalletEntity = WalletEntity(
    id = id,
    nama = nama,
    jenis = jenis.name,
    terhubung = terhubung,
    saldoAwal = saldoAwal,
    urutan = urutan,
    packageHint = packageHint,
)
