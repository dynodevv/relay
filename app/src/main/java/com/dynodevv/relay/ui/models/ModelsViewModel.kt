package com.dynodevv.relay.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynodevv.relay.data.repository.ProviderRepository
import com.dynodevv.relay.domain.model.AIModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching

    private val _fetchError = MutableStateFlow<String?>(null)
    val fetchError: StateFlow<String?> = _fetchError

    fun getModels(providerId: Long): Flow<List<AIModel>> =
        repository.getModels(providerId)

    fun addModel(model: AIModel) {
        viewModelScope.launch {
            repository.addModel(model)
        }
    }

    fun editModel(model: AIModel) {
        viewModelScope.launch {
            repository.updateModel(model)
        }
    }

    fun deleteModel(id: String, providerId: Long) {
        viewModelScope.launch {
            repository.deleteModel(id, providerId)
        }
    }

    fun fetchModelsFromApi(providerId: Long) {
        viewModelScope.launch {
            _isFetching.value = true
            _fetchError.value = null
            try {
                val provider = repository.getProvider(providerId)
                if (provider == null) {
                    _fetchError.value = "Provider not found"
                    return@launch
                }
                if (provider.apiKey.isNullOrBlank()) {
                    _fetchError.value = "API key is required to fetch models"
                    return@launch
                }
                val result = repository.fetchModelsFromApi(provider)
                result.fold(
                    onSuccess = { models ->
                        if (models.isEmpty()) {
                            _fetchError.value = "No models returned from API"
                        } else {
                            models.forEach { model ->
                                repository.addModel(model)
                            }
                        }
                    },
                    onFailure = { error ->
                        _fetchError.value = error.message ?: "Failed to fetch models"
                    }
                )
            } catch (e: Exception) {
                _fetchError.value = e.message ?: "Unknown error"
            } finally {
                _isFetching.value = false
            }
        }
    }

    fun clearFetchError() {
        _fetchError.value = null
    }
}
