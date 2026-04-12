package com.owlitech.owli.assist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OwliBackendStreamingFailureHandlingTest {
    private val client = OwliBackendVlmClient(
        profile = VlmProfileLoader.fallbackProfiles().first(),
        installationIdProvider = { "installation-id" }
    )

    @Test
    fun earlyClosedStreamFallsBackAndUsesGenericMessage() {
        val exception = client.classifyStreamingIOException(IOException("closed"), sawDelta = false)

        assertTrue(exception.canFallback)
        assertEquals("Owli backend streaming did not start correctly.", exception.message)
    }

    @Test
    fun postDeltaClosedStreamDoesNotFallback() {
        val exception = client.classifyStreamingIOException(IOException("closed"), sawDelta = true)

        assertFalse(exception.canFallback)
        assertEquals("Owli backend streaming ended before completion.", exception.message)
    }

    @Test
    fun rawMessageIsNotExposedToUserFacingException() {
        val exception = client.classifyStreamingIOException(IOException("closed"), sawDelta = false)

        assertEquals("Owli backend streaming did not start correctly.", exception.message)
        assertEquals("closed", exception.cause?.message)
    }
}
