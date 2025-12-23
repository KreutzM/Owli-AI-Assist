package com.example.bikeassist.vlm

data class VlmSession(
    val snapshotBytes: ByteArray,
    val messageHistory: MutableList<VlmChatMessage>
)
