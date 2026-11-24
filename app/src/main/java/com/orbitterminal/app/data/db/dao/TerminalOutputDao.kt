package com.orbitterminal.app.data.db.dao

import androidx.room.*
import com.orbitterminal.app.data.db.entity.TerminalOutputEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalOutputDao {
    @Query("SELECT * FROM terminal_outputs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getOutputsForSession(sessionId: String): Flow<List<TerminalOutputEntity>>

    @Insert
    suspend fun insertOutput(output: TerminalOutputEntity)

    @Query("DELETE FROM terminal_outputs WHERE sessionId = :sessionId")
    suspend fun clearOutputsForSession(sessionId: String)

    @Query("SELECT COUNT(*) FROM terminal_outputs WHERE sessionId = :sessionId")
    suspend fun getOutputCount(sessionId: String): Int
}
