package com.nijika21.yourmoney.domain.model

enum class Sumber { OTOMATIS, MANUAL }

enum class MatchState { PENDING, MATCHED, UNMATCHED, DUPLICATE }

/**
 * One ledger row.
 *
 * [nominal] is **always non-negative** (TDD §2.3). Direction lives in [jenis],
 * never in the sign — that removes a whole class of sign-flip bugs from the
 * totals code. Whole rupiah as [Long]; never Double, Float or BigDecimal.
 */
data class Transaction(
    val id: String,
    val jenis: Jenis,
    val nominal: Long,
    /** Epoch millis. */
    val waktu: Long,
    val walletId: String,
    val walletTujuanId: String? = null,
    /**
     * Derived, not user-owned: merchant name from the parser, or the cash
     * label. A parser re-run is allowed to rewrite this (§6.8).
     */
    val keterangan: String,
    /**
     * The user's own free note. A parser re-run must never touch it (§6.8) —
     * this is the whole reason it is a separate column from [keterangan].
     */
    val catatan: String? = null,
    val sumber: Sumber,
    val parserId: String? = null,
    val parserVersion: Int? = null,
    val rawNotificationId: String? = null,
    val matchState: MatchState? = null,
    /** Links a transfer to its admin-fee row (§3.5). */
    val groupId: String? = null,
    val createdAt: Long,
    val editedAt: Long? = null,
    /** Soft delete, everywhere (§6.4). Nothing here is ever hard-deleted. */
    val deletedAt: Long? = null,
) {
    init {
        require(nominal >= 0) { "nominal must be non-negative, was $nominal" }
    }

    val isDeleted: Boolean get() = deletedAt != null
}
