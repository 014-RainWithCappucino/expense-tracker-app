package com.nijika21.yourmoney.domain.ledger

import com.nijika21.yourmoney.domain.model.Transaction

/**
 * Headline figures for a period — a day on Home, a month in the summary.
 *
 * The exclusions are the whole point (§6.3):
 *
 * - **`PINDAH_DOMPET` appears in neither total.** Moving your own money from BCA
 *   to GoPay is not spending, and counting it would inflate "keluar" by every
 *   topup — the single most common transaction this user makes.
 * - **`KOREKSI_*` is its own line, not folded into masuk/keluar.** Reconcile
 *   drift is real money, so it has to show up somewhere; burying it inside the
 *   expense figure would quietly make "12% lebih hemat dari Juli" a lie.
 */
data class PeriodTotals(
    val masuk: Long,
    val keluar: Long,
    /** Signed: `KOREKSI_NAIK` adds, `KOREKSI_TURUN` subtracts. */
    val koreksi: Long,
    /** Rows counted, transfers and corrections included. Drives empty states. */
    val jumlahTransaksi: Int,
) {
    /** What the period did to the total balance, corrections included. */
    val net: Long get() = masuk - keluar + koreksi

    val kosong: Boolean get() = jumlahTransaksi == 0

    companion object {
        val EMPTY = PeriodTotals(masuk = 0, keluar = 0, koreksi = 0, jumlahTransaksi = 0)

        /**
         * Soft-deleted rows are skipped here too — a deleted transaction must
         * leave no trace in any figure the user reads, only in the audit trail.
         */
        fun of(transactions: List<Transaction>): PeriodTotals {
            var masuk = 0L
            var keluar = 0L
            var koreksi = 0L
            var counted = 0

            for (tx in transactions) {
                if (tx.isDeleted) continue
                counted++
                when {
                    tx.jenis.countsAsMasuk -> masuk += tx.nominal
                    tx.jenis.countsAsKeluar -> keluar += tx.nominal
                    tx.jenis.isKoreksi -> koreksi += tx.jenis.effectOnSource(tx.nominal)
                    // PINDAH_DOMPET: counted as a row, absent from every total.
                }
            }

            return PeriodTotals(
                masuk = masuk,
                keluar = keluar,
                koreksi = koreksi,
                jumlahTransaksi = counted,
            )
        }
    }
}
