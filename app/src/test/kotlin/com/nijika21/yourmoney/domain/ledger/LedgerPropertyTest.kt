package com.nijika21.yourmoney.domain.ledger

import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.domain.model.Sumber
import com.nijika21.yourmoney.domain.model.Transaction
import com.nijika21.yourmoney.domain.model.Wallet
import com.nijika21.yourmoney.domain.model.WalletJenis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The ledger invariants from TDD §11, asserted over generated transaction sets
 * rather than hand-picked examples. Example tests pass a `PINDAH_DOMPET`
 * regression happily — you write one transfer, check both wallets, and never hit
 * the case where a transfer's destination was also somebody else's source.
 *
 * Generators are a seeded [Random] loop, not a property-testing framework. The
 * three invariants below need shrinking about as much as they need a DSL, and
 * the seed is printed on failure, which is the only part of shrinking that
 * actually saves time here.
 */
class LedgerPropertyTest {

    private val wallets = listOf(
        Wallet("bca", "BCA", WalletJenis.BANK, terhubung = true, saldoAwal = 2_500_000, urutan = 0),
        Wallet("gopay", "GoPay", WalletJenis.EWALLET, terhubung = true, saldoAwal = 150_000, urutan = 1),
        Wallet("ovo", "OVO", WalletJenis.EWALLET, terhubung = true, saldoAwal = 75_000, urutan = 2),
        Wallet("tunai", "Tunai", WalletJenis.CASH, terhubung = false, saldoAwal = 300_000, urutan = 3),
    )

    private fun generate(seed: Long, count: Int): List<Transaction> {
        val random = Random(seed)
        val ids = wallets.map { it.id }

        return (0 until count).map { i ->
            val jenis = Jenis.entries[random.nextInt(Jenis.entries.size)]
            val source = ids[random.nextInt(ids.size)]
            // A transfer to itself would be trivially neutral and would hide the
            // very bug this test exists for, so the destination is forced apart.
            val destination = if (jenis.movesBetweenWallets) {
                ids.filterNot { it == source }[random.nextInt(ids.size - 1)]
            } else {
                null
            }

            Transaction(
                id = "tx-$i",
                jenis = jenis,
                nominal = random.nextLong(0, 5_000_000),
                waktu = 1_770_000_000_000L + i * 60_000L,
                walletId = source,
                walletTujuanId = destination,
                keterangan = "generated-$i",
                sumber = if (random.nextBoolean()) Sumber.OTOMATIS else Sumber.MANUAL,
                createdAt = 1_770_000_000_000L,
                // Roughly one row in eight is soft-deleted: deleted rows must
                // move no money and appear in no total (§6.4).
                deletedAt = if (random.nextInt(8) == 0) 1_770_100_000_000L else null,
            )
        }
    }

    private val seeds = listOf(1L, 7L, 42L, 1_337L, 20_260_805L, Long.MAX_VALUE / 3)

    @Test
    fun `PINDAH_DOMPET is balance-neutral across the wallet set`() {
        for (seed in seeds) {
            val transfersOnly = generate(seed, 400).filter { it.jenis.movesBetweenWallets }
            val before = wallets.sumOf { it.saldoAwal }
            val after = BalanceCalculator.totalBalance(wallets, transfersOnly)

            assertEquals(
                "seed=$seed: transfers moved the total, which means one side was dropped",
                before,
                after,
            )
        }
    }

    @Test
    fun `sum of balances equals saldoAwal plus net masuk keluar koreksi`() {
        for (seed in seeds) {
            val transactions = generate(seed, 400)
            val live = transactions.filterNot { it.isDeleted }

            val expected = wallets.sumOf { it.saldoAwal } +
                live.filter { it.jenis.countsAsMasuk }.sumOf { it.nominal } -
                live.filter { it.jenis.countsAsKeluar }.sumOf { it.nominal } +
                live.filter { it.jenis.isKoreksi }.sumOf { it.jenis.effectOnSource(it.nominal) }

            assertEquals(
                "seed=$seed: total balance drifted from the effect table in §6.3",
                expected,
                BalanceCalculator.totalBalance(wallets, transactions),
            )
        }
    }

    @Test
    fun `period totals never include PINDAH_DOMPET`() {
        for (seed in seeds) {
            val transactions = generate(seed, 400)
            val totals = PeriodTotals.of(transactions)
            val withoutTransfers = PeriodTotals.of(
                transactions.filterNot { it.jenis.movesBetweenWallets },
            )

            assertEquals("seed=$seed", totals.masuk, withoutTransfers.masuk)
            assertEquals("seed=$seed", totals.keluar, withoutTransfers.keluar)
            assertEquals("seed=$seed", totals.koreksi, withoutTransfers.koreksi)
        }
    }

    @Test
    fun `soft-deleted rows move no money and land in no total`() {
        for (seed in seeds) {
            val transactions = generate(seed, 400)
            val live = transactions.filterNot { it.isDeleted }

            assertEquals(
                "seed=$seed: a deleted row still moved a balance",
                BalanceCalculator.balances(wallets, live),
                BalanceCalculator.balances(wallets, transactions),
            )
            assertEquals(
                "seed=$seed: a deleted row still showed up in a total",
                PeriodTotals.of(live),
                PeriodTotals.of(transactions),
            )
            assertTrue(
                "seed=$seed: the generator produced no deleted rows, so this proved nothing",
                transactions.any { it.isDeleted },
            )
        }
    }

    @Test
    fun `masuk and keluar totals stay non-negative`() {
        // nominal is unsigned by construction (§2.3); this guards the day someone
        // "simplifies" the effect table by letting a sign into the totals.
        for (seed in seeds) {
            val totals = PeriodTotals.of(generate(seed, 400))
            assertTrue("seed=$seed", totals.masuk >= 0)
            assertTrue("seed=$seed", totals.keluar >= 0)
        }
    }
}
