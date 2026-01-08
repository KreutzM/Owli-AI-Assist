package com.owlitech.owli.assist.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingTtsControllerTest {

    @Test
    fun speaksOnSentenceEndWhenLongEnough() {
        val speaker = FakeSpeaker()
        val clock = FakeClock()
        val controller = StreamingTtsController(speaker, clock = { clock.now })

        val text = "Das ist ein wirklich langer Satz der jetzt endet."
        controller.onDelta(text)

        assertEquals(1, speaker.spoken.size)
        assertEquals(text, speaker.spoken[0].first)
    }

    @Test
    fun timeoutFlushesWhenIdleAndLongEnough() {
        val speaker = FakeSpeaker()
        val clock = FakeClock()
        val controller = StreamingTtsController(speaker, clock = { clock.now })

        controller.onDelta("Dies ist ein langer Text ohne Satzende der warten muss")
        clock.advance(700)
        controller.onIdleTimeout()

        assertEquals(1, speaker.spoken.size)
    }

    @Test
    fun hardCapSplitsLongBuffers() {
        val speaker = FakeSpeaker()
        val clock = FakeClock()
        val controller = StreamingTtsController(speaker, clock = { clock.now })

        val text = "wort ".repeat(50)
        controller.onDelta(text)

        assertEquals(1, speaker.spoken.size)
        assertTrue(speaker.spoken[0].first.length <= StreamingTtsController.DEFAULT_HARD_CAP_CHARS)
    }

    @Test
    fun dedupSkipsAlreadySpokenChunk() {
        val speaker = FakeSpeaker()
        val clock = FakeClock()
        val controller = StreamingTtsController(speaker, clock = { clock.now })

        val sentence = "Dies ist ein ausreichend langer Satz der endet."
        controller.onDelta(sentence)
        controller.onDelta(sentence)

        assertEquals(1, speaker.spoken.size)
    }

    @Test
    fun startNewRunStopsAndClearsBuffer() {
        val speaker = FakeSpeaker()
        val clock = FakeClock()
        val controller = StreamingTtsController(speaker, clock = { clock.now })

        controller.onDelta("Langer Text ohne Punkt damit nichts gesprochen wird")
        controller.startNewRun()
        clock.advance(700)
        controller.onIdleTimeout()

        assertEquals(1, speaker.stopCount)
        assertEquals(0, speaker.spoken.size)
    }

    private class FakeSpeaker : StreamingTtsController.Speaker {
        val spoken = mutableListOf<Pair<String, QueueMode>>()
        var stopCount = 0

        override fun speakChunk(text: String, queueMode: QueueMode) {
            spoken += text to queueMode
        }

        override fun stop() {
            stopCount += 1
        }
    }

    private class FakeClock {
        var now: Long = 0L
            private set

        fun advance(deltaMs: Long) {
            now += deltaMs
        }
    }
}
