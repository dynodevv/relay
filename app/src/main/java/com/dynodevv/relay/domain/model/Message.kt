package com.dynodevv.relay.domain.model

sealed class MessageRole {
    data object User : MessageRole()
    data object Assistant : MessageRole()
    data object System : MessageRole()

    companion object {
        fun fromString(role: String): MessageRole = when (role.lowercase()) {
            "user" -> User
            "assistant" -> Assistant
            "system" -> System
            else -> User
        }
    }
}

data class Message(
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val imageUris: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isStreaming: Boolean = false
) {
    val roleString: String
        get() = when (role) {
            is MessageRole.User -> "user"
            is MessageRole.Assistant -> "assistant"
            is MessageRole.System -> "system"
        }
}
