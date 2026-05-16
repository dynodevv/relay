package com.dynodevv.relay.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dynodevv.relay.data.local.entity.ConversationTagCrossRef
import com.dynodevv.relay.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllOnce(): List<TagEntity>

    @Query("SELECT * FROM conversation_tags")
    suspend fun getAllCrossRefs(): List<ConversationTagCrossRef>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun addTagToConversation(crossRef: ConversationTagCrossRef)

    @Query("DELETE FROM conversation_tags WHERE conversationId = :conversationId AND tagId = :tagId")
    suspend fun removeTagFromConversation(conversationId: Long, tagId: Long)

    @Query("DELETE FROM conversation_tags WHERE conversationId = :conversationId")
    suspend fun removeAllTagsFromConversation(conversationId: Long)

    @Transaction
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN conversation_tags ON tags.id = conversation_tags.tagId
        WHERE conversation_tags.conversationId = :conversationId
    """)
    fun getTagsForConversation(conversationId: Long): Flow<List<TagEntity>>

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN conversation_tags ON tags.id = conversation_tags.tagId
        WHERE conversation_tags.conversationId = :conversationId
    """)
    suspend fun getTagsForConversationOnce(conversationId: Long): List<TagEntity>
}
