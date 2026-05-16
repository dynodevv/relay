package com.dynodevv.relay.domain.model

data class Conversation(
    val id: Long = 0,
    val title: String = "New Chat",
    val providerId: Long,
    val modelId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val systemPrompt: String? = null,
    val isArchived: Boolean = false,
    val folderId: Long? = null,
    val tags: List<Tag> = emptyList()
)
