package com.example.elosonoro.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecorderManagerTest {

    @Test
    fun testAudioRecorderMaxDurationIsStrictly5Seconds() {
        val fakeRecorder = FakeAudioRecorder()
        assertEquals(5000L, fakeRecorder.maxDurationMs)
        assertEquals(5000L, AudioRecorderManager.MAX_RECORDING_DURATION_MS)
    }

    @Test
    fun testAudioRecorderStartStopFlow() {
        val fakeRecorder = FakeAudioRecorder()
        assertFalse(fakeRecorder.isRecording())

        val started = fakeRecorder.startRecording("/path/to/audio_rec.mp4")
        assertTrue(started)
        assertTrue(fakeRecorder.isRecording())

        val stoppedPath = fakeRecorder.stopRecording()
        assertEquals("/path/to/audio_rec.mp4", stoppedPath)
        assertFalse(fakeRecorder.isRecording())
    }

    @Test
    fun testAudioRecorderAutoStopOnMaxDurationReached() {
        val fakeRecorder = FakeAudioRecorder()
        var maxDurationReached = false

        fakeRecorder.startRecording("/path/to/audio_limit.mp4") {
            maxDurationReached = true
        }

        // Simula o disparo do limite de 5s
        fakeRecorder.triggerMaxDuration()

        assertTrue(maxDurationReached)
        assertFalse(fakeRecorder.isRecording())
    }

    private class FakeAudioRecorder : AudioRecorder {
        private var recording = false
        private var currentPath: String? = null
        private var onMaxDuration: (() -> Unit)? = null

        override fun startRecording(outputFilePath: String, onMaxDurationReached: (() -> Unit)?): Boolean {
            recording = true
            currentPath = outputFilePath
            onMaxDuration = onMaxDurationReached
            return true
        }

        override fun stopRecording(): String? {
            recording = false
            return currentPath
        }

        override fun isRecording(): Boolean = recording

        override fun release() {
            recording = false
        }

        fun triggerMaxDuration() {
            stopRecording()
            onMaxDuration?.invoke()
        }
    }
}
