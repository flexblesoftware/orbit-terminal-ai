package com.orbitterminal.app.data.agent

import com.orbitterminal.app.data.datastore.AppSettings
import com.orbitterminal.app.data.db.dao.AgentTaskDao
import com.orbitterminal.app.data.db.entity.AgentTaskEntity
import com.orbitterminal.app.data.terminal.TerminalController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentManager @Inject constructor(
    private val terminalController: TerminalController,
    private val antigravityLoginHandler: AntigravityLoginHandler,
    private val appSettings: AppSettings,
    private val agentTaskDao: AgentTaskDao
) {
    private val _agentStatuses = MutableStateFlow<Map<AgentId, AgentStatus>>(
        mapOf(
            AgentId.ANTIGRAVITY to AgentStatus.NotInstalled,
            AgentId.CLAUDE_CODE to AgentStatus.NotInstalled,
            AgentId.CODEX to AgentStatus.NotInstalled
        )
    )
    val agentStatuses: StateFlow<Map<AgentId, AgentStatus>> = _agentStatuses.asStateFlow()

    suspend fun refreshAllStatuses() = withContext(Dispatchers.IO) {
        for (agent in AgentId.values()) {
            checkAgentStatus(agent)
        }
    }

    suspend fun checkAgentStatus(agentId: AgentId): AgentStatus = withContext(Dispatchers.IO) {
        val status = when (agentId) {
            AgentId.ANTIGRAVITY -> {
                val versionOut = terminalController.runCommandAndCapture("agy --version 2>&1")
                if (versionOut.contains("command not found", ignoreCase = true) || versionOut.isEmpty()) {
                    AgentStatus.NotInstalled
                } else {
                    val (isLogged, email) = antigravityLoginHandler.checkStatus()
                    AgentStatus.Installed(versionOut.trim(), isLogged, email)
                }
            }
            AgentId.CLAUDE_CODE -> {
                val versionOut = terminalController.runCommandAndCapture("claude --version 2>&1")
                if (versionOut.contains("command not found", ignoreCase = true) || versionOut.isEmpty()) {
                    AgentStatus.NotInstalled
                } else {
                    val authOut = terminalController.runCommandAndCapture("claude auth status 2>&1")
                    val isLogged = !authOut.contains("not logged in", ignoreCase = true)
                    AgentStatus.Installed(versionOut.trim(), isLogged)
                }
            }
            AgentId.CODEX -> {
                val versionOut = terminalController.runCommandAndCapture("codex --version 2>&1")
                if (versionOut.contains("command not found", ignoreCase = true) || versionOut.isEmpty()) {
                    AgentStatus.NotInstalled
                } else {
                    AgentStatus.Installed(versionOut.trim(), true)
                }
            }
        }
        _agentStatuses.value = _agentStatuses.value + (agentId to status)
        status
    }

    suspend fun installAgent(agentId: AgentId): Flow<String> = flow {
        emit("Starting installation of ${agentId.displayName}...")
        _agentStatuses.value = _agentStatuses.value + (agentId to AgentStatus.Installing("Installing dependencies..."))

        when (agentId) {
            AgentId.ANTIGRAVITY -> {
                emit("Downloading Antigravity CLI...")
                val res = terminalController.runCommandAndCapture(
                    "curl -sSL https://antigravity.google.com/install.sh | bash || pip install antigravity-cli 2>&1"
                )
                emit(res)
                appSettings.setAgentInstalled(agentId.id, true)
            }
            AgentId.CLAUDE_CODE -> {
                emit("Running npm install -g @anthropic-ai/claude-code...")
                val res = terminalController.runCommandAndCapture("npm install -g @anthropic-ai/claude-code 2>&1")
                emit(res)
                appSettings.setAgentInstalled(agentId.id, true)
            }
            AgentId.CODEX -> {
                emit("Running npm install -g @openai/codex 2>&1...")
                val res = terminalController.runCommandAndCapture("npm install -g @openai/codex 2>&1")
                emit(res)
                appSettings.setAgentInstalled(agentId.id, true)
            }
        }

        checkAgentStatus(agentId)
        emit("${agentId.displayName} installation finished.")
    }

    suspend fun launchAgentSession(agentId: AgentId, workspaceDir: String = "/root/workspace"): String =
        withContext(Dispatchers.IO) {
            val session = terminalController.createSession(agentId.displayName, workspaceDir)
            val launchCmd = when (agentId) {
                AgentId.ANTIGRAVITY -> "agy\n"
                AgentId.CLAUDE_CODE -> "claude\n"
                AgentId.CODEX -> "codex\n"
            }
            session.writeInput(launchCmd)
            session.id
        }

    suspend fun recordTask(agentId: AgentId, description: String, sessionId: String? = null): String {
        val taskId = UUID.randomUUID().toString()
        agentTaskDao.insertTask(
            AgentTaskEntity(
                id = taskId,
                agentId = agentId.id,
                taskDescription = description,
                status = "running",
                sessionId = sessionId
            )
        )
        return taskId
    }
}
