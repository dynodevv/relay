package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.ConversationDao
import com.dynodevv.relay.data.local.dao.TagDao
import com.dynodevv.relay.data.local.entity.ConversationEntity
import com.dynodevv.relay.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val tagDao: TagDao
) {
    fun getConversations(): Flow<List<Conversation>> =
        conversationDao.getAllActive().map { list ->
            list.map { it.toDomainWithTags() }
        }

    fun getArchivedConversations(): Flow<List<Conversation>> =
        conversationDao.getAllArchived().map { list ->
            list.map { it.toDomainWithTags() }
        }

    fun getConversationsByFolder(folderId: Long): Flow<List<Conversation>> =
        conversationDao.getByFolder(folderId).map { list ->
            list.map { it.toDomainWithTags() }
        }

    fun searchConversations(query: String): Flow<List<Conversation>> =
        conversationDao.searchConversations(query).map { list ->
            list.map { it.toDomainWithTags() }
        }

    suspend fun getConversation(id: Long): Conversation? =
        conversationDao.getById(id)?.toDomainWithTags()

    suspend fun createConversation(providerId: Long, modelId: String, systemPrompt: String? = null, folderId: Long? = null): Long {
        return conversationDao.insert(
            ConversationEntity(
                title = "New Chat",
                providerId = providerId,
                modelId = modelId,
                systemPrompt = systemPrompt,
                folderId = folderId
            )
        )
    }

    suspend fun updateTitle(id: Long, title: String) {
        conversationDao.updateTitle(id, title)
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    suspend fun deleteConversations(ids: List<Long>) {
        conversationDao.deleteByIds(ids)
    }

    suspend fun updateTimestamp(id: Long) {
        conversationDao.updateTimestamp(id)
    }

    suspend fun updateModel(id: Long, modelId: String) {
        conversationDao.updateModel(id, modelId)
    }

    suspend fun updateSystemPrompt(id: Long, systemPrompt: String?) {
        conversationDao.updateSystemPrompt(id, systemPrompt)
    }

    suspend fun archiveConversation(id: Long) {
        conversationDao.updateArchived(id, true)
    }

    suspend fun unarchiveConversation(id: Long) {
        conversationDao.updateArchived(id, false)
    }

    suspend fun archiveConversations(ids: List<Long>) {
        conversationDao.archiveByIds(ids)
    }

    suspend fun unarchiveConversations(ids: List<Long>) {
        conversationDao.unarchiveByIds(ids)
    }

    suspend fun moveToFolder(id: Long, folderId: Long?) {
        conversationDao.updateFolder(id, folderId)
    }

    suspend fun moveToFolder(ids: List<Long>, folderId: Long?) {
        conversationDao.moveToFolder(ids, folderId)
    }

    private suspend fun ConversationEntity.toDomainWithTags(): Conversation {
        val tags = tagDao.getTagsForConversationOnce(id)
        return Conversation(
            id = id,
            title = title,
            providerId = providerId,
            modelId = modelId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            systemPrompt = systemPrompt,
            isArchived = isArchived,
            folderId = folderId,
            tags = tags.map { com.dynodevv.relay.domain.model.Tag(it.id, it.name, it.colorHex, it.createdAt) }
        )
    }
}
