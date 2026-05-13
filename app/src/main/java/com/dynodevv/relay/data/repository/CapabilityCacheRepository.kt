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

    suspend fun lookup(modelId: String): Capabilities? {
        val cached = capabilityCacheDao.getById(modelId.lowercase())
        return cached?.let {
            Capabilities(
                vision = it.supportsVision,
                tools = it.supportsTools,
                reasoning = it.supportsReasoning
            )
        }
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

            Result.success(entries.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
