package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.MessageDao
import com.dynodevv.relay.data.local.entity.MessageEntity
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    fun getMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.getByConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getMessagesOnce(conversationId: Long): List<Message> =
        messageDao.getByConversationOnce(conversationId).map { it.toDomain() }

    suspend fun addMessage(conversationId: Long, role: MessageRole, content: String, imageUris: List<String> = emptyList(), isStreaming: Boolean = false): Long {
        return messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                role = roleString(role),
                content = content,
                imageUris = imageUris.joinToString(","),
                isStreaming = isStreaming
            )
        )
    }

    suspend fun updateMessageContent(id: Long, content: String, isStreaming: Boolean = false) {
        messageDao.updateContent(id, content, isStreaming)
    }

    suspend fun updateMessage(id: Long, content: String, imageUris: List<String>) {
        messageDao.updateMessage(id, content, imageUris.joinToString(","))
    }

    suspend fun deleteMessage(id: Long) {
        messageDao.deleteById(id)
    }

    suspend fun deleteMessagesAfter(conversationId: Long, messageId: Long) {
        messageDao.deleteMessagesAfter(conversationId, messageId)
    }

    suspend fun deleteMessagesByConversation(conversationId: Long) {
        messageDao.deleteByConversation(conversationId)
    }

    private fun MessageEntity.toDomain() = Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.fromString(role),
        content = content,
        imageUris = imageUris.takeIf { it.isNotBlank() }?.split(",") ?: emptyList(),
        createdAt = createdAt,
        isError = isError,
        isStreaming = isStreaming
    )

    private fun roleString(role: MessageRole): String = when (role) {
        is MessageRole.User -> "user"
        is MessageRole.Assistant -> "assistant"
        is MessageRole.System -> "system"
    }
}
