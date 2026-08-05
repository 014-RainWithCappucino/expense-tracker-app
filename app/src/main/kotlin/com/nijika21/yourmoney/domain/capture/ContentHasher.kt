package com.nijika21.yourmoney.domain.capture

import java.security.MessageDigest

/**
 * Content hash for the raw-notification dedup case in TDD §3.6: the same
 * notification re-posted or updated by the source app.
 *
 * Deliberately excludes `postTime` and the sbn key — an updated notification
 * keeps its key but may carry a new post time, and it is the *content* being
 * identical that makes it a duplicate.
 */
object ContentHasher {

    /**
     * U+001F (unit separator) as the field delimiter, not a space: it cannot
     * occur in notification text, so `("ab", "c")` and `("a", "bc")` can never
     * hash alike. A space would make exactly that collision possible, and it
     * would surface as silently dropped transactions.
     *
     * Written as a code point rather than a literal so no editor or tool can
     * silently eat the control character.
     */
    private val SEP: Char = 31.toChar()

    fun hash(
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
    ): String {
        val payload = buildString {
            append(packageName).append(SEP)
            append(title.orEmpty()).append(SEP)
            append(text.orEmpty()).append(SEP)
            append(bigText.orEmpty())
        }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String {
        val digits = "0123456789abcdef"
        val hex = CharArray(size * 2)
        for (i in indices) {
            val v = this[i].toInt() and 0xFF
            hex[i * 2] = digits[v ushr 4]
            hex[i * 2 + 1] = digits[v and 0x0F]
        }
        return String(hex)
    }
}
