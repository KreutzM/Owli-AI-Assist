package com.owlitech.owli.assist.settings

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class EncryptedOpenRouterUserKeyBlobTest {
    @Test
    fun encodedBlobRoundTripsMetadata() {
        val blob = EncryptedOpenRouterUserKeyBlob(
            version = 1,
            iv = byteArrayOf(1, 2, 3, 4),
            ciphertext = byteArrayOf(9, 8, 7, 6)
        )

        val decoded = EncryptedOpenRouterUserKeyBlob.decodeOrNull(blob.encode())

        requireNotNull(decoded)
        assertEquals(1, decoded.version)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), decoded.iv)
        assertArrayEquals(byteArrayOf(9, 8, 7, 6), decoded.ciphertext)
    }

    @Test
    fun encodedBlobDoesNotContainPlaintextKey() {
        val plaintextKey = "sk-or-example"
        val blob = EncryptedOpenRouterUserKeyBlob(
            version = 1,
            iv = byteArrayOf(1, 2, 3, 4),
            ciphertext = byteArrayOf(12, 13, 14, 15)
        )

        assertFalse(blob.encode().contains(plaintextKey))
    }

    @Test
    fun invalidBlobReturnsNull() {
        assertNull(EncryptedOpenRouterUserKeyBlob.decodeOrNull("not-a-valid-blob"))
        assertNull(EncryptedOpenRouterUserKeyBlob.decodeOrNull("1::abc"))
        assertNull(EncryptedOpenRouterUserKeyBlob.decodeOrNull("1:abc:"))
    }
}
