package com.owlitech.owli.assist.vlm

import com.owlitech.owli.assist.settings.VlmTransportMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VlmProfileLoaderTest {

    @Test
    fun parsePublicRegistry_readsCurrentBackendShape() {
        val registry = VlmProfileLoader.parsePublicRegistry(
            """
            {
              "schemaVersion": "vlm_profile_registry/v1",
              "defaultProfileId": "scene-brief",
              "profiles": [
                {
                  "id": "scene-brief",
                  "label": "Scene Brief",
                  "description": "Short scene profile",
                  "transports": {
                    "backend": {
                      "available": true,
                      "supportsStreaming": true,
                      "supportsFollowup": true,
                      "supportsFollowupImages": false
                    },
                    "byok": {
                      "available": true,
                      "supportsStreaming": true,
                      "supportsVision": true
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertNotNull(registry)
        assertEquals("scene-brief", registry?.defaultProfileId)
        assertEquals(1, registry?.profiles?.size)
        val profile = registry?.profiles?.first() ?: error("missing profile")
        assertTrue(profile.backendAvailable)
        assertTrue(profile.directByokAvailable)
        assertTrue(profile.backendSupportsStreaming)
        assertTrue(profile.directByokSupportsStreaming)
    }

    @Test
    fun mergePublicRegistry_preservesLocalByokDetails_andFiltersByTransport() {
        val localRegistry = VlmProfileLoader.parseRegistryAsset(
            """
            {
              "schema_version": "vlm_profile_registry/v1",
              "default_profile_id": "scene-brief",
              "profiles": [
                {
                  "id": "scene-brief",
                  "label": "Scene Brief Local",
                  "description": "Local scene profile",
                  "availability": "both",
                  "backend": {
                    "profile_id": "scene-brief",
                    "supports_streaming": true,
                    "supports_followup": true,
                    "supports_followup_images": false
                  },
                  "byok": {
                    "provider": "openrouter",
                    "model_id": "openai/gpt-5.2-chat",
                    "family": "gpt5",
                    "streaming_enabled": true,
                    "system_prompt": "system local",
                    "overview_prompt": "overview local",
                    "token_policy": {
                      "max_tokens": 420
                    }
                  },
                  "debug": {
                    "embedded_key_allowed": true
                  }
                },
                {
                  "id": "reader-fast",
                  "label": "Reader Fast Local",
                  "description": "Local byok profile",
                  "availability": "byok",
                  "byok": {
                    "provider": "openrouter",
                    "model_id": "openai/gpt-5-nano",
                    "streaming_enabled": false,
                    "system_prompt": "reader system",
                    "overview_prompt": "reader overview",
                    "token_policy": {
                      "max_tokens": 140
                    }
                  },
                  "debug": {
                    "embedded_key_allowed": true
                  }
                }
              ]
            }
            """.trimIndent()
        )
        val publicRegistry = VlmProfileLoader.parsePublicRegistry(
            """
            {
              "schemaVersion": "vlm_profile_registry/v1",
              "defaultProfileId": "scene-brief",
              "profiles": [
                {
                  "id": "scene-brief",
                  "label": "Scene Brief Remote",
                  "description": "Remote scene profile",
                  "transports": {
                    "backend": {
                      "available": true,
                      "supportsStreaming": true,
                      "supportsFollowup": true,
                      "supportsFollowupImages": false
                    },
                    "byok": {
                      "available": true,
                      "supportsStreaming": true,
                      "supportsVision": true
                    }
                  }
                },
                {
                  "id": "reader-fast",
                  "label": "Reader Fast Remote",
                  "description": "Remote reader profile",
                  "transports": {
                    "backend": {
                      "available": false,
                      "supportsStreaming": false,
                      "supportsFollowup": false,
                      "supportsFollowupImages": false
                    },
                    "byok": {
                      "available": true,
                      "supportsStreaming": false,
                      "supportsVision": true
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        ) ?: error("failed to parse public registry")

        val merged = VlmProfileLoader.mergePublicRegistry(publicRegistry, localRegistry)

        assertEquals(VlmProfilesSource.REMOTE_BACKEND, merged.source)
        assertEquals("scene-brief", merged.defaultProfileId)
        assertEquals(2, merged.profiles.size)

        val sceneProfile = merged.resolve("scene-brief", VlmTransportMode.BACKEND_MANAGED)
        assertEquals("Scene Brief Remote", sceneProfile.label)
        assertEquals("openai/gpt-5.2-chat", sceneProfile.modelId)
        assertTrue(sceneProfile.backendManagedAvailable)
        assertTrue(sceneProfile.directByokAvailable)
        assertTrue(sceneProfile.embeddedDebugAvailable)

        val backendProfiles = merged.profilesForTransport(VlmTransportMode.BACKEND_MANAGED)
        assertEquals(listOf("scene-brief"), backendProfiles.map { it.id })

        val byokProfiles = merged.profilesForTransport(VlmTransportMode.DIRECT_OPENROUTER_BYOK)
        assertEquals(listOf("scene-brief", "reader-fast"), byokProfiles.map { it.id })

        val debugProfiles = merged.profilesForTransport(VlmTransportMode.EMBEDDED_DEBUG)
        assertEquals(listOf("scene-brief", "reader-fast"), debugProfiles.map { it.id })
    }

    @Test
    fun parsePublicRegistry_rejectsUnexpectedSchemaVersion() {
        val registry = VlmProfileLoader.parsePublicRegistry(
            """
            {
              "schemaVersion": "vlm_profile_registry/v2",
              "defaultProfileId": "scene-brief",
              "profiles": []
            }
            """.trimIndent()
        )

        assertFalse(registry != null)
    }
}
