package com.dynodevv.relay.domain.model

data class Provider(
    val id: Long = 0,
    val name: String,
    val apiBaseUrl: String,
    val apiPath: String = "/chat/completions",
    val apiKey: String? = null,
    val isBuiltin: Boolean = false,
    val iconName: String? = null
)

val BuiltInProviders = listOf(
    Provider(
        id = 1,
        name = "OpenAI",
        apiBaseUrl = "https://api.openai.com/v1",
        isBuiltin = true,
        iconName = "openai"
    ),
    Provider(
        id = 2,
        name = "Google Gemini",
        apiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        isBuiltin = true,
        iconName = "gemini"
    ),
    Provider(
        id = 3,
        name = "Anthropic",
        apiBaseUrl = "https://api.anthropic.com/v1",
        isBuiltin = true,
        iconName = "anthropic"
    ),
    Provider(
        id = 4,
        name = "OpenRouter",
        apiBaseUrl = "https://openrouter.ai/api/v1",
        isBuiltin = true,
        iconName = "openrouter"
    ),
    Provider(
        id = 5,
        name = "Groq",
        apiBaseUrl = "https://api.groq.com/openai/v1",
        isBuiltin = true,
        iconName = "groq"
    )
)
