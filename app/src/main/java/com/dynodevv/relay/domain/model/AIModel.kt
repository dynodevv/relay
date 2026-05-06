package com.dynodevv.relay.domain.model

data class AIModel(
    val id: String,
    val providerId: Long,
    val displayName: String,
    val supportsImageInput: Boolean = false,
    val supportsTools: Boolean = false,
    val supportsReasoning: Boolean = false,
    val contextLength: Int? = null,
    val isCustom: Boolean = true,
    val isFavorite: Boolean = false,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null
)
