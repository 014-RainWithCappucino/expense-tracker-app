package com.nijika21.yourmoney.domain.money

/**
 * Rupiah formatting. Pure Kotlin — no `NumberFormat`, no locale lookup, so it
 * is deterministic in unit tests and identical on every OEM skin.
 *
 * IDR has no circulating subunit, so there is never a decimal part.
 */
object Rupiah {

    /** `45000` → `"45.000"`. Negative input keeps its sign: `"-45.000"`. */
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

    /** `45000` → `"Rp45.000"`. */
    fun format(amount: Long): String = "Rp" + grouped(amount)

    /**
     * `45000` → `"+Rp45.000"` / `"-Rp45.000"`. Used only where the sign is the
     * point (reconcile drift); ledger rows carry direction in `jenis` instead.
     */
    fun signed(amount: Long): String = when {
        amount > 0 -> "+" + format(amount)
        amount < 0 -> "-" + format(-amount)
        else -> format(0)
    }
}
