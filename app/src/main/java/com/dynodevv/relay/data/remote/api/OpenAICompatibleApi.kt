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
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
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
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                val errorBody = try {
                    response.body<ChatResponseDto>().error
                } catch (_: Exception) { null }
                emit(
                    ChatResponseDto(
                        error = errorBody ?: com.dynodevv.relay.data.remote.dto.ErrorDto(
                            "HTTP ${response.status.value}: ${response.status.description}"
                        )
                    )
                )
                return@flow
            }

            val channel = response.bodyAsChannel()
            var accumulatedLine = ""
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue

                // Handle SSE format: "data: {...}"
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = json.decodeFromString<ChatResponseDto>(data)
                        emit(chunk)
                    } catch (_: Exception) {
                        // Skip malformed chunks
                    }
                    continue
                }

                // Handle raw JSON lines (some providers send JSON directly without SSE prefix)
                if (line.startsWith("{")) {
                    try {
                        val chunk = json.decodeFromString<ChatResponseDto>(line)
                        emit(chunk)
                    } catch (_: Exception) {
                        // Might be a partial JSON line, accumulate
                        accumulatedLine += line
                        try {
                            val chunk = json.decodeFromString<ChatResponseDto>(accumulatedLine)
                            emit(chunk)
                            accumulatedLine = ""
                        } catch (_: Exception) {
                            // Still partial, keep accumulating
                        }
                    }
                    continue
                }
            }
        } catch (e: Exception) {
            emit(
                ChatResponseDto(
                    error = com.dynodevv.relay.data.remote.dto.ErrorDto(e.message ?: "Unknown error")
                )
            )
        }
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
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error = try {
                    response.body<ChatResponseDto>().error
                } catch (_: Exception) { null }
                Result.failure(
                    Exception(error?.message ?: "HTTP ${response.status.value}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
