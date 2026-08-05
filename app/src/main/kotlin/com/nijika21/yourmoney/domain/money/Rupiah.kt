package com.nijika21.yourmoney.domain.money

/**
 * Rupiah formatting. Pure Kotlin — no `NumberFormat`, no locale lookup, so it
 * is deterministic in unit tests and identical on every OEM skin.
 *
 * IDR has no circulating subunit, so there is never a decimal part.
 */
object Rupiah {

    /**
     * U+2212 MINUS SIGN, not the ASCII hyphen (§4). The hyphen is narrower than
     * a digit even in a `tnum` face, so a column of negatives sits a hair off
     * the ones above it — which is exactly what the design's tabular figures
     * exist to prevent.
     */
    const val MINUS: Char = '−'

    /** Marks a *predicted* balance. Honest by construction: balance is derived. */
    const val APPROX: Char = '≈'

    /**
     * `45000` → `"45.000"`. Digits and separators only; a negative keeps the
     * ASCII `-` here because this is also the CSV/backup shape (§9), where a
     * typographic minus would be a parsing hazard on the laptop. Display code
     * goes through [format], [signed] or [approx] instead.
     */
    fun grouped(amount: Long): String {
        val negative = amount < 0
        // Guard Long.MIN_VALUE, where -amount overflows back to itself.
        val digits = if (amount == Long.MIN_VALUE) {
            Long.MIN_VALUE.toString().removePrefix("-")
        } else {
            (if (negative) -amount else amount).toString()
        }

        val out = StringBuilder(digits.length + digits.length / 3 + 1)
        if (negative) out.append('-')
        for ((i, c) in digits.withIndex()) {
            if (i > 0 && (digits.length - i) % 3 == 0) out.append('.')
            out.append(c)
        }
        return out.toString()
    }

    /** `45000` → `"Rp45.000"`, `-45000` → `"−Rp45.000"`. */
    fun format(amount: Long): String =
        if (amount < 0) MINUS + "Rp" + magnitude(amount) else "Rp" + grouped(amount)

    /**
     * `45000` → `"+Rp45.000"` / `"−Rp45.000"`. Used only where the sign is the
     * point (reconcile drift); ledger rows carry direction in `jenis` instead.
     */
    fun signed(amount: Long): String = when {
        amount > 0 -> "+Rp" + grouped(amount)
        amount < 0 -> MINUS + "Rp" + magnitude(amount)
        else -> format(0)
    }

    /**
     * `45000` → `"≈Rp45.000"`. Every wallet balance in the app is a prediction
     * derived from captured notifications (§6.3), so it is shown with the
     * approximation mark until a reconcile confirms it. Dropping the mark would
     * claim a certainty the ledger does not have.
     */
    fun approx(amount: Long): String = APPROX + format(amount)

    /** Digits of `|amount|`, overflow-safe at [Long.MIN_VALUE]. */
    private fun magnitude(amount: Long): String = grouped(amount).removePrefix("-")
}
