package com.orbitterminal.app.data.db.dao

import androidx.room.*
import com.orbitterminal.app.data.db.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChatById(id: String)

    @Query("UPDATE chats SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameChat(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE chats SET updatedAt = :updatedAt, providerId = :providerId, modelId = :modelId WHERE id = :id")
    suspend fun updateChatModel(id: String, providerId: String, modelId: String, updatedAt: Long = System.currentTimeMillis())
}
