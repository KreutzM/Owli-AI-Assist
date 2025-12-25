package com.example.bikeassist.audio

import kotlin.math.min

/**
 * Steuert fruehes TTS bei VLM-Streaming: puffert Deltas und spricht in sauberen Chunks.
 */
class StreamingTtsController(
    private val speaker: Speaker,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val minGapBetweenSpeaksMs: Long = DEFAULT_MIN_GAP_MS,
    private val sentenceMinChars: Int = DEFAULT_SENTENCE_MIN_CHARS,
    private val idleFlushMinChars: Int = DEFAULT_IDLE_MIN_CHARS,
    private val idleFlushTimeoutMs: Long = DEFAULT_IDLE_TIMEOUT_MS,
    private val hardCapChars: Int = DEFAULT_HARD_CAP_CHARS
) {

    interface Speaker {
        fun speakChunk(text: String, queueMode: QueueMode)
        fun stop()
    }

    private val pending = StringBuilder()
    private val spokenHashes = HashSet<Int>()
    private var lastSpeakAt = 0L
    private var lastDeltaAt = 0L

    fun startNewRun() {
        pending.clear()
        spokenHashes.clear()
        lastSpeakAt = 0L
        lastDeltaAt = 0L
        speaker.stop()
    }

    fun cancel() {
        pending.clear()
        spokenHashes.clear()
        lastSpeakAt = 0L
        lastDeltaAt = 0L
        speaker.stop()
    }

    fun onDelta(delta: String) {
        if (delta.isBlank()) return
        pending.append(delta)
        lastDeltaAt = clock()
        maybeSpeakOnDelta()
    }

    fun onIdleTimeout() {
        val now = clock()
        if (pending.length < idleFlushMinChars) return
        if (now - lastDeltaAt < idleFlushTimeoutMs) return
        flushPending(force = false, drainAll = false)
    }

    fun flushRemaining() {
        flushPending(force = true, drainAll = true)
    }

    private fun maybeSpeakOnDelta() {
        val now = clock()
        if (!canSpeak(now, force = false)) return
        if (pending.length >= hardCapChars) {
            val idx = findSplitIndex(pending, min(hardCapChars, pending.length))
            speakAndConsume(idx, now)
            return
        }
        if (pending.length >= sentenceMinChars) {
            val idx = findLastSentenceBoundary(pending)
            if (idx >= 0) {
                speakAndConsume(idx + 1, now)
            }
        }
    }

    private fun flushPending(force: Boolean, drainAll: Boolean) {
        val now = clock()
        if (pending.isEmpty()) return
        if (!canSpeak(now, force)) return
        if (drainAll) {
            while (pending.isNotEmpty()) {
                val idx = findSplitIndex(pending, pending.length)
                speakAndConsume(idx, now)
                if (!canSpeak(now, force = true)) break
            }
        } else {
            val idx = findSplitIndex(pending, pending.length)
            speakAndConsume(idx, now)
        }
    }

    private fun speakAndConsume(endIndex: Int, now: Long) {
        if (endIndex <= 0) return
        val chunk = pending.substring(0, endIndex)
        pending.delete(0, endIndex)
        val normalized = chunk.trim()
        if (normalized.isEmpty()) return
        val hash = normalized.hashCode()
        if (!spokenHashes.add(hash)) {
            return
        }
        speaker.speakChunk(normalized, QueueMode.ADD)
        lastSpeakAt = now
    }

    private fun canSpeak(now: Long, force: Boolean): Boolean {
        return force || lastSpeakAt == 0L || now - lastSpeakAt >= minGapBetweenSpeaksMs
    }

    private fun findLastSentenceBoundary(text: CharSequence): Int {
        for (i in text.length - 1 downTo 0) {
            if (isSentenceEnd(text[i])) return i
        }
        return -1
    }

    private fun findSplitIndex(text: CharSequence, limit: Int): Int {
        val safeLimit = limit.coerceIn(1, text.length)
        for (i in safeLimit - 1 downTo 0) {
            val ch = text[i]
            if (isSplitChar(ch)) return i + 1
        }
        return safeLimit
    }

    private fun isSentenceEnd(ch: Char): Boolean {
        return ch == '.' || ch == '!' || ch == '?' || ch == ';' || ch == ':'
    }

    private fun isSplitChar(ch: Char): Boolean {
        return ch.isWhitespace() || isSentenceEnd(ch)
    }

    companion object {
        const val DEFAULT_MIN_GAP_MS = 350L
        const val DEFAULT_SENTENCE_MIN_CHARS = 40
        const val DEFAULT_IDLE_MIN_CHARS = 45
        const val DEFAULT_IDLE_TIMEOUT_MS = 650L
        const val DEFAULT_HARD_CAP_CHARS = 180
    }
}

enum class QueueMode {
    ADD,
    FLUSH
}
