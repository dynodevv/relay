package com.dynodevv.relay.domain.model

data class AIModel(
    val id: String,
    val providerId: Long,
    val displayName: String,
    val supportsImageInput: Boolean = false,
    val supportsTools: Boolean = false,
    val supportsReasoning: Boolean = false,
    val contextLength: Int? = null,
    val isCustom: Boolean = true
)
