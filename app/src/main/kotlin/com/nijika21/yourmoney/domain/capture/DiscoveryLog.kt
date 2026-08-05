package com.nijika21.yourmoney.domain.capture

/**
 * Solves the problem [SourceRegistry] creates.
 *
 * The whitelist has to be checked before anything is stored, but every package
 * name in it is a guess (TDD §3.4). If a guess is wrong the app silently
 * captures nothing, and M1's whole purpose — accumulating a real corpus while
 * M2–M4 are built — quietly fails with no visible symptom until M3.
 *
 * So: for packages *outside* the whitelist we record the package name and a
 * count, and **never the content**. No title, no text, no bigText. That is
 * enough for the diagnostics screen to say "ovo.id — 14 notifications today",
 * which is what turns a guessed package name into a verified one, while
 * keeping the §3.3 promise that non-whitelisted content is never persisted.
 *
 * This is a build-time diagnostic, not a product feature: it is expected to be
 * switched off once the package list is confirmed.
 */
data class DiscoveredSource(
    val packageName: String,
    val count: Long,
    val firstSeen: Long,
    val lastSeen: Long,
) {
    /** True when this package is already one we capture content from. */
    val isKnownCandidate: Boolean get() = SourceRegistry.isCaptured(packageName)

    val candidateLabel: String? get() = SourceRegistry.labelFor(packageName)
}
