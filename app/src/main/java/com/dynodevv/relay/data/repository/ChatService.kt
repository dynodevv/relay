package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.remote.api.OpenAICompatibleApi
import com.dynodevv.relay.data.remote.dto.ChatRequestDto
import com.dynodevv.relay.data.remote.dto.textMessageDto
import com.dynodevv.relay.data.remote.dto.visionMessageDto
import com.dynodevv.relay.domain.model.AIModel
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.Provider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
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
        systemPrompt: String? = null,
        modelParams: AIModel? = null
    ): Flow<String> = flow {
        val requestMessages = buildList {
            systemPrompt?.let {
                add(textMessageDto(role = "system", text = it))
            }
            messages.forEach { msg ->
                if (msg.imageUris.isNotEmpty()) {
                    add(visionMessageDto(role = msg.roleString, text = msg.content, imageBase64s = msg.imageUris))
                } else {
                    add(textMessageDto(role = msg.roleString, text = msg.content))
                }
            }
        }

        val request = ChatRequestDto(
            model = modelId,
            messages = requestMessages,
            stream = true,
            temperature = modelParams?.temperature,
            maxTokens = modelParams?.maxTokens,
            topP = modelParams?.topP,
            topK = modelParams?.topK,
            presencePenalty = modelParams?.presencePenalty,
            frequencyPenalty = modelParams?.frequencyPenalty
        )

        var hasEmittedContent = false
        val accumulated = StringBuilder()

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
                    val deltaContent = choice?.delta?.content
                    val messageContent = (choice?.message?.content as? kotlinx.serialization.json.JsonPrimitive)?.content
                    val content = deltaContent ?: messageContent ?: ""
                    if (content.isNotEmpty()) {
                        hasEmittedContent = true
                        accumulated.append(content)
                    }
                }
            }
        }

        if (hasEmittedContent && accumulated.isNotEmpty()) {
            val fullText = accumulated.toString()
            android.util.Log.d("RelayStream", "Streaming collected ${fullText.length} chars, emitting word-by-word")
            emitWordByWord(fullText)
        }

        if (!hasEmittedContent) {
            android.util.Log.d("RelayStream", "Streaming produced no content. Falling back to non-streaming.")
            val nonStreamRequest = request.copy(stream = false)
            val result = api.sendChatCompletion(
                baseUrl = provider.apiBaseUrl,
                apiPath = provider.apiPath,
                apiKey = provider.apiKey,
                request = nonStreamRequest
            )
            result.getOrNull()?.choices?.firstOrNull()?.message?.content?.let { contentElement ->
                val fullText = (contentElement as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                if (fullText.isNotEmpty()) {
                    android.util.Log.d("RelayStream", "Fallback: emitting ${fullText.length} chars word-by-word")
                    emitWordByWord(fullText)
                }
            }
        }
    }

    private suspend fun FlowCollector<String>.emitWordByWord(text: String) {
        var lastBoundary = 0
        for (i in text.indices) {
            val c = text[i]
            if (c == ' ' || c == '\n') {
                val end = i + 1
                emit(text.substring(lastBoundary, end))
                lastBoundary = end
                delay(12)
            }
        }
        if (lastBoundary < text.length) {
            emit(text.substring(lastBoundary))
        }
    }

    suspend fun generateTitle(firstUserMessage: String, provider: Provider, modelId: String): String {
        val request = ChatRequestDto(
            model = modelId,
            messages = listOf(
                textMessageDto(
                    role = "system",
                    text = "Generate a very short title (max 4 words) for a chat that starts with this message. Reply with only the title, no quotes."
                ),
                textMessageDto(role = "user", text = firstUserMessage)
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
        ).getOrNull()?.choices?.firstOrNull()?.message?.content?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }?.trim()?.take(40)
            ?: "New Chat"
    }
}
