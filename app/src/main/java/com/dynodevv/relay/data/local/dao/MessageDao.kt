package com.dynodevv.relay.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dynodevv.relay.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getByConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getByConversationOnce(conversationId: Long): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)

    @Query("UPDATE messages SET content = :content, isStreaming = :isStreaming WHERE id = :id")
    suspend fun updateContent(id: Long, content: String, isStreaming: Boolean = false)

    @Query("UPDATE messages SET content = :content, imageUris = :imageUris WHERE id = :id")
    suspend fun updateMessage(id: Long, content: String, imageUris: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND createdAt > (SELECT createdAt FROM messages WHERE id = :messageId)")
    suspend fun deleteMessagesAfter(conversationId: Long, messageId: Long)
}
