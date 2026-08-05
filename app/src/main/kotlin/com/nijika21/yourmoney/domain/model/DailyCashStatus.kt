package com.nijika21.yourmoney.domain.model

enum class CashDayStatus {
    /** Asked, not yet answered. The reminder keeps nagging while any day is here. */
    MENUNGGU,

    /** User logged cash spending for the day. */
    TERCATAT,

    /** User confirmed they spent no cash. */
    NOL,

    /** Auto-closed after 7 days without an answer (§3.7). */
    TIDAK_TERCATAT,
}

data class DailyCashStatus(
    /** ISO local date, `YYYY-MM-DD`. */
    val date: String,
    val status: CashDayStatus,
    val answeredAt: Long? = null,
)
