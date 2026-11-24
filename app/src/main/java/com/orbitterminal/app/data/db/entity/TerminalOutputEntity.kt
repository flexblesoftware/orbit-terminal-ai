package com.orbitterminal.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "terminal_outputs",
    foreignKeys = [
        ForeignKey(
            entity = TerminalSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class TerminalOutputEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis(),
    val outputType: String = "stdout" // "stdout", "stderr", "stdin"
)
