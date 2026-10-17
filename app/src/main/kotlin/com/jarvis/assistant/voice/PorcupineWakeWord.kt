package com.jarvis.assistant.voice

import android.content.Context
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import ai.picovoice.porcupine.PorcupineManagerErrorCallback

/**
 * Offline "Jarvis" wake-word detector (Picovoice Porcupine, built-in JARVIS keyword).
 *
 * Unlike looping Android's SpeechRecognizer, this keeps one AudioRecord open and runs a tiny
 * on-device model, so there is no start/stop beep, no mic flicker and very little battery use.
 * It owns the microphone while running, so [stop] it before using SpeechRecognizer, then
 * [start] it again afterwards.
 */
class PorcupineWakeWord(private val context: Context) {

    @Volatile private var manager: PorcupineManager? = null

    val isRunning: Boolean get() = manager != null

    /** Starts listening. Returns null on success, or a human-readable error message. */
    @Synchronized
    fun start(accessKey: String, onDetected: () -> Unit, onError: (String) -> Unit): String? {
        stop()
        return try {
            val m = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .setErrorCallback(PorcupineManagerErrorCallback { e ->
                    onError(e.message ?: "Porcupine error")
                })
                .build(context, PorcupineManagerCallback { _ -> onDetected() })
            m.start()
            manager = m
            null
        } catch (e: PorcupineException) {
            e.message ?: "Could not start the wake-word engine."
        } catch (e: Exception) {
            e.message ?: "Could not start the wake-word engine."
        }
    }

    @Synchronized
    fun stop() {
        val m = manager ?: return
        manager = null
        runCatching { m.stop() }
        runCatching { m.delete() }
    }
}
