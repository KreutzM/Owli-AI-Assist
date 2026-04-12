package com.owlitech.owli.assist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwliBackendSseParserTest {
    @Test
    fun parsesMetadataEvent() {
        val event = OwliBackendSseParser.parseEvent(
            "metadata",
            """{"mode":"describe","modelAlias":"scene-describe-v1","profileId":"default","locale":"de-DE"}"""
        ) as OwliBackendSseEvent.Metadata

        assertEquals("describe", event.mode)
        assertEquals("scene-describe-v1", event.modelAlias)
        assertEquals("default", event.profileId)
        assertEquals("de-DE", event.locale)
    }

    @Test
    fun parsesDeltaEvent() {
        val event = OwliBackendSseParser.parseEvent(
            "delta",
            """{"textDelta":"Hallo","requestId":"req-1"}"""
        ) as OwliBackendSseEvent.Delta

        assertEquals("Hallo", event.textDelta)
        assertEquals("req-1", event.requestId)
    }

    @Test
    fun parsesDoneEventWithSceneToken() {
        val event = OwliBackendSseParser.parseEvent(
            "done",
            """
                {
                  "answerText": "A blue owl icon.",
                  "mode": "describe",
                  "modelAlias": "scene-describe-v1",
                  "requestId": "req-1",
                  "sceneToken": "scene-token",
                  "sceneTokenExpiresAt": "2026-04-12T05:47:11.000Z"
                }
            """.trimIndent()
        ) as OwliBackendSseEvent.Done

        assertEquals("A blue owl icon.", event.answerText)
        assertEquals("describe", event.mode)
        assertEquals("scene-describe-v1", event.modelAlias)
        assertEquals("req-1", event.requestId)
        assertEquals("scene-token", event.sceneToken)
        assertEquals("2026-04-12T05:47:11Z", event.sceneTokenExpiresAt.toString())
    }

    @Test
    fun parsesErrorEvent() {
        val event = OwliBackendSseParser.parseEvent(
            "error",
            """{"error":"STREAM_FAILED","message":"Streaming failed."}"""
        ) as OwliBackendSseEvent.Error

        assertEquals("Streaming failed.", event.message)
    }

    @Test
    fun rejectsInvalidOrUnknownEvents() {
        assertNull(OwliBackendSseParser.parseEvent("delta", """{"requestId":"req-1"}"""))
        assertNull(OwliBackendSseParser.parseEvent("unknown", """{"foo":"bar"}"""))
        assertNull(OwliBackendSseParser.parseEvent("done", "not-json"))
    }
}
