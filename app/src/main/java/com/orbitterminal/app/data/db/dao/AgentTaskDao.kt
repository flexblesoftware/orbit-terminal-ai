package com.orbitterminal.app.data.db.dao

import androidx.room.*
import com.orbitterminal.app.data.db.entity.AgentTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentTaskDao {
    @Query("SELECT * FROM agent_tasks ORDER BY startedAt DESC")
    fun getAllTasks(): Flow<List<AgentTaskEntity>>

    @Query("SELECT * FROM agent_tasks WHERE agentId = :agentId ORDER BY startedAt DESC LIMIT 1")
    fun getLastTaskForAgent(agentId: String): Flow<AgentTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AgentTaskEntity)

    @Query("UPDATE agent_tasks SET status = :status, completedAt = :completedAt, outputSummary = :summary WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, completedAt: Long? = System.currentTimeMillis(), summary: String? = null)

    @Query("UPDATE agent_tasks SET status = 'stopped', completedAt = :time WHERE id = :id AND status = 'running'")
    suspend fun stopTask(id: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM agent_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}
