package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.ConversationDao
import com.dynodevv.relay.data.local.entity.ConversationEntity
import com.dynodevv.relay.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao
) {
    fun getConversations(): Flow<List<Conversation>> =
        conversationDao.getAll().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getConversation(id: Long): Conversation? =
        conversationDao.getById(id)?.toDomain()

    suspend fun createConversation(providerId: Long, modelId: String, systemPrompt: String? = null): Long {
        return conversationDao.insert(
            ConversationEntity(
                title = "New Chat",
                providerId = providerId,
                modelId = modelId,
                systemPrompt = systemPrompt
            )
        )
    }

    suspend fun updateTitle(id: Long, title: String) {
        conversationDao.updateTitle(id, title)
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
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

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        providerId = providerId,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        systemPrompt = systemPrompt
    )
}
