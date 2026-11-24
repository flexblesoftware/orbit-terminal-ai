package com.orbitterminal.app.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbitterminal.app.data.terminal.LinuxEnvironmentManager
import com.orbitterminal.app.data.terminal.SetupState
import com.orbitterminal.app.data.terminal.TerminalController
import com.orbitterminal.app.data.terminal.TerminalSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val terminalController: TerminalController,
    private val envManager: LinuxEnvironmentManager
) : ViewModel() {

    val setupState: StateFlow<SetupState> = envManager.setupState
    val sessions: StateFlow<List<TerminalSession>> = terminalController.sessions
    val activeSessionId: StateFlow<String?> = terminalController.activeSessionId

    val activeSessionOutput: Flow<String> = activeSessionId.flatMapLatest { id ->
        val currentSession = sessions.value.find { it.id == id }
        currentSession?.outputHistory ?: flowOf("")
    }

    init {
        checkAndInitialize()
    }

    private fun checkAndInitialize() {
        viewModelScope.launch {
            try {
                if (sessions.value.isEmpty()) {
                    terminalController.createSession("Main")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing default terminal session")
            }
        }
    }

    fun startSetup() {
        viewModelScope.launch {
            try {
                envManager.startSetup(forceDownload = true)
            } catch (e: Exception) {
                Timber.e(e, "Error starting Linux setup")
            }
        }
    }

    fun selectSession(id: String) {
        terminalController.setActiveSession(id)
    }

    fun createSession() {
        viewModelScope.launch {
            try {
                terminalController.createSession()
            } catch (e: Exception) {
                Timber.e(e, "Error creating new terminal session")
            }
        }
    }

    fun closeActiveSession() {
        activeSessionId.value?.let { terminalController.closeSession(it) }
    }

    fun sendCommand(command: String) {
        terminalController.getActiveSession()?.writeInput("$command\n")
    }

    fun sendCtrlC() {
        terminalController.getActiveSession()?.sendCtrlC()
    }

    fun sendSpecialKey(key: String) {
        val session = terminalController.getActiveSession() ?: return
        when (key) {
            "TAB" -> session.writeInput("\t")
            "ESC" -> session.writeInput("\u001B")
            "↑" -> session.writeInput("\u001B[A")
            "↓" -> session.writeInput("\u001B[B")
            "→" -> session.writeInput("\u001B[C")
            "←" -> session.writeInput("\u001B[D")
            "|" -> session.writeInput("|")
            "~" -> session.writeInput("~")
            "/" -> session.writeInput("/")
            "-" -> session.writeInput("-")
        }
    }
}
