package com.dynodevv.relay.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dynodevv.relay.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllActive(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getAllArchived(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE folderId = :folderId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getByFolder(folderId: Long): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations")
    suspend fun getAllOnce(): List<ConversationEntity>

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE conversations SET modelId = :modelId WHERE id = :id")
    suspend fun updateModel(id: Long, modelId: String)

    @Query("UPDATE conversations SET systemPrompt = :systemPrompt WHERE id = :id")
    suspend fun updateSystemPrompt(id: Long, systemPrompt: String?)

    @Query("UPDATE conversations SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE conversations SET folderId = :folderId WHERE id = :id")
    suspend fun updateFolder(id: Long, folderId: Long?)

    @Query("UPDATE conversations SET isArchived = 1 WHERE id IN (:ids)")
    suspend fun archiveByIds(ids: List<Long>)

    @Query("UPDATE conversations SET isArchived = 0 WHERE id IN (:ids)")
    suspend fun unarchiveByIds(ids: List<Long>)

    @Query("DELETE FROM conversations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE conversations SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<Long>, folderId: Long?)

    @Query("""
        SELECT c.* FROM conversations c
        INNER JOIN messages m ON c.id = m.conversationId
        WHERE c.isArchived = 0 AND m.content LIKE '%' || :query || '%'
        GROUP BY c.id
        ORDER BY c.updatedAt DESC
    """)
    fun searchConversations(query: String): Flow<List<ConversationEntity>>
}
