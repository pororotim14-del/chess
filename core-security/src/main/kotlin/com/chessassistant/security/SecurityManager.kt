package com.chessassistant.security

import com.chessassistant.security.cipher.AesCipher

/**
 * High-level façade the app talks to: fingerprints the installation, derives
 * a storage key and encrypts/decrypts opaque payloads.
 */
class SecurityManager(
    private val fingerprint: DeviceFingerprint,
    private val storage: SecretStorage,
) {

    private val canonicalAlias = "app.data.key.v1"

    /** 16-char installation id, safe to store plain-text. */
    val installationId: String = fingerprint.shortHash()

    @Volatile
    private var cachedKey: ByteArray? = null

    private fun key(): ByteArray {
        cachedKey?.let { return it }
        return storage.obtainKey(canonicalAlias).also { cachedKey = it }
    }

    fun encrypt(payload: ByteArray): ByteArray = AesCipher.encrypt(payload, key())

    fun decrypt(blob: ByteArray): ByteArray = AesCipher.decrypt(blob, key())

    fun wipe() {
        storage.delete(canonicalAlias)
        cachedKey = null
    }
}