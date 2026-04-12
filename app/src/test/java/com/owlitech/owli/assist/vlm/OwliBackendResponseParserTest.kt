package com.owlitech.owli.assist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwliBackendResponseParserTest {
    @Test
    fun parsesBootstrapResponse() {
        val body = """
            {
              "sessionToken": "session-token",
              "expiresAt": "2026-04-12T05:51:05.000Z",
              "featureFlags": {
                "sceneDescribe": true,
                "followup": false
              }
            }
        """.trimIndent()

        val parsed = OwliBackendResponseParser.parseBootstrap(body)

        requireNotNull(parsed)
        assertEquals("session-token", parsed.sessionToken)
        assertTrue(parsed.canDescribe)
        assertFalse(parsed.canFollowUp)
        assertEquals("2026-04-12T05:51:05Z", parsed.expiresAt.toString())
    }

    @Test
    fun parsesDescribeResponse() {
        val body = """
            {
              "answerText": "A blue owl icon.",
              "mode": "describe",
              "modelAlias": "scene-describe-v1",
              "requestId": "req-1",
              "sceneToken": "scene-token",
              "sceneTokenExpiresAt": "2026-04-12T05:47:11.000Z"
            }
        """.trimIndent()

        val parsed = OwliBackendResponseParser.parseDescribe(body)

        requireNotNull(parsed)
        assertEquals("A blue owl icon.", parsed.answerText)
        assertEquals("describe", parsed.mode)
        assertEquals("scene-describe-v1", parsed.modelAlias)
        assertEquals("req-1", parsed.requestId)
        assertEquals("scene-token", parsed.sceneToken)
        assertEquals("2026-04-12T05:47:11Z", parsed.sceneTokenExpiresAt.toString())
    }

    @Test
    fun parsesFollowupResponse() {
        val body = """
            {
              "answerText": "The eyes are bright blue.",
              "mode": "followup",
              "modelAlias": "scene-followup-v1",
              "requestId": "req-2"
            }
        """.trimIndent()

        val parsed = OwliBackendResponseParser.parseFollowUp(body)

        requireNotNull(parsed)
        assertEquals("The eyes are bright blue.", parsed.answerText)
        assertEquals("followup", parsed.mode)
        assertEquals("scene-followup-v1", parsed.modelAlias)
        assertEquals("req-2", parsed.requestId)
    }

    @Test
    fun extractsBackendErrorMessage() {
        val body = """
            {
              "error": "BAD_REQUEST",
              "message": "Scene describe request validation failed."
            }
        """.trimIndent()

        assertEquals(
            "Scene describe request validation failed.",
            OwliBackendResponseParser.parseErrorMessage(body)
        )
    }

    @Test
    fun rejectsInvalidPayloads() {
        assertNull(OwliBackendResponseParser.parseBootstrap("""{"foo":"bar"}"""))
        assertNull(OwliBackendResponseParser.parseDescribe("""{"answerText":"x"}"""))
        assertNull(OwliBackendResponseParser.parseFollowUp("not-json"))
    }
}
