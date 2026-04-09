package com.owlitech.owli.assist.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultsUseEmbeddedOpenRouterKeyMode() {
        val settings = AppSettings()

        assertEquals(OpenRouterKeyMode.EMBEDDED_APP_KEY, settings.openRouterKeyMode)
    }

    @Test
    fun resolverUsesUserProvidedKeyWhenModeAndKeyAreAvailable() {
        val settings = AppSettings(openRouterKeyMode = OpenRouterKeyMode.USER_PROVIDED_KEY)

        val selection = resolveOpenRouterApiKeySelection(
            settings = settings,
            embeddedAppKey = "embedded",
            userProvidedKey = " user-key "
        )

        assertEquals(OpenRouterKeyMode.USER_PROVIDED_KEY, selection.requestedMode)
        assertEquals(OpenRouterKeyMode.USER_PROVIDED_KEY, selection.activeMode)
        assertEquals("user-key", selection.apiKey)
        assertTrue(selection.hasUsableKey)
    }

    @Test
    fun resolverFallsBackToEmbeddedKeyWhenUserModeHasNoStoredKey() {
        val settings = AppSettings(openRouterKeyMode = OpenRouterKeyMode.USER_PROVIDED_KEY)

        val selection = resolveOpenRouterApiKeySelection(
            settings = settings,
            embeddedAppKey = " embedded ",
            userProvidedKey = null
        )

        assertEquals(OpenRouterKeyMode.USER_PROVIDED_KEY, selection.requestedMode)
        assertEquals(OpenRouterKeyMode.EMBEDDED_APP_KEY, selection.activeMode)
        assertEquals("embedded", selection.apiKey)
        assertTrue(selection.hasUsableKey)
    }

    @Test
    fun resolverReportsMissingKeyWhenEmbeddedKeyIsBlank() {
        val selection = resolveOpenRouterApiKeySelection(
            settings = AppSettings(),
            embeddedAppKey = " "
        )

        assertEquals(OpenRouterKeyMode.EMBEDDED_APP_KEY, selection.activeMode)
        assertFalse(selection.hasUsableKey)
    }
}
