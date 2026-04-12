package com.owlitech.owli.assist.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultsUseBackendManagedTransport() {
        val settings = AppSettings()

        assertEquals(VlmTransportMode.BACKEND_MANAGED, settings.vlmTransportMode)
        assertEquals(OpenRouterKeyMode.EMBEDDED_APP_KEY, settings.openRouterKeyMode)
    }

    @Test
    fun transportResolverUsesUserProvidedKeyForByokMode() {
        val settings = AppSettings(vlmTransportMode = VlmTransportMode.DIRECT_OPENROUTER_BYOK)

        val selection = resolveVlmTransportSelection(
            settings = settings,
            embeddedAppKey = "embedded",
            userProvidedKey = " user-key "
        )

        assertEquals(VlmTransportMode.DIRECT_OPENROUTER_BYOK, selection.requestedMode)
        assertEquals(VlmTransportMode.DIRECT_OPENROUTER_BYOK, selection.activeMode)
        assertEquals("user-key", selection.apiKey)
        assertTrue(selection.hasUsableTransport)
    }

    @Test
    fun transportResolverDoesNotHideMissingByokKeyBehindEmbeddedFallback() {
        val settings = AppSettings(vlmTransportMode = VlmTransportMode.DIRECT_OPENROUTER_BYOK)

        val selection = resolveVlmTransportSelection(
            settings = settings,
            embeddedAppKey = " embedded ",
            userProvidedKey = null
        )

        assertEquals(VlmTransportMode.DIRECT_OPENROUTER_BYOK, selection.requestedMode)
        assertEquals(VlmTransportMode.DIRECT_OPENROUTER_BYOK, selection.activeMode)
        assertEquals("", selection.apiKey)
        assertFalse(selection.hasUsableTransport)
    }

    @Test
    fun transportResolverReportsMissingEmbeddedDebugKeyWhenBlank() {
        val selection = resolveVlmTransportSelection(
            settings = AppSettings(vlmTransportMode = VlmTransportMode.EMBEDDED_DEBUG),
            embeddedAppKey = " "
        )

        assertEquals(VlmTransportMode.EMBEDDED_DEBUG, selection.activeMode)
        assertFalse(selection.hasUsableTransport)
    }

    @Test
    fun openRouterSelectionMappingMatchesTransportMode() {
        val byokSelection = VlmTransportSelection(
            requestedMode = VlmTransportMode.DIRECT_OPENROUTER_BYOK,
            activeMode = VlmTransportMode.DIRECT_OPENROUTER_BYOK,
            apiKey = "user"
        ).toOpenRouterApiKeySelection()
        val debugSelection = VlmTransportSelection(
            requestedMode = VlmTransportMode.EMBEDDED_DEBUG,
            activeMode = VlmTransportMode.EMBEDDED_DEBUG,
            apiKey = "embedded"
        ).toOpenRouterApiKeySelection()

        assertEquals(OpenRouterKeyMode.USER_PROVIDED_KEY, byokSelection.activeMode)
        assertEquals("user", byokSelection.apiKey)
        assertEquals(OpenRouterKeyMode.EMBEDDED_APP_KEY, debugSelection.activeMode)
        assertEquals("embedded", debugSelection.apiKey)
    }
}
