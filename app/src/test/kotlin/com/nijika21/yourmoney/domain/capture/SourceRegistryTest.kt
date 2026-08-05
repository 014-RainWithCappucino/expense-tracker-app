package com.nijika21.yourmoney.domain.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The privacy boundary from TDD §3.3 is a correctness requirement, not
 * manners — it is what the setup screen promises the user. These tests exist
 * so that widening the whitelist is always a deliberate, visible act.
 */
class SourceRegistryTest {

    @Test
    fun `every declared candidate is captured`() {
        for (candidate in SourceRegistry.candidates) {
            assertTrue(
                "${candidate.packageName} is declared but not captured",
                SourceRegistry.isCaptured(candidate.packageName),
            )
        }
    }

    @Test
    fun `nothing outside the whitelist is captured`() {
        val outsiders = listOf(
            "com.whatsapp",
            "com.google.android.gm",
            "com.instagram.android",
            "com.android.systemui",
            "com.nijika21.yourmoney",
            "",
        )
        for (pkg in outsiders) {
            assertFalse("$pkg must not be captured", SourceRegistry.isCaptured(pkg))
        }
    }

    @Test
    fun `matching is exact, not prefix or substring`() {
        // "com.bca" is whitelisted; a lookalike package must not ride along.
        assertFalse(SourceRegistry.isCaptured("com.bca.fake"))
        assertFalse(SourceRegistry.isCaptured("com.bc"))
        assertFalse(SourceRegistry.isCaptured("notcom.bca"))
        assertTrue(SourceRegistry.isCaptured("com.bca"))
    }

    @Test
    fun `package names are unique`() {
        val names = SourceRegistry.candidates.map { it.packageName }
        assertTrue("duplicate package in registry", names.size == names.toSet().size)
    }

    @Test
    fun `labels resolve only for known packages`() {
        assertNull(SourceRegistry.labelFor("com.whatsapp"))
        for (candidate in SourceRegistry.candidates) {
            assertTrue(SourceRegistry.labelFor(candidate.packageName) == candidate.label)
        }
    }

    /**
     * Guard rail for M3. Every package name in the registry is still a guess
     * (§3.4); writing parsers against an unverified name is the wasted work
     * the M1-before-M3 ordering exists to prevent.
     */
    @Test
    fun `no candidate is marked verified until the device says so`() {
        for (candidate in SourceRegistry.candidates) {
            assertFalse(
                "${candidate.packageName} claims to be verified — confirm it on the " +
                    "device via the diagnostics screen before flipping this",
                candidate.verified,
            )
        }
    }
}
