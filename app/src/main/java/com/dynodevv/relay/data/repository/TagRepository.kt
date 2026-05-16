package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.TagDao
import com.dynodevv.relay.data.local.entity.ConversationTagCrossRef
import com.dynodevv.relay.data.local.entity.TagEntity
import com.dynodevv.relay.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {
    fun getTags(): Flow<List<Tag>> =
        tagDao.getAll().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun createTag(name: String, colorHex: String): Long {
        return tagDao.insert(TagEntity(name = name, colorHex = colorHex))
    }

    suspend fun updateTag(id: Long, name: String, colorHex: String) {
        tagDao.getById(id)?.let { existing ->
            tagDao.update(existing.copy(name = name, colorHex = colorHex))
        }
    }

    suspend fun deleteTag(id: Long) {
        tagDao.deleteById(id)
    }

    suspend fun addTagToConversation(conversationId: Long, tagId: Long) {
        tagDao.addTagToConversation(ConversationTagCrossRef(conversationId, tagId))
    }

    suspend fun removeTagFromConversation(conversationId: Long, tagId: Long) {
        tagDao.removeTagFromConversation(conversationId, tagId)
    }

    fun getTagsForConversation(conversationId: Long): Flow<List<Tag>> =
        tagDao.getTagsForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    private fun TagEntity.toDomain() = Tag(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt
    )
}
