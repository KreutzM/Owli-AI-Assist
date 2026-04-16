package com.owlitech.owli.assist.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VlmComposerLogicTest {

    @Test
    fun `allows send with text only`() {
        assertTrue(
            canSubmitFollowUp(
                question = "Where is the door?",
                attachmentCount = 0,
                isBusy = false
            )
        )
    }

    @Test
    fun `allows send with image attachment only`() {
        assertTrue(
            canSubmitFollowUp(
                question = "   ",
                attachmentCount = 1,
                isBusy = false
            )
        )
    }

    @Test
    fun `rejects empty follow up without text or image`() {
        assertFalse(
            canSubmitFollowUp(
                question = "   ",
                attachmentCount = 0,
                isBusy = false
            )
        )
    }

    @Test
    fun `rejects send while busy`() {
        assertFalse(
            canSubmitFollowUp(
                question = "Where is the exit?",
                attachmentCount = 1,
                isBusy = true
            )
        )
    }
}
