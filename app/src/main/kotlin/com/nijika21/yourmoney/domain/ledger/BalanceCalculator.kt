package com.nijika21.yourmoney.domain.ledger

import com.nijika21.yourmoney.domain.model.Transaction
import com.nijika21.yourmoney.domain.model.Wallet
import com.nijika21.yourmoney.domain.model.WalletWithBalance

/**
 * The balance rule of TDD §6.3, in Kotlin.
 *
 * Production reads balances from the SQL aggregate in `WalletDao`, not from
 * here — §4 makes Room `Flow` the single source of truth, and loading the whole
 * ledger into memory to add it up would scale with history for no gain.
 *
 * This object is the **executable specification of that same rule**, and it has
 * two real jobs:
 *
 *  1. The ledger property tests (§11) bind their invariants to it. Bound to the
 *     SQL instead they would need a device or Robolectric, and the invariants
 *     they protect — `PINDAH_DOMPET` neutrality above all — are the exact bugs
 *     the concept doc warns about.
 *  2. Reconcile (§6.3, M5) needs a predicted balance for an arbitrary
 *     transaction set, which is this function with a filtered input.
 *
 * **The duplication is deliberate but load-bearing:** if the `CASE t.jenis`
 * ladder in `WalletDao.observeWithBalances` and `Jenis.effectOnSource` ever
 * disagree, the app shows one number and reconciles against another. Both sides
 * derive from the one table in §6.3, and neither may be edited alone.
 */
object BalanceCalculator {

    /**
     * `saldoAwal` plus every effect this wallet takes, as source or as transfer
     * destination. Soft-deleted rows are excluded (§6.4) — they stay in the
     * table for the audit trail but must not move money.
     */
    fun balanceOf(wallet: Wallet, transactions: List<Transaction>): Long {
        var saldo = wallet.saldoAwal
        for (tx in transactions) {
            if (tx.isDeleted) continue
            if (tx.walletId == wallet.id) saldo += tx.jenis.effectOnSource(tx.nominal)
            if (tx.walletTujuanId == wallet.id) saldo += tx.jenis.effectOnDestination(tx.nominal)
        }
        return saldo
    }

    /** Balances for a whole wallet set, in the wallets' own display order. */
    fun balances(
        wallets: List<Wallet>,
        transactions: List<Transaction>,
    ): List<WalletWithBalance> = wallets.map { wallet ->
        WalletWithBalance(wallet = wallet, saldo = balanceOf(wallet, transactions))
    }

    /**
     * Total across a wallet set. Kept separate from summing [balances] because
     * the headline figure on Home is this number, and a fold over a mapped list
     * at every recomposition is wasted work.
     */
    fun totalBalance(
        wallets: List<Wallet>,
        transactions: List<Transaction>,
    ): Long = wallets.sumOf { balanceOf(it, transactions) }
}
