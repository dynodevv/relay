package com.dynodevv.relay.data.repository

object ModelCapabilityDatabase {
    data class Capabilities(
        val vision: Boolean = false,
        val tools: Boolean = false,
        val reasoning: Boolean = false
    )

    private val exactMatches: Map<String, Capabilities> = buildMap {
        // OpenAI
        put("gpt-4o", Capabilities(vision = true, tools = true))
        put("gpt-4o-mini", Capabilities(vision = true, tools = true))
        put("gpt-4-turbo", Capabilities(vision = true, tools = true))
        put("gpt-4-turbo-preview", Capabilities(vision = true, tools = true))
        put("gpt-4", Capabilities(tools = true))
        put("gpt-3.5-turbo", Capabilities(tools = true))
        put("o1", Capabilities(vision = true, tools = true, reasoning = true))
        put("o1-mini", Capabilities(reasoning = true))
        put("o1-preview", Capabilities(reasoning = true))
        put("o3-mini", Capabilities(reasoning = true))
        put("o3", Capabilities(vision = true, tools = true, reasoning = true))
        put("o4-mini", Capabilities(vision = true, tools = true, reasoning = true))

        // Anthropic
        put("claude-3-5-sonnet-20241022", Capabilities(vision = true, tools = true))
        put("claude-3-5-haiku-20241022", Capabilities(vision = true, tools = true))
        put("claude-3-opus-20240229", Capabilities(vision = true, tools = true))
        put("claude-3-sonnet-20240229", Capabilities(vision = true, tools = true))
        put("claude-3-haiku-20240307", Capabilities(vision = true, tools = true))
        put("claude-3-7-sonnet-20250219", Capabilities(vision = true, tools = true, reasoning = true))

        // Google
        put("gemini-2.5-pro-preview-03-25", Capabilities(vision = true, tools = true, reasoning = true))
        put("gemini-2.0-flash", Capabilities(vision = true, tools = true))
        put("gemini-2.0-flash-thinking-exp-01-21", Capabilities(vision = true, tools = true, reasoning = true))
        put("gemini-2.0-pro-exp-02-05", Capabilities(vision = true, tools = true))
        put("gemini-1.5-pro", Capabilities(vision = true, tools = true))
        put("gemini-1.5-flash", Capabilities(vision = true, tools = true))
        put("gemini-pro-vision", Capabilities(vision = true, tools = true))

        // Groq
        put("llama-3.3-70b-versatile", Capabilities(tools = true))
        put("llama-3.1-8b-instant", Capabilities(tools = true))
        put("llama-3.1-70b-versatile", Capabilities(tools = true))
        put("mixtral-8x7b-32768", Capabilities(tools = true))
        put("mixtral-8x22b-instruct", Capabilities(tools = true))

        // DeepSeek
        put("deepseek-chat", Capabilities(tools = true))
        put("deepseek-reasoner", Capabilities(reasoning = true))

        // Cohere
        put("command-r", Capabilities(tools = true))
        put("command-r-plus", Capabilities(tools = true))
        put("command-r7b", Capabilities(tools = true))

        // xAI
        put("grok-2", Capabilities(tools = true))
        put("grok-2-vision", Capabilities(vision = true, tools = true))
        put("grok-3", Capabilities(tools = true, reasoning = true))

        // Mistral
        put("mistral-large", Capabilities(tools = true))
        put("mistral-medium", Capabilities(tools = true))
        put("pixtral-large", Capabilities(vision = true, tools = true))
        put("pixtral-12b", Capabilities(vision = true, tools = true))

        // Alibaba
        put("qwen2.5-72b-instruct", Capabilities(tools = true))
        put("qwen-max", Capabilities(tools = true))
        put("qwen-plus", Capabilities(tools = true))
        put("qwq-32b-preview", Capabilities(reasoning = true))

        // Moonshot
        put("kimi-k1-5", Capabilities(reasoning = true))
        put("kimi-k2", Capabilities(reasoning = true))

        // Meta
        put("llama-3.2-11b-vision-instruct", Capabilities(vision = true, tools = true))
        put("llama-3.2-90b-vision-instruct", Capabilities(vision = true, tools = true))
    }

    private val prefixMatches: List<Pair<String, Capabilities>> = listOf(
        // OpenAI
        "gpt-4o" to Capabilities(vision = true, tools = true),
        "gpt-4-turbo" to Capabilities(vision = true, tools = true),
        "gpt-4-" to Capabilities(tools = true),
        "o1" to Capabilities(reasoning = true),
        "o3" to Capabilities(reasoning = true),
        "o4" to Capabilities(reasoning = true),

        // Anthropic
        "claude-3-7-sonnet" to Capabilities(vision = true, tools = true, reasoning = true),
        "claude-3-5-sonnet" to Capabilities(vision = true, tools = true),
        "claude-3-5-haiku" to Capabilities(vision = true, tools = true),
        "claude-3-opus" to Capabilities(vision = true, tools = true),
        "claude-3-sonnet" to Capabilities(vision = true, tools = true),
        "claude-3-haiku" to Capabilities(vision = true, tools = true),

        // Google
        "gemini-2.5-pro" to Capabilities(vision = true, tools = true, reasoning = true),
        "gemini-2.0-flash-thinking" to Capabilities(vision = true, tools = true, reasoning = true),
        "gemini-2.0-flash" to Capabilities(vision = true, tools = true),
        "gemini-2.0-pro" to Capabilities(vision = true, tools = true),
        "gemini-1.5-pro" to Capabilities(vision = true, tools = true),
        "gemini-1.5-flash" to Capabilities(vision = true, tools = true),
        "gemini-pro-vision" to Capabilities(vision = true, tools = true),

        // Meta
        "llama-3.3" to Capabilities(tools = true),
        "llama-3.2" to Capabilities(vision = true, tools = true),
        "llama-3.1" to Capabilities(tools = true),

        // Mistral
        "mixtral-8x22b" to Capabilities(tools = true),
        "mixtral-large" to Capabilities(tools = true),
        "mistral-large" to Capabilities(tools = true),
        "mistral-medium" to Capabilities(tools = true),
        "pixtral" to Capabilities(vision = true, tools = true),

        // Alibaba
        "qwen2.5" to Capabilities(tools = true),
        "qwen-max" to Capabilities(tools = true),
        "qwen-plus" to Capabilities(tools = true),
        "qwq" to Capabilities(reasoning = true),

        // Cohere
        "command-r" to Capabilities(tools = true),
        "command-r7b" to Capabilities(tools = true),

        // xAI
        "grok-2" to Capabilities(tools = true),
        "grok-3" to Capabilities(tools = true, reasoning = true),

        // DeepSeek
        "deepseek-r" to Capabilities(reasoning = true),
        "deepseek-v3" to Capabilities(tools = true),

        // Moonshot
        "kimi-k1" to Capabilities(reasoning = true),
        "kimi-k2" to Capabilities(reasoning = true),

        // Amazon
        "nova-pro" to Capabilities(tools = true),
        "nova-premier" to Capabilities(tools = true),

        // Vision-specific
        "llava" to Capabilities(vision = true),
    )

    fun lookup(modelId: String): Capabilities? {
        val idLower = modelId.lowercase()
        exactMatches[idLower]?.let { return it }
        for ((prefix, caps) in prefixMatches) {
            if (idLower.startsWith(prefix.lowercase())) {
                return caps
            }
        }
        return null
    }
}
