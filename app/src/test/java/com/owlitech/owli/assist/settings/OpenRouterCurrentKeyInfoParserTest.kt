package com.owlitech.owli.assist.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterCurrentKeyInfoParserTest {
    @Test
    fun parsesCurrentKeyInfoResponse() {
        val body = """
            {
              "data": {
                "label": "sk-or-v1-au7...890",
                "limit": 100,
                "limit_remaining": 74.5,
                "limit_reset": "monthly",
                "usage": 25.5,
                "usage_daily": 5.5,
                "usage_weekly": 10.5,
                "usage_monthly": 25.5,
                "is_free_tier": false,
                "expires_at": "2027-12-31T23:59:59Z"
              }
            }
        """.trimIndent()

        val parsed = OpenRouterCurrentKeyInfoParser.parse(body)

        requireNotNull(parsed)
        assertEquals("sk-or-v1-au7...890", parsed.label)
        assertEquals(100.0, parsed.limit)
        assertEquals(74.5, parsed.limitRemaining)
        assertEquals("monthly", parsed.limitReset)
        assertEquals(25.5, parsed.usage)
        assertFalse(parsed.isFreeTier ?: true)
        assertEquals("2027-12-31T23:59:59Z", parsed.expiresAt)
    }

    @Test
    fun handlesMissingOptionalFields() {
        val body = """{"data":{"is_free_tier":true}}"""

        val parsed = OpenRouterCurrentKeyInfoParser.parse(body)

        requireNotNull(parsed)
        assertTrue(parsed.isFreeTier == true)
        assertNull(parsed.label)
        assertNull(parsed.limit)
        assertNull(parsed.limitRemaining)
    }

    @Test
    fun rejectsInvalidPayload() {
        assertNull(OpenRouterCurrentKeyInfoParser.parse("""{"error":"bad"}"""))
        assertNull(OpenRouterCurrentKeyInfoParser.parse("not json"))
    }
}
