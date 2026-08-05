package com.nijika21.yourmoney.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The table in TDD §6.3, asserted. These five rows are what every balance and
 * every headline total depend on, so they are pinned rather than trusted.
 */
class JenisTest {

    @Test
    fun `effect on the source wallet matches the spec table`() {
        assertEquals(1_000L, Jenis.MASUK.effectOnSource(1_000))
        assertEquals(-1_000L, Jenis.KELUAR.effectOnSource(1_000))
        assertEquals(-1_000L, Jenis.PINDAH_DOMPET.effectOnSource(1_000))
        assertEquals(1_000L, Jenis.KOREKSI_NAIK.effectOnSource(1_000))
        assertEquals(-1_000L, Jenis.KOREKSI_TURUN.effectOnSource(1_000))
    }

    @Test
    fun `only a transfer credits a destination wallet`() {
        assertEquals(1_000L, Jenis.PINDAH_DOMPET.effectOnDestination(1_000))
        for (jenis in Jenis.entries - Jenis.PINDAH_DOMPET) {
            assertEquals(
                "$jenis must not credit a destination",
                0L,
                jenis.effectOnDestination(1_000),
            )
        }
    }

    @Test
    fun `a transfer is balance-neutral across both wallets`() {
        val n = 250_000L
        val net = Jenis.PINDAH_DOMPET.effectOnSource(n) + Jenis.PINDAH_DOMPET.effectOnDestination(n)
        assertEquals(0L, net)
    }

    /**
     * §13 item 2: KOREKSI moves the balance but stays out of the headline
     * figures, so "12% lebih hemat dari Juli" keeps meaning something.
     */
    @Test
    fun `koreksi is excluded from both headline totals`() {
        for (jenis in listOf(Jenis.KOREKSI_NAIK, Jenis.KOREKSI_TURUN)) {
            assertFalse(jenis.countsAsMasuk)
            assertFalse(jenis.countsAsKeluar)
            assertTrue(jenis.isKoreksi)
        }
    }

    @Test
    fun `transfers are excluded from both headline totals`() {
        assertFalse(Jenis.PINDAH_DOMPET.countsAsMasuk)
        assertFalse(Jenis.PINDAH_DOMPET.countsAsKeluar)
    }

    @Test
    fun `exactly one jenis counts as masuk and one as keluar`() {
        assertEquals(listOf(Jenis.MASUK), Jenis.entries.filter { it.countsAsMasuk })
        assertEquals(listOf(Jenis.KELUAR), Jenis.entries.filter { it.countsAsKeluar })
    }
}
