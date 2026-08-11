package com.chessassistant.security

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Hardware- and system-derived identifiers used to bind protected data to a
 * specific installation. All identifiers are PII-free.
 */
data class DeviceFingerprint(
    val model: String,
    val board: String,
    val manufacturer: String,
    val bootId: String?,
) {
    /** Stable short hash suitable as a salt or key-derivation input. */
    fun shortHash(): String {
        val raw = "$manufacturer|$model|$board|${bootId ?: ""}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return BigInteger(1, digest).toString(16).padStart(64, '0').take(16)
    }
}