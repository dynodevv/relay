package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.CapabilityCacheDao
import com.dynodevv.relay.data.local.entity.CapabilityCacheEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityCacheRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val capabilityCacheDao: CapabilityCacheDao,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val LITELLM_CATALOG_URL =
            "https://raw.githubusercontent.com/BerriAI/litellm/main/model_prices_and_context_window.json"
        private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    }

    data class Capabilities(
        val vision: Boolean = false,
        val tools: Boolean = false,
        val reasoning: Boolean = false
    )

    /** In-memory cache of the DB, rebuilt after fetchAndCache(). */
    private var memoryCache: Map<String, Capabilities>? = null

    private suspend fun loadMemoryCache(): Map<String, Capabilities> {
        memoryCache?.let { return it }
        val all = capabilityCacheDao.getAll()
        val map = all.associate {
            it.modelId to Capabilities(
                vision = it.supportsVision,
                tools = it.supportsTools,
                reasoning = it.supportsReasoning
            )
        }
        memoryCache = map
        return map
    }

    suspend fun lookup(modelId: String): Capabilities? {
        val map = loadMemoryCache()
        val idLower = modelId.lowercase()

        // Strategy 1: exact match
        map[idLower]?.let { return it }

        // Strategy 2: strip everything before last '/' (provider prefix)
        // e.g. "openai/gpt-4o" -> "gpt-4o"
        //      "openrouter/anthropic/claude-3-sonnet" -> "claude-3-sonnet"
        val lastSlash = idLower.lastIndexOf('/')
        if (lastSlash != -1) {
            map[idLower.substring(lastSlash + 1)]?.let { return it }
        }

        // Strategy 3: strip everything before first '.' (bedrock style)
        // e.g. "anthropic.claude-3-sonnet-20240229-v1:0" -> "claude-3-sonnet-20240229-v1:0"
        val firstDot = idLower.indexOf('.')
        if (firstDot != -1) {
            val afterDot = idLower.substring(firstDot + 1)
            map[afterDot]?.let { return it }
            map[afterDot.replace('.', '-')]?.let { return it }
        }

        // Strategy 4: for multi-slash paths, try each suffix segment
        // e.g. "openrouter/openai/gpt-4o" -> "openai/gpt-4o" -> "gpt-4o"
        val segments = idLower.split('/')
        if (segments.size > 2) {
            for (i in 1 until segments.size - 1) {
                val suffix = segments.subList(i, segments.size).joinToString("/")
                map[suffix]?.let { return it }
            }
        }

        // Strategy 5: try stripping common provider prefixes
        val providerPrefixes = listOf(
            "anthropic.", "openai.", "google.", "mistralai.", "meta.",
            "azure/", "aws/", "bedrock/", "groq/", "together_ai/",
            "openrouter/", "ai21/", "cohere/", "xai/"
        )
        for (prefix in providerPrefixes) {
            if (idLower.startsWith(prefix)) {
                map[idLower.removePrefix(prefix)]?.let { return it }
            }
        }

        // Strategy 6: find any LiteLLM key that ends with the base name
        // e.g. provider returns "claude-3-sonnet", LiteLLM has "openrouter/anthropic/claude-3-sonnet"
        val baseName = segments.lastOrNull() ?: idLower
        map.entries.firstOrNull { it.key.endsWith(baseName) || baseName.endsWith(it.key) }?.value?.let { return it }

        return null
    }

    suspend fun shouldSync(): Boolean {
        val lastSync = settingsRepository.capabilityCacheLastSync.first()
        val autoUpdate = settingsRepository.capabilityCacheAutoUpdate.first()
        if (!autoUpdate) return false
        val cacheCount = capabilityCacheDao.getCount()
        if (cacheCount == 0) return true
        if (lastSync == 0L) return true
        return System.currentTimeMillis() - lastSync > ONE_DAY_MS
    }

    suspend fun fetchAndCache(): Result<Int> {
        return try {
            val response = httpClient.get(LITELLM_CATALOG_URL).bodyAsText()
            val json = Json.parseToJsonElement(response).jsonObject

            val entries = mutableListOf<CapabilityCacheEntity>()
            val now = System.currentTimeMillis()

            for ((modelId, element) in json) {
                if (modelId == "sample_spec") continue
                val obj = try {
                    element.jsonObject
                } catch (_: Exception) { continue }

                val mode = obj["mode"]?.jsonPrimitive?.content
                if (mode != "chat") continue

                val vision = obj["supports_vision"]?.jsonPrimitive?.booleanOrNull ?: false
                val tools = obj["supports_function_calling"]?.jsonPrimitive?.booleanOrNull ?: false
                val reasoning = obj["supports_reasoning"]?.jsonPrimitive?.booleanOrNull ?: false

                entries.add(
                    CapabilityCacheEntity(
                        modelId = modelId.lowercase(),
                        supportsVision = vision,
                        supportsTools = tools,
                        supportsReasoning = reasoning,
                        cachedAt = now
                    )
                )
            }

            capabilityCacheDao.clearAll()
            capabilityCacheDao.insertAll(entries)
            settingsRepository.setCapabilityCacheLastSync(now)
            memoryCache = null // invalidate in-memory cache

            Result.success(entries.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
