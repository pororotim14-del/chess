package com.chessassistant.security.cipher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.SecureRandom

class AesCipherTest {

    private val random = SecureRandom()

    @Test
    fun `round trip preserves payload`() {
        val key = randomBytes(32)
        val payload = "attack main thrust".toByteArray()

        val blob = AesCipher.encrypt(payload, key)

        assertEquals(payload.toList(), AesCipher.decrypt(blob, key).toList())
    }

    @Test
    fun `different key cannot decrypt`() {
        val key1 = randomBytes(32)
        val key2 = randomBytes(32)
        val blob = AesCipher.encrypt("secret".toByteArray(), key1)

        assertThrows(Exception::class.java) {
            AesCipher.decrypt(blob, key2)
        }
    }

    @Test
    fun `rejects a non 32-byte key`() {
        assertThrows(IllegalArgumentException::class.java) {
            AesCipher.encrypt("data".toByteArray(), randomBytes(16))
        }
    }

    @Test
    fun `encryption is non deterministic`() {
        val key = randomBytes(32)
        val payload = "same".toByteArray()

        val a = AesCipher.encrypt(payload, key)
        val b = AesCipher.encrypt(payload, key)

        assertEquals(false, a.toList() == b.toList())
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }
}