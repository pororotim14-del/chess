package com.chessassistant.security.cipher

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM helpers with an explicit 96-bit nonce prepended to the output.
 * Pure JVM so it is unit-testable on Robolectric.
 */
object AesCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    /**
     * Encrypts [plain] under a 32-byte [key]. The returned blob is
     * `nonce || ciphertext || tag` (IV length + block-compatible).
     */
    fun encrypt(plain: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        require(iv != null && iv.size == IV_LENGTH)
        val encrypted = cipher.doFinal(plain)
        return iv + encrypted
    }

    /**
     * Decrypts a blob produced by [encrypt].
     */
    fun decrypt(blob: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        require(blob.size > IV_LENGTH) { "Malformed blob (too short)" }
        val iv = blob.copyOfRange(0, IV_LENGTH)
        val payload = blob.copyOfRange(IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(payload)
    }
}