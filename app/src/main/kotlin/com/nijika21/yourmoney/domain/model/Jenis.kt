package com.nijika21.yourmoney.domain.model

/**
 * Five values, not three (TDD §6.3). Reconciliation needs a fourth concept,
 * and splitting correction by direction is what keeps [Transaction.nominal]
 * unsigned and the balance SQL trivial.
 *
 * `KOREKSI_*` moves the balance but stays out of the headline Masuk/Keluar
 * figures (decided, §13 item 2) so accumulated drift stays visible as its own
 * line instead of hiding inside the expense number.
 */
enum class Jenis {
    MASUK,
    KELUAR,
    PINDAH_DOMPET,
    KOREKSI_NAIK,
    KOREKSI_TURUN,
    ;

    /** Signed effect on the *source* wallet ([Transaction.walletId]). */
    fun effectOnSource(nominal: Long): Long = when (this) {
        MASUK, KOREKSI_NAIK -> nominal
        KELUAR, KOREKSI_TURUN, PINDAH_DOMPET -> -nominal
    }

    /** Signed effect on the *destination* wallet, if there is one. */
    fun effectOnDestination(nominal: Long): Long = when (this) {
        PINDAH_DOMPET -> nominal
        else -> 0L
    }

    val countsAsMasuk: Boolean get() = this == MASUK
    val countsAsKeluar: Boolean get() = this == KELUAR
    val isKoreksi: Boolean get() = this == KOREKSI_NAIK || this == KOREKSI_TURUN
    val movesBetweenWallets: Boolean get() = this == PINDAH_DOMPET
}
