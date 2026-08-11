package com.chessassistant.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Production [SecretStorage] backed by the Android Keystore. Keys are never
 * extractable and stay in hardware-backed storage when available.
 */
class AndroidKeyStoreSecretStorage : SecretStorage {

    private val keyStore: KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    override fun obtainKey(alias: String): ByteArray {
        val existing = (keyStore.getKey(alias, null) as? SecretKey)
        return (existing ?: generate(alias).also { keyStore.setKeyEntry(alias, it, null, null) })
            .encoded
    }

    private fun generate(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    override fun contains(alias: String): Boolean = keyStore.containsAlias(alias)

    override fun delete(alias: String) {
        keyStore.deleteEntry(alias)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    }
}