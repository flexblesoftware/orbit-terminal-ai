package com.orbitterminal.app.data.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream

data class TerminalSession(
    val id: String,
    val title: String,
    val process: Process,
    val workingDir: String = "/root",
    val createdAt: Long = System.currentTimeMillis()
) {
    val inputStream: InputStream = process.inputStream
    val errorStream: InputStream = process.errorStream
    val outputStream: OutputStream = process.outputStream

    private val _outputHistory = MutableStateFlow("")
    val outputHistory: StateFlow<String> = _outputHistory.asStateFlow()

    fun appendOutput(text: String) {
        val current = _outputHistory.value
        val updated = if (current.length > 50000) {
            current.takeLast(30000) + text
        } else {
            current + text
        }
        _outputHistory.value = updated
    }

    fun clearOutput() {
        _outputHistory.value = ""
    }

    fun writeInput(text: String) {
        try {
            outputStream.write(text.toByteArray())
            outputStream.flush()
        } catch (_: Exception) {}
    }

    fun sendCtrlC() {
        writeInput("\u0003")
    }

    fun isAlive(): Boolean = process.isAlive

    fun terminate() {
        process.destroyForcibly()
    }
}
