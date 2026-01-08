package com.owlitech.owli.assist.vlm

data class VlmSession(
    val snapshotBytes: ByteArray,
    val messageHistory: MutableList<VlmChatMessage>
)
