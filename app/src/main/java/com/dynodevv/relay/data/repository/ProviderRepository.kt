package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.AIModelDao
import com.dynodevv.relay.data.local.dao.ProviderDao
import com.dynodevv.relay.data.local.entity.AIModelEntity
import com.dynodevv.relay.data.local.entity.ProviderEntity
import com.dynodevv.relay.data.remote.api.OpenAICompatibleApi
import com.dynodevv.relay.domain.model.AIModel
import com.dynodevv.relay.domain.model.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val providerDao: ProviderDao,
    private val modelDao: AIModelDao,
    private val api: OpenAICompatibleApi
) {
    fun getProviders(): Flow<List<Provider>> =
        providerDao.getAll().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getProvider(id: Long): Provider? =
        providerDao.getById(id)?.toDomain()

    suspend fun addProvider(provider: Provider): Long {
        return providerDao.insert(provider.toEntity())
    }

    suspend fun updateProvider(provider: Provider) {
        providerDao.update(provider.toEntity())
    }

    suspend fun deleteProvider(id: Long) {
        providerDao.deleteById(id)
    }

    fun getModels(providerId: Long): Flow<List<AIModel>> =
        modelDao.getByProvider(providerId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun addModel(model: AIModel) {
        modelDao.insert(model.toEntity())
    }

    suspend fun updateModel(model: AIModel) {
        modelDao.update(model.toEntity())
    }

    suspend fun deleteModel(id: String, providerId: Long) {
        modelDao.deleteById(id, providerId)
    }

    suspend fun updateModelSortOrders(models: List<AIModel>) {
        models.forEachIndexed { index, model ->
            modelDao.updateSortOrder(model.id, model.providerId, index)
        }
    }

    suspend fun testProviderConnection(provider: Provider): Result<Unit> {
        return api.testConnection(provider.apiBaseUrl, provider.apiKey)
    }

    suspend fun fetchModelsFromApi(provider: Provider): Result<List<AIModel>> {
        val result = api.fetchModels(provider.apiBaseUrl, provider.apiKey)
        return result.map { response ->
            response.data?.map { dto ->
                val id = dto.id
                AIModel(
                    id = id,
                    providerId = provider.id,
                    displayName = dto.name ?: id,
                    supportsImageInput = detectVisionCapability(id, dto),
                    supportsTools = detectToolsCapability(id, dto),
                    supportsReasoning = detectReasoningCapability(id, dto),
                    contextLength = dto.context_length,
                    isCustom = false
                )
            } ?: emptyList()
        }
    }

    private fun detectVisionCapability(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        ModelCapabilityDatabase.lookup(modelId)?.let { return it.vision }
        val idLower = modelId.lowercase()
        if (idLower.contains("vision")) return true
        val modality = dto.architecture?.modality?.lowercase()
        if (modality != null && (modality.contains("image") || modality.contains("vision") || modality.contains("multimodal"))) return true
        return false
    }

    private fun detectToolsCapability(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        ModelCapabilityDatabase.lookup(modelId)?.let { return it.tools }
        val descLower = dto.description?.lowercase() ?: ""
        if (descLower.contains("tool") || descLower.contains("function calling") || descLower.contains("function-calling")) return true
        return false
    }

    private fun detectReasoningCapability(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        ModelCapabilityDatabase.lookup(modelId)?.let { return it.reasoning }
        val descLower = dto.description?.lowercase() ?: ""
        if (descLower.contains("reasoning") || descLower.contains("chain of thought") || descLower.contains("thinking")) return true
        return false
    }

    private fun ProviderEntity.toDomain() = Provider(
        id = id,
        name = name,
        apiBaseUrl = apiBaseUrl,
        apiPath = apiPath,
        apiKey = apiKey,
        isBuiltin = isBuiltin,
        iconName = iconName
    )

    private fun Provider.toEntity() = ProviderEntity(
        id = id,
        name = name,
        apiBaseUrl = apiBaseUrl,
        apiPath = apiPath,
        apiKey = apiKey,
        isBuiltin = isBuiltin,
        iconName = iconName
    )

    private fun AIModelEntity.toDomain() = AIModel(
        id = id,
        providerId = providerId,
        displayName = displayName,
        supportsImageInput = supportsImageInput,
        supportsTools = supportsTools,
        supportsReasoning = supportsReasoning,
        contextLength = contextLength,
        isCustom = isCustom,
        isFavorite = isFavorite,
        sortOrder = sortOrder,
        temperature = temperature,
        maxTokens = maxTokens,
        topP = topP,
        topK = topK,
        presencePenalty = presencePenalty,
        frequencyPenalty = frequencyPenalty
    )

    private fun AIModel.toEntity() = AIModelEntity(
        id = id,
        providerId = providerId,
        displayName = displayName,
        supportsImageInput = supportsImageInput,
        supportsTools = supportsTools,
        supportsReasoning = supportsReasoning,
        contextLength = contextLength,
        isCustom = isCustom,
        isFavorite = isFavorite,
        sortOrder = sortOrder,
        temperature = temperature,
        maxTokens = maxTokens,
        topP = topP,
        topK = topK,
        presencePenalty = presencePenalty,
        frequencyPenalty = frequencyPenalty
    )
}
