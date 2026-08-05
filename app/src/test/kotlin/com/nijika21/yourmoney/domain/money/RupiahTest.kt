package com.nijika21.yourmoney.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

class RupiahTest {

    @Test
    fun `groups thousands with dots`() {
        assertEquals("0", Rupiah.grouped(0))
        assertEquals("1", Rupiah.grouped(1))
        assertEquals("999", Rupiah.grouped(999))
        assertEquals("1.000", Rupiah.grouped(1_000))
        assertEquals("45.000", Rupiah.grouped(45_000))
        assertEquals("100.000", Rupiah.grouped(100_000))
        assertEquals("1.234.567", Rupiah.grouped(1_234_567))
    }

    @Test
    fun `keeps the sign outside the digits`() {
        assertEquals("-45.000", Rupiah.grouped(-45_000))
        assertEquals("-1", Rupiah.grouped(-1))
    }

    @Test
    fun `format prefixes Rp without a space`() {
        assertEquals("Rp45.000", Rupiah.format(45_000))
        assertEquals("Rp0", Rupiah.format(0))
    }

    @Test
    fun `signed always shows a sign except for zero`() {
        assertEquals("+Rp2.500", Rupiah.signed(2_500))
        assertEquals("-Rp2.500", Rupiah.signed(-2_500))
        assertEquals("Rp0", Rupiah.signed(0))
    }

    /**
     * `-Long.MIN_VALUE` overflows back to itself. Reconcile drift is the one
     * place a negative reaches this code, so the guard is not theoretical
     * enough to skip.
     */
    @Test
    fun `does not overflow on Long MIN_VALUE`() {
        assertEquals("-9.223.372.036.854.775.808", Rupiah.grouped(Long.MIN_VALUE))
    }

    @Test
    fun `grouping boundary is every third digit from the right`() {
        // Regression guard: an off-by-one here puts the dot in "4.5000".
        assertEquals("4.500", Rupiah.grouped(4_500))
        assertEquals("45.500", Rupiah.grouped(45_500))
        assertEquals("450.500", Rupiah.grouped(450_500))
    }
}
