package com.chessassistant.security

import java.security.SecureRandom

/**
 * Nonsensical in a real app; used only so logic that depends on [SecretStorage]
 * is unit-testable. Production shipping builds pick the Android Keystore.
 */
class InMemorySecretStorage : SecretStorage {

    private val store = mutableMapOf<String, ByteArray>()

    override fun obtainKey(alias: String): ByteArray =
        store[alias] ?: ByteArray(32).also { random.nextBytes(it) }.also { store[alias] = it }

    override fun contains(alias: String): Boolean = store.containsKey(alias)

    override fun delete(alias: String) {
        store.remove(alias)
    }

    companion object {
        private val random = SecureRandom()
    }
}