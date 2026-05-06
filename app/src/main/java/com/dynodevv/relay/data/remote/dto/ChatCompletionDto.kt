package com.dynodevv.relay.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("top_k")
    val topK: Int? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Double? = null,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Double? = null
)

@Serializable
data class MessageDto(
    val role: String,
    val content: JsonElement
)

fun textMessageDto(role: String, text: String): MessageDto =
    MessageDto(role = role, content = JsonPrimitive(text))

fun visionMessageDto(role: String, text: String, imageBase64s: List<String>): MessageDto =
    MessageDto(
        role = role,
        content = buildJsonArray {
            add(buildJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive(text))
            })
            imageBase64s.forEach { base64 ->
                add(buildJsonObject {
                    put("type", JsonPrimitive("image_url"))
                    put("image_url", buildJsonObject {
                        put("url", JsonPrimitive("data:image/jpeg;base64,$base64"))
                    })
                })
            }
        }
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
    val description: String? = null,
    val context_length: Int? = null,
    val architecture: ModelArchitectureDto? = null
)

@Serializable
data class ModelArchitectureDto(
    val modality: String? = null
)
