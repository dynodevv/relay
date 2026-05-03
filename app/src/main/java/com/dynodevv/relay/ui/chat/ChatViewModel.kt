package com.dynodevv.relay.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynodevv.relay.data.repository.ChatRepository
import com.dynodevv.relay.data.repository.ChatService
import com.dynodevv.relay.data.repository.MessageRepository
import com.dynodevv.relay.data.repository.ProviderRepository
import com.dynodevv.relay.domain.model.Conversation
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.MessageRole
import com.dynodevv.relay.domain.model.Provider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val currentConversationId: Long = 0L,
    val conversationTitle: String = "New Chat",
    val messages: List<Message> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentProvider: Provider? = null,
    val currentModelId: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val providerRepository: ProviderRepository,
    private val chatService: ChatService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private val _activeConversationId = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            chatRepository.getConversations().collectLatest { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }

        viewModelScope.launch {
            _activeConversationId
                .flatMapLatest { id ->
                    if (id == 0L) flowOf(emptyList()) else messageRepository.getMessages(id)
                }
                .collectLatest { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
        }
    }

    fun loadConversation(conversationId: Long) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            if (conversationId == 0L) {
                val providers = providerRepository.getProviders().first()
                val defaultProvider = providers.firstOrNull()
                val defaultModel = defaultProvider?.let {
                    providerRepository.getModels(it.id).first().firstOrNull()
                }
                _uiState.update {
                    it.copy(
                        currentConversationId = 0L,
                        conversationTitle = "New Chat",
                        messages = emptyList(),
                        currentProvider = defaultProvider,
                        currentModelId = defaultModel?.id ?: "",
                        error = null
                    )
                }
            } else {
                val conversation = chatRepository.getConversation(conversationId)
                conversation?.let { conv ->
                    val provider = providerRepository.getProvider(conv.providerId)
                    _uiState.update {
                        it.copy(
                            currentConversationId = conv.id,
                            conversationTitle = conv.title,
                            currentProvider = provider,
                            currentModelId = conv.modelId,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isLoading) return
        val provider = _uiState.value.currentProvider
        val modelId = _uiState.value.currentModelId

        if (provider == null || modelId.isEmpty()) {
            _uiState.update { it.copy(error = "Please configure a provider and model in settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

            try {
                var conversationId = _uiState.value.currentConversationId
                if (conversationId == 0L) {
                    conversationId = chatRepository.createConversation(provider.id, modelId)
                    _activeConversationId.value = conversationId
                    _uiState.update { it.copy(currentConversationId = conversationId) }
                }

                messageRepository.addMessage(conversationId, MessageRole.User, text)
                chatRepository.updateTimestamp(conversationId)

                // Generate title on first message
                val messages = messageRepository.getMessages(conversationId).first()
                if (messages.size == 1) {
                    val title = chatService.generateTitle(text, provider, modelId)
                    chatRepository.updateTitle(conversationId, title)
                    _uiState.update { it.copy(conversationTitle = title) }
                }

                val assistantMessageId = messageRepository.addMessage(
                    conversationId,
                    MessageRole.Assistant,
                    "",
                    isStreaming = true
                )

                val history = messageRepository.getMessages(conversationId).first()
                    .filter { !it.isError && it.id != assistantMessageId }

                var accumulated = ""
                chatService.streamResponse(
                    provider = provider,
                    modelId = modelId,
                    messages = history,
                    conversationId = conversationId
                ).collect { chunk ->
                    accumulated += chunk
                    messageRepository.updateMessageContent(assistantMessageId, accumulated, isStreaming = true)
                }

                messageRepository.updateMessageContent(assistantMessageId, accumulated, isStreaming = false)
                chatRepository.updateTimestamp(conversationId)

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun startNewChat() {
        _activeConversationId.value = 0L
        _uiState.update {
            it.copy(
                currentConversationId = 0L,
                conversationTitle = "New Chat",
                messages = emptyList(),
                inputText = "",
                error = null
            )
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun regenerateMessage(messageId: Long) {
        viewModelScope.launch {
            val conversationId = _uiState.value.currentConversationId
            val provider = _uiState.value.currentProvider ?: return@launch
            val modelId = _uiState.value.currentModelId

            messageRepository.deleteMessage(messageId)
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val assistantMessageId = messageRepository.addMessage(
                    conversationId,
                    MessageRole.Assistant,
                    "",
                    isStreaming = true
                )

                val history = messageRepository.getMessages(conversationId).first()
                    .filter { !it.isError }

                var accumulated = ""
                chatService.streamResponse(
                    provider = provider,
                    modelId = modelId,
                    messages = history,
                    conversationId = conversationId
                ).collect { chunk ->
                    accumulated += chunk
                    messageRepository.updateMessageContent(assistantMessageId, accumulated, isStreaming = true)
                }

                messageRepository.updateMessageContent(assistantMessageId, accumulated, isStreaming = false)
                chatRepository.updateTimestamp(conversationId)

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setProviderAndModel(providerId: Long, modelId: String) {
        viewModelScope.launch {
            val provider = providerRepository.getProvider(providerId)
            _uiState.update { it.copy(currentProvider = provider, currentModelId = modelId) }
        }
    }

    fun renameConversation(id: Long, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateTitle(id, newTitle)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                startNewChat()
            }
        }
    }
}
