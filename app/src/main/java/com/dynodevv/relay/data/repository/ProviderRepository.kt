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
    private val api: OpenAICompatibleApi,
    private val capabilityCacheRepository: CapabilityCacheRepository
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
        val response = result.getOrNull()
            ?: return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        val models = response.data?.map { dto ->
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
        return Result.success(models)
    }

    private suspend fun detectVisionCapability(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        val cached = capabilityCacheRepository.lookup(modelId)
        val heuristic = heuristicVision(modelId, dto)
        return cached?.vision == true || heuristic
    }

    private suspend fun detectToolsCapability(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        val cached = capabilityCacheRepository.lookup(modelId)
        val heuristic = heuristicTools(modelId, dto)
        return cached?.tools == true || heuristic
    }

    private suspend fun detectReasoningCapability(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        val cached = capabilityCacheRepository.lookup(modelId)
        val heuristic = heuristicReasoning(modelId, dto)
        return cached?.reasoning == true || heuristic
    }

    private fun heuristicVision(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        val idLower = modelId.lowercase()
        if (idLower.contains("vision")) return true
        if (idLower.contains("gpt-4o")) return true
        if (idLower.contains("claude-3")) return true
        if (idLower.contains("gemini-1.5") || idLower.contains("gemini-2")) return true
        if (idLower.contains("gemini-pro-vision")) return true
        if (idLower.contains("llava")) return true
        if (idLower.contains("pixtral")) return true
        if (idLower.contains("grok-2-vision")) return true
        val modality = dto.architecture?.modality?.lowercase()
        if (modality != null && (modality.contains("image") || modality.contains("vision") || modality.contains("multimodal"))) return true
        return false
    }

    private fun heuristicTools(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        val idLower = modelId.lowercase()
        val descLower = dto.description?.lowercase() ?: ""

        if (idLower.contains("gpt-4")) return true
        if (idLower.contains("claude-3")) return true
        if (idLower.contains("gemini-1.5") || idLower.contains("gemini-2") || idLower.contains("gemini-pro") || idLower.contains("gemini-ultra") || idLower.contains("gemini-flash")) return true
        if (idLower.contains("command-r") || idLower.contains("command-r7b")) return true
        if (idLower.contains("mixtral-8x22b") || idLower.contains("mixtral-large")) return true
        if (idLower.contains("qwen2.5") || idLower.contains("qwen-2.5") || idLower.contains("qwen-max") || idLower.contains("qwen-plus")) return true
        if (idLower.contains("llama-3.1") || idLower.contains("llama-3.2") || idLower.contains("llama-3.3")) return true
        if (idLower.contains("mistral-large") || idLower.contains("mistral-medium") || idLower.contains("pixtral")) return true
        if (idLower.contains("command") && idLower.contains("cohere")) return true
        if (idLower.contains("grok-2") || idLower.contains("grok2")) return true
        if (idLower.contains("nova-pro") || idLower.contains("nova-premier")) return true

        if (descLower.contains("tool") || descLower.contains("function calling") || descLower.contains("function-calling")) return true

        return false
    }

    private fun heuristicReasoning(modelId: String, dto: com.dynodevv.relay.data.remote.dto.ModelInfoDto): Boolean {
        val idLower = modelId.lowercase()
        val descLower = dto.description?.lowercase() ?: ""

        if (idLower.contains("o1") || idLower.contains("o3") || idLower.contains("o4")) return true
        if (idLower.contains("reasoning")) return true
        if (idLower.contains("deepseek-r")) return true
        if (idLower.contains("claude-3-7") || idLower.contains("claude-3.7")) return true
        if (idLower.contains("claude-3-5-sonnet") && idLower.contains("thinking")) return true
        if (idLower.contains("kimi-k1") || idLower.contains("kimi-k2")) return true
        if (idLower.contains("qwq")) return true
        if (idLower.contains("gemini-2.0-flash-thinking") || idLower.contains("gemini-2.5-pro")) return true
        if (idLower.contains("grok-3") || idLower.contains("grok3")) return true
        if (idLower.contains("deepseek-v3") && (idLower.contains("0324") || idLower.contains("2501"))) return true

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
