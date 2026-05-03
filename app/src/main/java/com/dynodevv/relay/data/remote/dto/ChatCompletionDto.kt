package com.dynodevv.relay.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponseDto(
    val id: String? = null,
    val choices: List<ChoiceDto>? = null,
    val error: ErrorDto? = null
)

@Serializable
data class ChoiceDto(
    val delta: DeltaDto? = null,
    val message: MessageDto? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class DeltaDto(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ErrorDto(
    val message: String,
    val type: String? = null
)

@Serializable
data class ModelsResponseDto(
    val data: List<ModelInfoDto>? = null
)

@Serializable
data class ModelInfoDto(
    val id: String,
    val name: String? = null,
    val description: String? = null
)
