package com.nijika21.yourmoney.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Supplies the SQLCipher passphrase (TDD §6.1).
 *
 * The passphrase is a random 256-bit value. It is **not** derived from the
 * PIN — deriving it would make the whole database offline-brute-forceable
 * against a 10^6 keyspace, which is strictly worse than what we do here.
 * The PIN is a UI gate only.
 *
 * Keystore keys are non-extractable, so the passphrase cannot *be* the
 * Keystore key. Instead: generate the passphrase once, wrap it with a
 * hardware-backed AES-GCM Keystore key, and keep only the wrapped blob.
 * Unwrapping requires the device's Keystore, so a copied `.db` plus a copied
 * prefs file is still useless off-device.
 *
 * Deliberately **no** `setUserAuthenticationRequired(true)`: per §6.5 the
 * adversary already knows the device lock credential, so binding to it would
 * add no protection while making the app unopenable whenever the lock is
 * changed. Nothing in this app may depend on the Android device lock.
 *
 * Consequence, stated in §6.1: because the key never leaves the device, backup
 * must be a logical export (§9), never a copy of the encrypted file.
 */
class DatabaseKeyProvider(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Returns the raw passphrase bytes, creating and wrapping them on first run. */
    fun passphrase(): ByteArray {
        prefs.getString(KEY_WRAPPED, null)?.let { stored ->
            return unwrap(stored)
        }
        val fresh = ByteArray(PASSPHRASE_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_WRAPPED, wrap(fresh)).commit()
        return fresh
    }

    private fun wrap(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        // iv length is fixed at 12 for GCM, but prefix it anyway so a future
        // provider change cannot silently misparse an old blob.
        val out = ByteArray(1 + iv.size + ct.size)
        out[0] = iv.size.toByte()
        iv.copyInto(out, 1)
        ct.copyInto(out, 1 + iv.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun unwrap(stored: String): ByteArray {
        val blob = Base64.decode(stored, Base64.NO_WRAP)
        val ivLen = blob[0].toInt()
        val iv = blob.copyOfRange(1, 1 + ivLen)
        val ct = blob.copyOfRange(1 + ivLen, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "yourmoney_db_key"
        const val PREFS = "yourmoney_db"
        const val KEY_WRAPPED = "wrapped_passphrase"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PASSPHRASE_BYTES = 32
    }
}
