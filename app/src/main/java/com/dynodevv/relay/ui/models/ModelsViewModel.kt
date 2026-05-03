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
            try {
                val provider = repository.getProvider(providerId)
                provider?.let {
                    val result = repository.fetchModelsFromApi(it)
                    result.getOrNull()?.let { models ->
                        models.forEach { model ->
                            repository.addModel(model)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                _isFetching.value = false
            }
        }
    }
}
