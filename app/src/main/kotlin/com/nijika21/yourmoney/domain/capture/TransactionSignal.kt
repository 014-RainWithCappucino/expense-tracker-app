package com.nijika21.yourmoney.domain.capture

/**
 * How likely one captured notification is to be an actual money movement.
 *
 * Three values, not two, on purpose: a binary verdict forces every unfamiliar
 * wording into one of the wrong buckets, and new wordings are guaranteed —
 * banks and e-wallets rewrite their copy without warning.
 */
enum class CaptureSignal {
    /** Carries an amount and a completion phrase. Worth a parser. */
    TRANSAKSI,

    /** Carries an amount, but nothing says a transaction completed. Human's call. */
    MUNGKIN,

    /** No amount at all, or an amount inside promotional copy. */
    BUKAN,
}

/**
 * Separates real transaction notifications from the surrounding noise.
 *
 * Both whitelisted sources send far more marketing than transactions, and the
 * marketing is written to look urgent and specific — *"Cihuy, kamu dapet 26
 * GoPay Coins!"* has a number, a brand, and an exclamation mark, and means
 * nothing financially. The diagnostics list is unreadable without this, and in
 * M3 the same call decides whether a parser is even attempted.
 *
 * **This never gates storage.** Every whitelisted notification is persisted
 * verbatim regardless of the verdict here (§3.3): the classifier is a heuristic,
 * heuristics are wrong, and a wrong verdict at capture time would be permanent
 * data loss. It only ever reorders what a human reads.
 *
 * Every rule below is derived from notifications actually captured on the
 * device, not from guessed formats — see `src/test/resources/notifications/`.
 */
object TransactionSignal {

    /**
     * An amount with a currency marker attached. The marker is what makes this
     * safe: *"26 GoPay Coins"* is a bare number and must not read as money,
     * while `Rp8.127` and `IDR 8,127.00` — the two shapes the two sources
     * actually use — both do.
     *
     * Separators are deliberately loose (`.` and `,` both, in either role):
     * GoPay writes `Rp26.374`, BCA writes `IDR 8,127.00`, and normalising that
     * mess is the parser's job in M3, not this filter's.
     */
    private val amountPattern =
        Regex("""(?:rp|idr)\s*\d[\d.,]*""", RegexOption.IGNORE_CASE)

    /**
     * Phrases that say a movement completed. Past tense or a result — never an
     * offer. Taken from the real corpus: GoPay's "Transfer berhasil" and
     * "kamu barusan bayar", BCA's "Financial Diary: Pemasukan sebesar".
     */
    private val completionPhrases = listOf(
        "berhasil",
        "sukses",
        "dikirim",
        "diterima",
        "barusan bayar",
        "pembayaran",
        "pemasukan",
        "pengeluaran",
        "financial diary",
        "transfer",
        "top up",
        "topup",
        "tarik",
        "setor",
        "debit",
        "kredit",
        "saldo",
    )

    /**
     * Marketing vocabulary. These veto a match even when an amount is present,
     * because *"diskon sampai Rp50.000"* is an offer, not a transaction.
     *
     * `cashback`, `coins` and `poin` are vetoed rather than trusted: a real
     * cashback credit arrives as a saldo/pemasukan notification with its own
     * wording, so treating the promotional version as money would double-count
     * a reward that was never in the ledger.
     */
    private val promoWords = listOf(
        "coins",
        "coin",
        "poin",
        "promo",
        "voucher",
        "kupon",
        "diskon",
        "cashback",
        "gratis",
        "undian",
        "menang",
        "hadiah",
        "klik buat",
        "klik di sini",
        "yuk ",
        "buruan",
        "jangan sampai",
        "berlaku sampai",
        "penawaran",
    )

    fun classify(title: String?, text: String?, bigText: String?): CaptureSignal {
        val haystack = listOfNotNull(title, text, bigText)
            .joinToString(separator = " ")
            .lowercase()

        if (!amountPattern.containsMatchIn(haystack)) return CaptureSignal.BUKAN

        // Completion wins over the promo veto, and the order matters: GoPay
        // appends reward copy to genuine receipts ("Pembayaran berhasil! ...
        // kamu dapet 10 poin"), so vetoing on a promo word first would drop real
        // transactions. The errors are not symmetric — a false positive costs a
        // glance at the M3 queue, a false negative loses a transaction silently.
        if (completionPhrases.any { it in haystack }) return CaptureSignal.TRANSAKSI
        if (promoWords.any { it in haystack }) return CaptureSignal.BUKAN

        return CaptureSignal.MUNGKIN
    }
}
