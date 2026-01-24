package com.owlitech.owli.assist.vlm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class VlmAttachment(
    val id: String,
    val jpegBytes: ByteArray,
    val createdAtMs: Long
)

class VlmAttachmentStore(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val nextId = AtomicLong(1L)
    private val _attachments = MutableStateFlow<List<VlmAttachment>>(emptyList())
    val attachments: StateFlow<List<VlmAttachment>> = _attachments

    fun add(jpegBytes: ByteArray): VlmAttachment {
        val attachment = VlmAttachment(
            id = "att_${nextId.getAndIncrement()}",
            jpegBytes = jpegBytes,
            createdAtMs = clock()
        )
        _attachments.value = _attachments.value + attachment
        return attachment
    }

    fun remove(id: String): Boolean {
        val current = _attachments.value
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        _attachments.value = updated
        return true
    }

    fun clear() {
        _attachments.value = emptyList()
    }
}
