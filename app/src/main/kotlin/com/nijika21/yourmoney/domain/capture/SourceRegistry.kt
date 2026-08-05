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
 * ⚠ Every package below is UNVERIFIED — taken from memory, exactly as TDD
 * §3.4 warns not to trust. They are candidates, and [DiscoveryLog] is how
 * they get confirmed on the real device instead of guessed. Do not write
 * parsers against any of these until the diagnostics screen has shown the
 * package actually delivering transaction notifications.
 */
object SourceRegistry {

    data class Candidate(
        val packageName: String,
        val label: String,
        val verified: Boolean = false,
    )

    val candidates: List<Candidate> = listOf(
        Candidate("com.bca", "BCA (mobile)"),
        Candidate("com.bca.mybca.omni.android", "myBCA"),
        Candidate("com.gojek.app", "Gojek / GoPay"),
        Candidate("com.gojek.gopay", "GoPay (standalone)"),
        Candidate("ovo.id", "OVO"),
    )

    private val packages: Set<String> = candidates.mapTo(mutableSetOf()) { it.packageName }

    /** True when this package's notification content may be stored. */
    fun isCaptured(packageName: String): Boolean = packageName in packages

    fun labelFor(packageName: String): String? =
        candidates.firstOrNull { it.packageName == packageName }?.label
}
