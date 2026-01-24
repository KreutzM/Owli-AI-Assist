package com.owlitech.owli.assist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VlmAttachmentStoreTest {

    @Test
    fun addAttachment_appendsToList() {
        val store = VlmAttachmentStore(clock = { 1234L })
        val bytes = byteArrayOf(1, 2, 3)

        val added = store.add(bytes)

        val attachments = store.attachments.value
        assertEquals(1, attachments.size)
        assertEquals(added.id, attachments.first().id)
        assertEquals(1234L, attachments.first().createdAtMs)
        assertEquals(bytes, attachments.first().jpegBytes)
    }

    @Test
    fun removeAttachment_removesById() {
        val store = VlmAttachmentStore(clock = { 1L })
        val first = store.add(byteArrayOf(1))
        val second = store.add(byteArrayOf(2))

        val removed = store.remove(first.id)

        assertTrue(removed)
        val remaining = store.attachments.value
        assertEquals(1, remaining.size)
        assertEquals(second.id, remaining.first().id)
    }

    @Test
    fun removeAttachment_unknownId_returnsFalse() {
        val store = VlmAttachmentStore()
        store.add(byteArrayOf(1))

        val removed = store.remove("missing")

        assertFalse(removed)
        assertEquals(1, store.attachments.value.size)
    }

    @Test
    fun clearAttachments_emptiesList() {
        val store = VlmAttachmentStore()
        store.add(byteArrayOf(1))
        store.add(byteArrayOf(2))

        store.clear()

        assertTrue(store.attachments.value.isEmpty())
    }
}
