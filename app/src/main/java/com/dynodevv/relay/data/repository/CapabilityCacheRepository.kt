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

        /** Suffixes that providers (especially OpenRouter) append to model IDs
         *  but which LiteLLM does not include in its keys. */
        private val STRIP_SUFFIXES = listOf(
            ":free", ":beta", ":extended", ":online", ":self-hosted",
            ":nano", ":fast", ":nitro", ":search-preview", ":audio-preview",
            "-fast", "-nitro", "-extended"
        )
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

        // Generate variants of the model ID to try matching
        val variants = generateLookupVariants(idLower)

        for (variant in variants) {
            map[variant]?.let { return it }
        }

        // Final fallback: find any LiteLLM key whose *base name* matches
        // our base name (handles minor version differences)
        val ourBase = baseName(idLower)
        for ((key, caps) in map) {
            if (baseName(key) == ourBase) return caps
        }

        return null
    }

    /** Produce every reasonable canonical form of a model ID for lookup. */
    private fun generateLookupVariants(idLower: String): List<String> {
        val variants = mutableSetOf<String>()

        // 1. Exact
        variants += idLower

        // 2. With openrouter/ prepended (OpenRouter API strips this prefix)
        if (!idLower.startsWith("openrouter/")) {
            variants += "openrouter/$idLower"
        }

        // 3. Strip provider-slash prefixes that LiteLLM sometimes embeds
        val providerSlashPrefixes = listOf(
            "anthropic/", "openai/", "google/", "mistralai/", "meta-llama/",
            "deepseek/", "microsoft/", "nvidia/", "qwen/", "meta/",
            "ai21/", "cohere/", "xai/", "baidu/", "nousresearch/"
        )
        for (prefix in providerSlashPrefixes) {
            if (idLower.startsWith(prefix)) {
                variants += idLower.removePrefix(prefix)
                variants += "openrouter/$idLower"
            }
        }

        // 4. Strip Bedrock dot-prefix
        val firstDot = idLower.indexOf('.')
        if (firstDot != -1) {
            val afterDot = idLower.substring(firstDot + 1)
            variants += afterDot
            variants += afterDot.replace('.', '-')
        }

        // 5. Strip known provider-added suffixes and retry all above
        val stripped = stripSuffixes(idLower)
        if (stripped != idLower) {
            variants += stripped
            variants += "openrouter/$stripped"
            for (prefix in providerSlashPrefixes) {
                if (stripped.startsWith(prefix)) {
                    variants += stripped.removePrefix(prefix)
                    variants += "openrouter/$stripped"
                }
            }
        }

        // 6. Take last path segment (base name) alone
        variants += baseName(idLower)
        variants += baseName(stripped)

        return variants.toList()
    }

    private fun stripSuffixes(id: String): String {
        var result = id
        for (suffix in STRIP_SUFFIXES) {
            if (result.endsWith(suffix)) {
                result = result.removeSuffix(suffix)
            }
        }
        return result
    }

    private fun baseName(id: String): String {
        val lastSlash = id.lastIndexOf('/')
        return if (lastSlash != -1) id.substring(lastSlash + 1) else id
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
