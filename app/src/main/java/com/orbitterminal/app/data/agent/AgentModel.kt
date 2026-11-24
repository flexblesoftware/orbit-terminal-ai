package com.orbitterminal.app.data.agent

enum class AgentId(val id: String, val displayName: String, val description: String) {
    ANTIGRAVITY("antigravity", "Antigravity CLI", "Google's autonomous coding and research agent CLI"),
    CLAUDE_CODE("claude_code", "Claude Code", "Anthropic's interactive agentic coding CLI"),
    CODEX("codex", "OpenAI Codex CLI", "OpenAI's terminal coding assistant CLI")
}

sealed class AgentStatus {
    object NotInstalled : AgentStatus()
    data class Installing(val progress: String) : AgentStatus()
    data class Installed(
        val version: String,
        val isLoggedIn: Boolean,
        val userEmail: String? = null
    ) : AgentStatus()
    data class Error(val message: String) : AgentStatus()
}

data class AgentInfo(
    val id: AgentId,
    val status: AgentStatus,
    val lastTaskSummary: String? = null,
    val lastTaskStatus: String? = null
)
