package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.remote.api.OpenAICompatibleApi
import com.dynodevv.relay.data.remote.dto.ChatRequestDto
import com.dynodevv.relay.data.remote.dto.MessageDto
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.Provider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    ): Flow<String> = flow {
        val request = ChatRequestDto(
            model = modelId,
            messages = messages.map {
                MessageDto(role = it.roleString, content = it.content)
            },
            stream = true,
            temperature = temperature,
            maxTokens = maxTokens
        )

        var hasEmittedContent = false

        api.streamChatCompletion(
            baseUrl = provider.apiBaseUrl,
            apiPath = provider.apiPath,
            apiKey = provider.apiKey,
            request = request
        ).collect { chunk ->
            when {
                chunk.error != null -> throw Exception(chunk.error.message)
                else -> {
                    val choice = chunk.choices?.firstOrNull()
                    val content = choice?.delta?.content
                        ?: choice?.message?.content
                        ?: ""
                    if (content.isNotEmpty()) {
                        hasEmittedContent = true
                        emit(content)
                    }
                }
            }
        }

        // If streaming produced no content, fall back to non-streaming
        if (!hasEmittedContent) {
            android.util.Log.d("RelayStream", "Streaming produced no content. Falling back to non-streaming.")
            val nonStreamRequest = request.copy(stream = false)
            val result = api.sendChatCompletion(
                baseUrl = provider.apiBaseUrl,
                apiPath = provider.apiPath,
                apiKey = provider.apiKey,
                request = nonStreamRequest
            )
            result.getOrNull()?.choices?.firstOrNull()?.message?.content?.let { fullText ->
                if (fullText.isNotEmpty()) {
                    android.util.Log.d("RelayStream", "Fallback: simulating streaming for ${fullText.length} chars")
                    val words = fullText.split(" ")
                    for ((index, word) in words.withIndex()) {
                        emit(word + if (index < words.size - 1) " " else "")
                        delay(12) // ~80 words/sec typing effect
                    }
                }
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
