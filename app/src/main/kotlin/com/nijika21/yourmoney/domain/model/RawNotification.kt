package com.nijika21.yourmoney.domain.model

enum class ParseState {
    /** Not yet looked at by the registry. */
    BARU,

    /** A parser produced a transaction. */
    PARSED,

    /** Recognised and deliberately dropped — promo, OTP, balance info. */
    IGNORED,

    /** No parser claimed it. This is the work queue the diagnostics screen shows. */
    UNRECOGNIZED,

    /** Same notification re-posted, or same content hash (§3.6). */
    DUPLICATE,
}

/**
 * A captured notification, stored **before** any parsing is attempted (§3.3).
 *
 * This table is the parser corpus. Parsers will be wrong, and banks change
 * wording after app updates; with raws kept, fixing a parser is a version bump
 * plus a re-run over history. Without them, every parser bug is permanent
 * data loss.
 */
data class RawNotification(
    val id: String,
    val packageName: String,
    /** `StatusBarNotification.key` — used for the re-post case in §3.6. */
    val sbnKey: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    /** Epoch millis, from the notification itself. */
    val postTime: Long,
    /** Hash over the semantic content, for dedup. */
    val contentHash: String,
    val parseState: ParseState,
    val parserId: String? = null,
    /** Epoch millis, when we stored it. */
    val receivedAt: Long,
)
