package com.dynodevv.relay.ui.settings

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynodevv.relay.data.repository.CapabilityCacheRepository
import com.dynodevv.relay.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val capabilityCacheRepository: CapabilityCacheRepository
) : ViewModel() {

    val themeMode = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val dynamicColors = repository.dynamicColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultProviderId = repository.defaultProviderId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultModelId = repository.defaultModelId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val globalSystemPrompt = repository.globalSystemPrompt
        .map { it ?: RelayDefaultSystemPrompt }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RelayDefaultSystemPrompt)

    val capabilityCacheAutoUpdate = repository.capabilityCacheAutoUpdate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isSyncingCapabilities = mutableStateOf(false)
    val isSyncingCapabilities: Boolean get() = _isSyncingCapabilities.value

    private val _capabilitySyncResult = mutableStateOf<String?>(null)
    val capabilitySyncResult: String? get() = _capabilitySyncResult.value

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColors(enabled)
        }
    }

    fun setGlobalSystemPrompt(prompt: String?) {
        viewModelScope.launch {
            repository.setGlobalSystemPrompt(prompt)
        }
    }

    fun setCapabilityCacheAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCapabilityCacheAutoUpdate(enabled)
        }
    }

    fun syncCapabilityCache() {
        viewModelScope.launch {
            _isSyncingCapabilities.value = true
            _capabilitySyncResult.value = null
            val result = capabilityCacheRepository.fetchAndCache()
            _isSyncingCapabilities.value = false
            _capabilitySyncResult.value = result.fold(
                onSuccess = { "Updated $it models" },
                onFailure = { "Update failed: ${it.message}" }
            )
        }
    }

    fun clearCapabilitySyncResult() {
        _capabilitySyncResult.value = null
    }
}

val RelayDefaultSystemPrompt = """You are Relay, a digital assistant in the Relay app. Never identify as an AI, LLM, chatbot, or virtual assistant. Always say you are Relay.

LANGUAGE: Respond in the same language as the user's message. Do not default to Chinese. Switch languages only if the user explicitly requests it.

STYLE: Be concise. Match the user's tone. Use markdown formatting.

CODE: Fenced blocks with correct language tags. Explain what code does, not just provide it. For debugging, explain the root cause.

ACCURACY: If unsure, say so. Distinguish facts from opinions. Ask clarifying questions for ambiguous queries.

REASONING: Show step-by-step reasoning for complex problems. Consider edge cases."""
