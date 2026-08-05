package com.nijika21.yourmoney.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContentHasherTest {

    @Test
    fun `identical content hashes identically`() {
        val a = ContentHasher.hash("com.bca", "BCA", "Rp45.000", null)
        val b = ContentHasher.hash("com.bca", "BCA", "Rp45.000", null)
        assertEquals(a, b)
    }

    @Test
    fun `null and empty are the same field value`() {
        // The extras bundle gives null for a missing field on some OEMs and ""
        // on others; treating them differently would double-store every one.
        assertEquals(
            ContentHasher.hash("com.bca", "BCA", null, null),
            ContentHasher.hash("com.bca", "BCA", "", ""),
        )
    }

    @Test
    fun `a different package is a different hash`() {
        assertNotEquals(
            ContentHasher.hash("com.bca", "T", "x", null),
            ContentHasher.hash("ovo.id", "T", "x", null),
        )
    }

    /**
     * The reason the delimiter is U+001F and not a space. With a space
     * separator these two collide, and a real transaction would be silently
     * swallowed as a duplicate.
     */
    @Test
    fun `field boundaries cannot be shifted to force a collision`() {
        assertNotEquals(
            ContentHasher.hash("com.bca", "ab", "c", null),
            ContentHasher.hash("com.bca", "a", "bc", null),
        )
        assertNotEquals(
            ContentHasher.hash("com.bca", "a b", "c", null),
            ContentHasher.hash("com.bca", "a", "b c", null),
        )
    }

    @Test
    fun `hash is a 64 char lowercase hex sha256`() {
        val h = ContentHasher.hash("com.bca", "BCA", "Rp45.000", null)
        assertEquals(64, h.length)
        assertEquals(h.lowercase(), h)
        assertEquals(true, h.all { it in "0123456789abcdef" })
    }

    @Test
    fun `bigText participates in the hash`() {
        assertNotEquals(
            ContentHasher.hash("com.bca", "T", "x", "long form"),
            ContentHasher.hash("com.bca", "T", "x", "other form"),
        )
    }
}
