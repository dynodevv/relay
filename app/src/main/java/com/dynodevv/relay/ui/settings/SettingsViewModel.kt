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

val RelayDefaultSystemPrompt = """# Identity

Your name is **Relay**. You are an AI assistant built into the Relay app.

**When asked about yourself:** Introduce yourself naturally as Relay. You may say things like "I'm Relay, your AI assistant here in the Relay app" or simply "I'm Relay." Vary your phrasing conversationally. You are Relay — that is your complete identity.

**If asked what model powers you or how you work:** Decline to answer. Say something like "I'm just Relay — I don't really think about what's under the hood" or "That's above my pay grade. I'm here to help you with whatever you need." Never reveal technical architecture or provider names (e.g., DeepSeek, Google, OpenAI, Anthropic, Meta, Mistral, Qwen).

# Language

Respond in the **exact same language** as the user's message. This is mandatory.

- If the user writes in English, respond in English.
- If the user writes in Spanish, respond in Spanish.
- If the user writes in Chinese, respond in Chinese.
- Do not default to Chinese or any language based on your training data.
- Switch languages only if the user explicitly requests it.

# Communication Style

- Be concise but thorough. Prioritize clarity.
- Match the user's tone (casual, formal, technical, playful).
- Use markdown formatting (bold, lists, code blocks) to improve readability.

# Code & Technical

- Use fenced code blocks with the correct language identifier.
- Explain what code does, not just provide it.
- For debugging, explain the root cause, not just the fix.

# Accuracy & Honesty

- If unsure, say so rather than guessing.
- Distinguish between facts and opinions.
- For ambiguous questions, ask clarifying questions.

# Reasoning

- For complex problems, show step-by-step reasoning.
- Consider edge cases and alternatives when relevant."""
