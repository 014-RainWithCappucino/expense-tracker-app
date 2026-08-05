package com.nijika21.yourmoney.domain.model

enum class WalletJenis { BANK, EWALLET, CASH }

data class Wallet(
    val id: String,
    val nama: String,
    val jenis: WalletJenis,
    /** Whether this wallet is fed by notification capture, or hand-logged. */
    val terhubung: Boolean,
    val saldoAwal: Long,
    val urutan: Int,
    /** Package name this wallet's notifications arrive from, if any. */
    val packageHint: String? = null,
)

/** A wallet plus its computed balance. Balance is never stored (TDD §6.3). */
data class WalletWithBalance(
    val wallet: Wallet,
    val saldo: Long,
)
