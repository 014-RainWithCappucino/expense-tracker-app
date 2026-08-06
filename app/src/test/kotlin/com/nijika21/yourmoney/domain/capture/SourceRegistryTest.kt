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
     * Guard rail for M3, in its post-M1 form. `verified` used to be required to
     * be false everywhere; `com.bca` and `com.gojek.gopay` were confirmed on the
     * device on 2026-08-06, so the rule is now about evidence rather than
     * abstinence: flipping the flag costs you a note saying what you saw.
     *
     * Writing a parser against an unverified package name is the wasted work the
     * M1-before-M3 ordering exists to prevent.
     */
    @Test
    fun `a verified candidate has to carry its evidence`() {
        for (candidate in SourceRegistry.candidates) {
            if (!candidate.verified) continue
            assertTrue(
                "${candidate.packageName} is marked verified with no note — record the " +
                    "date and the wording you actually saw on the device",
                !candidate.note.isNullOrBlank(),
            )
        }
    }

    @Test
    fun `the two sources confirmed on hardware stay verified`() {
        // A regression guard with a purpose: if a refactor resets these, M3 has
        // no idea which package names were guesses and which were proven.
        val verified = SourceRegistry.candidates.filter { it.verified }.map { it.packageName }
        assertTrue("com.bca lost its verified flag", "com.bca" in verified)
        assertTrue("com.gojek.gopay lost its verified flag", "com.gojek.gopay" in verified)
    }

    @Test
    fun `packages never seen on the device are not claimed as verified`() {
        val unproven = listOf(
            "com.bca.mybca.omni.android",
            "com.gojek.app",
            "ovo.id",
            "com.shopeepay.id",
            "com.shopee.id",
        )
        for (pkg in unproven) {
            val candidate = SourceRegistry.candidates.first { it.packageName == pkg }
            assertFalse(
                "$pkg has never delivered a notification on the device",
                candidate.verified,
            )
        }
    }
}
