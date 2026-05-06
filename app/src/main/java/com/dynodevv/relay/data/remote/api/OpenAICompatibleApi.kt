package com.dynodevv.relay.data.remote.api

import com.dynodevv.relay.data.remote.dto.ChatRequestDto
import com.dynodevv.relay.data.remote.dto.ChatResponseDto
import com.dynodevv.relay.data.remote.dto.ModelsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAICompatibleApi @Inject constructor(
    private val client: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchModels(baseUrl: String, apiKey: String?): Result<ModelsResponseDto> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/models") {
                apiKey?.let { bearerAuth(it) }
                header("Accept", "application/json")
                // OpenRouter-specific headers
                header("HTTP-Referer", "https://github.com/dynodevv/relay")
                header("X-Title", "Relay")
            }
            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                try {
                    Result.success(json.decodeFromString<ModelsResponseDto>(bodyText))
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse models response: ${e.message}. Response start: ${bodyText.take(200)}"))
                }
            } else {
                val bodyText = response.bodyAsText()
                Result.failure(Exception("HTTP ${response.status.value}: ${bodyText.take(200)}"))
            }
        } catch (e: ClientRequestException) {
            Result.failure(Exception("Client error: ${e.response.status.description}"))
        } catch (e: ServerResponseException) {
            Result.failure(Exception("Server error: ${e.response.status.description}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChatCompletion(
        baseUrl: String,
        apiPath: String,
        apiKey: String?,
        request: ChatRequestDto
    ): Flow<ChatResponseDto> = flow {
        try {
            val response: HttpResponse = client.post("$baseUrl$apiPath") {
                contentType(ContentType.Application.Json)
                apiKey?.let { bearerAuth(it) }
                header("Accept", "text/event-stream")
                header("Cache-Control", "no-cache")
                header("X-Accel-Buffering", "no")
                // OpenRouter-specific headers
                header("HTTP-Referer", "https://github.com/dynodevv/relay")
                header("X-Title", "Relay")
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                val errorText = try { response.bodyAsText() } catch (_: Exception) { null }
                emit(
                    ChatResponseDto(
                        error = com.dynodevv.relay.data.remote.dto.ErrorDto(
                            "HTTP ${response.status.value}: ${errorText ?: response.status.description}"
                        )
                    )
                )
                return@flow
            }

            val channel = response.bodyAsChannel()
            val eventDataBuffer = StringBuilder()
            var lineCount = 0

            while (true) {
                val line = channel.readUTF8Line() ?: break
                lineCount++
                val trimmedLine = line.removePrefix("\uFEFF").trimEnd('\r')

                android.util.Log.d("RelaySSE", "[$lineCount] $trimmedLine")

                if (trimmedLine.isBlank()) {
                    // End of SSE event — process buffered data
                    if (eventDataBuffer.isNotEmpty()) {
                        val eventData = eventDataBuffer.toString()
                        eventDataBuffer.clear()
                        parseSseEvent(eventData)?.let { chunk ->
                            val preview = contentPreview(chunk.choices?.firstOrNull())
                            android.util.Log.d("RelaySSE", "Emitted buffered SSE chunk. Preview: $preview")
                            emit(chunk)
                        }
                    }
                    continue
                }

                // SSE data field: "data: ..." or "data:..."
                if (trimmedLine.startsWith("data:")) {
                    val data = trimmedLine.substringAfter("data:").removePrefix(" ").removePrefix("\t")

                    if (data == "[DONE]") {
                        android.util.Log.d("RelaySSE", "Received [DONE]")
                        eventDataBuffer.clear()
                        continue
                    }

                    // Most SSE events are single-line JSON — try to emit immediately
                    try {
                        val chunk = json.decodeFromString<ChatResponseDto>(data)
                        val preview = contentPreview(chunk.choices?.firstOrNull())
                        android.util.Log.d("RelaySSE", "Emitted immediate SSE chunk. Preview: $preview")
                        eventDataBuffer.clear() // Clear any stale buffer
                        emit(chunk)
                        continue
                    } catch (_: Exception) {
                        // Not valid JSON alone — buffer for multi-line event
                        if (eventDataBuffer.isNotEmpty()) {
                            eventDataBuffer.append("\n")
                        }
                        eventDataBuffer.append(data)
                        continue
                    }
                }

                // Raw JSON line (non-SSE response, e.g. provider ignored stream=true)
                if (trimmedLine.startsWith("{")) {
                    try {
                        val chunk = json.decodeFromString<ChatResponseDto>(trimmedLine)
                        android.util.Log.d("RelaySSE", "Emitted raw JSON chunk")
                        emit(chunk)
                    } catch (_: Exception) {
                        // Might be pretty-printed JSON spanning multiple lines
                        val jsonBuffer = StringBuilder(trimmedLine)
                        var jsonLines = 1
                        while (jsonLines < 100) {
                            val nextLine = channel.readUTF8Line() ?: break
                            jsonBuffer.appendLine(nextLine)
                            jsonLines++
                            try {
                                val chunk = json.decodeFromString<ChatResponseDto>(jsonBuffer.toString())
                                android.util.Log.d("RelaySSE", "Emitted multi-line JSON chunk ($jsonLines lines)")
                                emit(chunk)
                                break
                            } catch (_: Exception) {
                                // Keep accumulating until parse succeeds or limit reached
                            }
                        }
                    }
                    continue
                }

                android.util.Log.d("RelaySSE", "Ignored line: $trimmedLine")
            }

            // Process any remaining buffered event data after channel closes
            if (eventDataBuffer.isNotEmpty()) {
                parseSseEvent(eventDataBuffer.toString())?.let { chunk ->
                    val preview = contentPreview(chunk.choices?.firstOrNull())
                    android.util.Log.d("RelaySSE", "Emitted remaining SSE chunk. Preview: $preview")
                    emit(chunk)
                }
            }

            android.util.Log.d("RelaySSE", "Stream ended. Total lines: $lineCount")
        } catch (e: Exception) {
            android.util.Log.e("RelaySSE", "Streaming error", e)
            emit(
                ChatResponseDto(
                    error = com.dynodevv.relay.data.remote.dto.ErrorDto(e.message ?: "Unknown error")
                )
            )
        }
    }

    private fun parseSseEvent(eventData: String): ChatResponseDto? {
        val trimmed = eventData.trim()
        if (trimmed == "[DONE]") {
            android.util.Log.d("RelaySSE", "Received [DONE]")
            return null
        }
        return try {
            json.decodeFromString<ChatResponseDto>(trimmed)
        } catch (e: Exception) {
            android.util.Log.w("RelaySSE", "Failed to parse SSE event: $trimmed, error: ${e.message}")
            null
        }
    }

    private fun contentPreview(choice: com.dynodevv.relay.data.remote.dto.ChoiceDto?): String? {
        val deltaContent = choice?.delta?.content
        if (deltaContent != null) return deltaContent.take(30)
        val msgContent = choice?.message?.content
        return (msgContent as? kotlinx.serialization.json.JsonPrimitive)?.content?.take(30)
    }

    suspend fun sendChatCompletion(
        baseUrl: String,
        apiPath: String,
        apiKey: String?,
        request: ChatRequestDto
    ): Result<ChatResponseDto> {
        return try {
            val response: HttpResponse = client.post("$baseUrl$apiPath") {
                contentType(ContentType.Application.Json)
                apiKey?.let { bearerAuth(it) }
                // OpenRouter-specific headers
                header("HTTP-Referer", "https://github.com/dynodevv/relay")
                header("X-Title", "Relay")
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorText = try { response.bodyAsText() } catch (_: Exception) { null }
                Result.failure(
                    Exception(errorText ?: "HTTP ${response.status.value}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
