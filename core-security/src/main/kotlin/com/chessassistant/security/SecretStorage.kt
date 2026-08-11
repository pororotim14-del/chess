package com.chessassistant.security

/**
 * Abstraction over device key storage. Production implementations wrap the
 * Android Keystore; tests use an in-memory counterpart.
 */
interface SecretStorage {
    /**
     * Returns the 32-byte symmetric key under [alias], creating and storing
     * it when it does not yet exist.
     */
    fun obtainKey(alias: String): ByteArray

    fun contains(alias: String): Boolean

    fun delete(alias: String)
}