package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.remote.api.OpenAICompatibleApi
import com.dynodevv.relay.data.remote.dto.ChatRequestDto
import com.dynodevv.relay.data.remote.dto.MessageDto
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.MessageRole
import com.dynodevv.relay.domain.model.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatService @Inject constructor(
    private val api: OpenAICompatibleApi,
    private val messageRepository: MessageRepository
) {
    fun streamResponse(
        provider: Provider,
        modelId: String,
        messages: List<Message>,
        conversationId: Long,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): Flow<String> {
        val request = ChatRequestDto(
            model = modelId,
            messages = messages.map {
                MessageDto(role = it.roleString, content = it.content)
            },
            stream = true,
            temperature = temperature,
            maxTokens = maxTokens
        )

        return api.streamChatCompletion(
            baseUrl = provider.apiBaseUrl,
            apiPath = provider.apiPath,
            apiKey = provider.apiKey,
            request = request
        ).map { chunk ->
            when {
                chunk.error != null -> throw Exception(chunk.error.message)
                else -> chunk.choices?.firstOrNull()?.delta?.content ?: ""
            }
        }
    }

    suspend fun generateTitle(firstUserMessage: String, provider: Provider, modelId: String): String {
        val request = ChatRequestDto(
            model = modelId,
            messages = listOf(
                MessageDto(
                    role = "system",
                    content = "Generate a very short title (max 4 words) for a chat that starts with this message. Reply with only the title, no quotes."
                ),
                MessageDto(role = "user", content = firstUserMessage)
            ),
            stream = false,
            temperature = 0.7,
            maxTokens = 20
        )

        return api.sendChatCompletion(
            baseUrl = provider.apiBaseUrl,
            apiPath = provider.apiPath,
            apiKey = provider.apiKey,
            request = request
        ).getOrNull()?.choices?.firstOrNull()?.message?.content?.trim()?.take(40)
            ?: "New Chat"
    }
}
