package com.orbitterminal.app.data.terminal

import com.orbitterminal.app.data.db.dao.TerminalOutputDao
import com.orbitterminal.app.data.db.dao.TerminalSessionDao
import com.orbitterminal.app.data.db.entity.TerminalOutputEntity
import com.orbitterminal.app.data.db.entity.TerminalSessionEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalController @Inject constructor(
    private val envManager: LinuxEnvironmentManager,
    private val sessionDao: TerminalSessionDao,
    private val outputDao: TerminalOutputDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun getActiveSession(): TerminalSession? {
        val activeId = _activeSessionId.value ?: return null
        return _sessions.value.find { it.id == activeId }
    }

    fun setActiveSession(id: String) {
        _activeSessionId.value = id
        scope.launch { sessionDao.updateLastActive(id) }
    }

    suspend fun createSession(title: String? = null, workingDir: String = "/root"): TerminalSession =
        withContext(Dispatchers.IO) {
            if (!envManager.isEnvironmentReady()) {
                envManager.startSetup()
            }

            val id = UUID.randomUUID().toString()
            val name = title ?: "Tab ${_sessions.value.size + 1}"

            val proot = ProotProcess(
                prootBinary = envManager.prootBinary,
                rootfsDir = envManager.rootfsDir,
                homeDir = envManager.homeDir,
                workingDir = workingDir
            )
            val process = proot.start()

            val session = TerminalSession(
                id = id,
                title = name,
                process = process,
                workingDir = workingDir
            )

            startOutputReader(session)

            sessionDao.insertSession(
                TerminalSessionEntity(
                    id = id,
                    name = name,
                    workingDirectory = workingDir
                )
            )

            _sessions.value = _sessions.value + session
            _activeSessionId.value = id
            session
        }

    private fun startOutputReader(session: TerminalSession) {
        scope.launch {
            val reader = BufferedReader(InputStreamReader(session.inputStream))
            val buffer = CharArray(1024)
            var read: Int
            while (isActive && session.isAlive()) {
                try {
                    read = reader.read(buffer)
                    if (read > 0) {
                        val text = String(buffer, 0, read)
                        session.appendOutput(text)
                        outputDao.insertOutput(
                            TerminalOutputEntity(
                                sessionId = session.id,
                                output = text,
                                outputType = "stdout"
                            )
                        )
                    } else if (read == -1) break
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    suspend fun runCommandAndCapture(command: String, sessionId: String? = null): String =
        withContext(Dispatchers.IO) {
            val session = if (sessionId != null) {
                _sessions.value.find { it.id == sessionId } ?: createSession("AI Task")
            } else {
                createSession("AI Task")
            }

            val outputAccumulator = StringBuilder()
            val marker = "==DONE_${UUID.randomUUID().toString().take(8)}=="
            val fullCommand = "$command; echo \"$marker\"\n"

            session.writeInput(fullCommand)

            val startTime = System.currentTimeMillis()
            val timeoutMs = 120000L // 2 min timeout
            val reader = BufferedReader(InputStreamReader(session.inputStream))

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    if (line.contains(marker)) break
                    outputAccumulator.append(line).append("\n")
                } else {
                    delay(50)
                }
            }
            outputAccumulator.toString().trim()
        }

    fun closeSession(id: String) {
        val session = _sessions.value.find { it.id == id }
        session?.terminate()
        _sessions.value = _sessions.value.filter { it.id != id }
        if (_activeSessionId.value == id) {
            _activeSessionId.value = _sessions.value.lastOrNull()?.id
        }
        scope.launch { sessionDao.deleteSession(id) }
    }

    fun renameSession(id: String, newName: String) {
        val updated = _sessions.value.map {
            if (it.id == id) it.copy(title = newName) else it
        }
        _sessions.value = updated
        scope.launch { sessionDao.renameSession(id, newName) }
    }
}
