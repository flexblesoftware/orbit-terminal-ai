package com.orbitterminal.app.data.db.dao

import androidx.room.*
import com.orbitterminal.app.data.db.entity.TerminalSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalSessionDao {
    @Query("SELECT * FROM terminal_sessions ORDER BY lastActiveAt DESC")
    fun getAllSessions(): Flow<List<TerminalSessionEntity>>

    @Query("SELECT * FROM terminal_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): TerminalSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TerminalSessionEntity)

    @Query("UPDATE terminal_sessions SET lastActiveAt = :time WHERE id = :id")
    suspend fun updateLastActive(id: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE terminal_sessions SET name = :name WHERE id = :id")
    suspend fun renameSession(id: String, name: String)

    @Query("DELETE FROM terminal_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("UPDATE terminal_sessions SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean)
}
