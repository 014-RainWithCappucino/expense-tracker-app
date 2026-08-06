package com.nijika21.yourmoney.domain.capture

/**
 * The capture whitelist — the privacy boundary from TDD §3.3.
 *
 * A notification's package is checked against this *before anything is
 * written*. Only these packages ever have their content persisted, even
 * though the listener permission technically exposes the whole stream. This
 * is what the setup screen promises the user, so it is a correctness
 * requirement, not manners.
 *
 * `verified` means *this exact package was seen delivering a real transaction
 * notification on the device*, not "we are fairly sure". Unverified entries are
 * still captured — a candidate that never fires costs nothing, while a missing
 * one loses transactions silently, which is the failure §3.4 warns about.
 */
object SourceRegistry {

    data class Candidate(
        val packageName: String,
        val label: String,
        /** Confirmed on hardware, with the date and the wording in [note]. */
        val verified: Boolean = false,
        val note: String? = null,
    )

    val candidates: List<Candidate> = listOf(
        Candidate(
            packageName = "com.bca",
            label = "BCA (mobile)",
            verified = true,
            note = "2026-08-06: \"Financial Diary: Pemasukan sebesar IDR 8,127.00 …\", " +
                "channel fcm_fallback_notification_channel, BigTextStyle.",
        ),
        Candidate("com.bca.mybca.omni.android", "myBCA"),
        Candidate("com.gojek.app", "Gojek / GoPay"),
        Candidate(
            packageName = "com.gojek.gopay",
            label = "GoPay (standalone)",
            verified = true,
            note = "2026-08-06: \"Transfer berhasil\" / \"Pembayaran berhasil!\", " +
                "channel promotional_notifications — the channel is useless for " +
                "filtering, receipts and ads share it.",
        ),
        Candidate("ovo.id", "OVO"),
        // Both, because the receipt can come from either: the standalone wallet
        // app or the Shopee app it is embedded in. An entry that never fires
        // costs nothing; the missing one is what loses transactions.
        Candidate("com.shopeepay.id", "ShopeePay"),
        Candidate("com.shopee.id", "Shopee (ShopeePay)"),
    )

    private val packages: Set<String> = candidates.mapTo(mutableSetOf()) { it.packageName }

    /** True when this package's notification content may be stored. */
    fun isCaptured(packageName: String): Boolean = packageName in packages

    fun labelFor(packageName: String): String? =
        candidates.firstOrNull { it.packageName == packageName }?.label
}
