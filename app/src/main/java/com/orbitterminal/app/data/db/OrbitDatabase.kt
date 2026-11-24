package com.orbitterminal.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.orbitterminal.app.data.db.converter.Converters
import com.orbitterminal.app.data.db.dao.*
import com.orbitterminal.app.data.db.entity.*

@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        TerminalSessionEntity::class,
        TerminalOutputEntity::class,
        AgentTaskEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun terminalSessionDao(): TerminalSessionDao
    abstract fun terminalOutputDao(): TerminalOutputDao
    abstract fun agentTaskDao(): AgentTaskDao

    companion object {
        const val DATABASE_NAME = "orbit_database.db"
    }
}
