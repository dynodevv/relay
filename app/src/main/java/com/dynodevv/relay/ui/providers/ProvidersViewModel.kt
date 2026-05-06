package com.dynodevv.relay.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynodevv.relay.data.repository.ProviderRepository
import com.dynodevv.relay.data.secure.SecurePrefs
import com.dynodevv.relay.domain.model.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProvidersViewModel @Inject constructor(
    private val repository: ProviderRepository,
    private val securePrefs: SecurePrefs
) : ViewModel() {

    val providers = repository.getProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProvider(provider: Provider) {
        viewModelScope.launch {
            val id = repository.addProvider(provider)
            provider.apiKey?.let { securePrefs.saveApiKey(id, it) }
        }
    }

    fun editProvider(provider: Provider) {
        viewModelScope.launch {
            repository.updateProvider(provider)
            provider.apiKey?.let { securePrefs.saveApiKey(provider.id, it) }
        }
    }

    fun deleteProvider(id: Long) {
        viewModelScope.launch {
            repository.deleteProvider(id)
            securePrefs.removeApiKey(id)
        }
    }

    suspend fun testConnection(provider: Provider): TestResult {
        return try {
            val result = repository.testProviderConnection(provider)
            if (result.isSuccess) {
                TestResult.Success
            } else {
                TestResult.Error(result.exceptionOrNull()?.message ?: "Connection failed")
            }
        } catch (e: Exception) {
            TestResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class TestResult {
    data object Loading : TestResult()
    data object Success : TestResult()
    data class Error(val message: String) : TestResult()
}
