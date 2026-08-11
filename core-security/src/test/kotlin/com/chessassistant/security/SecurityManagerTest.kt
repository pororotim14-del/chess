package com.chessassistant.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityManagerTest {

    private val fingerprint = DeviceFingerprint(
        model = "Pixel 9",
        board = "husky",
        manufacturer = "Google",
        bootId = "boot-0001",
    )

    private val storage = InMemorySecretStorage()
    private val manager = SecurityManager(fingerprint, storage)

    @Test
    fun `installation id is stable`() {
        assertEquals(16, manager.installationId.length)
        assertEquals(manager.installationId, manager.installationId)
    }

    @Test
    fun `encrypt then decrypt restores payload`() {
        val payload = "private data".toByteArray()

        val blob = manager.encrypt(payload)
        val restored = manager.decrypt(blob)

        assertEquals(payload.toList(), restored.toList())
    }

    @Test
    fun `key is reused across calls`() {
        val a = manager.encrypt(byteArrayOf(1, 2, 3))
        val b = manager.encrypt(byteArrayOf(1, 2, 3))
        // Non-deterministic ciphertext but a single key must keep decrypting.
        assertTrue(a.toList() != b.toList())
        assertEquals(byteArrayOf(1, 2, 3).toList(), manager.decrypt(b).toList())
    }

    @Test
    fun `wipe breaks decryption`() {
        val blob = manager.encrypt("gone".toByteArray())
        manager.wipe()
        try {
            manager.decrypt(blob)
            throw AssertionError("expected decryption to fail after wipe")
        } catch (_: Exception) {
            // expected: a fresh key cannot authenticate the old blob
        }
    }
}