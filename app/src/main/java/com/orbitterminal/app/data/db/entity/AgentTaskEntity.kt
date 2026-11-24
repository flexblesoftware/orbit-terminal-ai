package com.orbitterminal.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey val id: String,
    val agentId: String, // "antigravity", "claude_code", "codex"
    val taskDescription: String,
    val status: String = "pending", // "pending","running","completed","failed","stopped"
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val outputSummary: String? = null,
    val sessionId: String? = null, // linked terminal session
    val errorMessage: String? = null
)
