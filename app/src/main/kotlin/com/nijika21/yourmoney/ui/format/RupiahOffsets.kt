package com.nijika21.yourmoney.ui.format

/**
 * Maps between a raw digit string (`205000`) and its formatted form (`Rp205.000`).
 *
 * The amount on screen 02 carries a caret, so a tap somewhere in `Rp205.000` has
 * to become an index into `205000`, and that index has to become an x position
 * again to draw the caret. Both directions have to agree exactly or the caret
 * lands one place off every time it crosses a separator.
 *
 * Pure arithmetic, kept out of the composable so it can be tested — this is
 * precisely the kind of off-by-one that only shows up on a real tap.
 */
object RupiahOffsets {

    const val PREFIX_LENGTH = 2 // "Rp"

    /**
     * Separators sitting before digit [rawIndex], given [digitCount] digits in
     * total. A separator precedes digit `j` when `j` is not the first and the
     * digits after it are a multiple of three — the rule `Rupiah.grouped` uses.
     */
    fun separatorsBefore(rawIndex: Int, digitCount: Int): Int {
        var count = 0
        // Never `j == digitCount`: that would be a dot after the last digit.
        for (j in 1..minOf(rawIndex, digitCount - 1)) {
            if ((digitCount - j) % 3 == 0) count++
        }
        return count
    }

    /** Index in the formatted string where the caret for [rawIndex] belongs. */
    fun rawToFormatted(rawIndex: Int, digitCount: Int): Int {
        val clamped = rawIndex.coerceIn(0, digitCount)
        return PREFIX_LENGTH + clamped + separatorsBefore(clamped, digitCount)
    }

    /** Digit index nearest a tap that landed at [formattedIndex]. */
    fun formattedToRaw(formattedIndex: Int, digitCount: Int): Int {
        if (formattedIndex <= PREFIX_LENGTH) return 0
        for (i in 0..digitCount) {
            if (rawToFormatted(i, digitCount) >= formattedIndex) return i
        }
        return digitCount
    }
}
