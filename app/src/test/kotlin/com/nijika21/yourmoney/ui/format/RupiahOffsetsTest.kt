package com.nijika21.yourmoney.ui.format

import com.nijika21.yourmoney.domain.money.Rupiah
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The caret on screen 02 is drawn from this arithmetic, so an off-by-one here is
 * a caret that lands in the wrong place every time it crosses a thousands
 * separator — the exact failure the hand-built `AmountField` exists to avoid.
 *
 * The load-bearing test is [caret lands on the digit it belongs to]: it does not
 * restate the grouping rule, it checks the offsets against the string
 * `Rupiah.grouped` actually produces. Two implementations of "where do the dots
 * go" that are asserted separately will drift; asserting one against the other
 * means they cannot.
 */
class RupiahOffsetsTest {

    /** `"Rp" + grouped`, which is what `AmountField` paints. */
    private fun formatted(digits: String): String =
        "Rp" + Rupiah.grouped(digits.toLong())

    @Test
    fun `caret lands on the digit it belongs to`() {
        // 1 through 12 digits — 12 is MAX_DIGITS in CatatViewModel.
        for (n in 1..12) {
            // 1234567890... so every position holds a distinguishable digit.
            val digits = (1..n).joinToString("") { ((it % 10)).toString() }
            val text = formatted(digits)

            for (i in digits.indices) {
                val at = RupiahOffsets.rawToFormatted(i, n)
                assertEquals(
                    "digit $i of $digits ($text) should sit at formatted index $at",
                    digits[i],
                    text[at],
                )
            }
            // A caret past the last digit belongs at the very end of the string.
            assertEquals(
                "end caret for $text",
                text.length,
                RupiahOffsets.rawToFormatted(n, n),
            )
        }
    }

    @Test
    fun `every caret position survives a round trip`() {
        for (n in 1..12) {
            for (i in 0..n) {
                val back = RupiahOffsets.formattedToRaw(RupiahOffsets.rawToFormatted(i, n), n)
                assertEquals("round trip lost caret $i of $n digits", i, back)
            }
        }
    }

    /**
     * The user's own scenario, the one that put the caret in the app: typing
     * `100200` instead of `12000` and needing to delete *one* wrong digit rather
     * than everything back to it.
     */
    @Test
    fun `the separator boundary in Rp100 200 is exact in both directions`() {
        val n = 6 // "100200" → "Rp100.200"
        assertEquals("Rp100.200", formatted("100200"))

        //                 R  p  1  0  0  .  2  0  0
        // formatted index 0  1  2  3  4  5  6  7  8
        assertEquals(2, RupiahOffsets.rawToFormatted(0, n)) // before the first 1
        assertEquals(4, RupiahOffsets.rawToFormatted(2, n)) // before the last 0 of "100"
        assertEquals(6, RupiahOffsets.rawToFormatted(3, n)) // after the dot, before the 2
        assertEquals(9, RupiahOffsets.rawToFormatted(6, n)) // past the end

        // A tap on the dot itself resolves to the digit after it, never inside.
        assertEquals(3, RupiahOffsets.formattedToRaw(6, n))
        assertEquals(3, RupiahOffsets.formattedToRaw(5, n))
    }

    @Test
    fun `a tap anywhere in the Rp prefix parks the caret at the first digit`() {
        for (index in 0..RupiahOffsets.PREFIX_LENGTH) {
            assertEquals("tap at $index", 0, RupiahOffsets.formattedToRaw(index, 6))
        }
    }

    @Test
    fun `a tap past the end parks the caret after the last digit`() {
        assertEquals(6, RupiahOffsets.formattedToRaw(999, 6))
    }

    @Test
    fun `raw indices outside the string are clamped rather than trusted`() {
        val n = 3
        assertEquals(RupiahOffsets.rawToFormatted(0, n), RupiahOffsets.rawToFormatted(-5, n))
        assertEquals(RupiahOffsets.rawToFormatted(n, n), RupiahOffsets.rawToFormatted(n + 5, n))
    }

    /**
     * A dot never follows the last digit — `1000` is `1.000`, never `1.000.`.
     * This is the boundary the loop in `separatorsBefore` deliberately excludes.
     */
    @Test
    fun `no separator is counted after the final digit`() {
        for (n in intArrayOf(3, 6, 9, 12)) {
            assertEquals(
                "a group boundary at the end of $n digits must not add a dot",
                RupiahOffsets.separatorsBefore(n - 1, n),
                RupiahOffsets.separatorsBefore(n, n),
            )
        }
    }

    @Test
    fun `separator counts match the grouping actually rendered`() {
        for (n in 1..12) {
            val digits = "1".repeat(n)
            val dots = formatted(digits).count { it == '.' }
            assertEquals(
                "total separators for $n digits",
                dots,
                RupiahOffsets.separatorsBefore(n, n),
            )
        }
    }

    @Test
    fun `an empty amount puts the caret straight after the prefix`() {
        assertEquals(RupiahOffsets.PREFIX_LENGTH, RupiahOffsets.rawToFormatted(0, 0))
        assertEquals(0, RupiahOffsets.formattedToRaw(RupiahOffsets.PREFIX_LENGTH, 0))
    }
}
