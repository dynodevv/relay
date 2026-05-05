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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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

    private var currentStreamingJob: Job? = null

    init {
        viewModelScope.launch {
            chatRepository.getConversations().collectLatest { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
    }

    fun loadConversation(conversationId: Long) {
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
                    val messages = messageRepository.getMessages(conv.id).first()
                    _uiState.update {
                        it.copy(
                            currentConversationId = conv.id,
                            conversationTitle = conv.title,
                            messages = messages,
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

        currentStreamingJob = viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

            try {
                var conversationId = _uiState.value.currentConversationId
                if (conversationId == 0L) {
                    conversationId = chatRepository.createConversation(provider.id, modelId)
                    _uiState.update { it.copy(currentConversationId = conversationId) }
                }

                val userMessageId = messageRepository.addMessage(conversationId, MessageRole.User, text)
                chatRepository.updateTimestamp(conversationId)

                // Add user message to UI immediately
                val userMessage = Message(
                    id = userMessageId,
                    conversationId = conversationId,
                    role = MessageRole.User,
                    content = text
                )
                _uiState.update { it.copy(messages = it.messages + userMessage) }

                // Generate title on first message
                if (_uiState.value.messages.size == 1) {
                    val title = chatService.generateTitle(text, provider, modelId)
                    chatRepository.updateTitle(conversationId, title)
                    _uiState.update { it.copy(conversationTitle = title) }
                }

                // Create assistant message in DB and UI
                val assistantMessageId = messageRepository.addMessage(
                    conversationId,
                    MessageRole.Assistant,
                    "",
                    isStreaming = true
                )
                var assistantMessage = Message(
                    id = assistantMessageId,
                    conversationId = conversationId,
                    role = MessageRole.Assistant,
                    content = "",
                    isStreaming = true
                )
                _uiState.update { it.copy(messages = it.messages + assistantMessage) }

                val history = messageRepository.getMessages(conversationId).first()
                    .filter { !it.isError && it.id != assistantMessageId }

                var accumulated = ""
                var chunkCount = 0
                chatService.streamResponse(
                    provider = provider,
                    modelId = modelId,
                    messages = history,
                    conversationId = conversationId
                ).collect { chunk ->
                    chunkCount++
                    accumulated += chunk
                    assistantMessage = assistantMessage.copy(content = accumulated, isStreaming = true)
                    android.util.Log.d("RelayUI", "Chunk #$chunkCount received (${chunk.length} chars). Total: ${accumulated.length}")
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) assistantMessage else msg
                            }
                        )
                    }
                    delay(16) // Force at least one frame render between chunks
                }
                android.util.Log.d("RelayUI", "Streaming complete. $chunkCount chunks, ${accumulated.length} total chars")

                // Final DB write only after streaming completes
                messageRepository.updateMessageContent(assistantMessageId, accumulated, isStreaming = false)
                assistantMessage = assistantMessage.copy(content = accumulated, isStreaming = false)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) assistantMessage else msg
                        },
                        isLoading = false
                    )
                }
                chatRepository.updateTimestamp(conversationId)
            } catch (e: Exception) {
                // Don't show cancellation as an error
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } finally {
                currentStreamingJob = null
            }
        }
    }

    fun stopGeneration() {
        currentStreamingJob?.cancel()
        currentStreamingJob = null
        // Mark the last assistant message as no longer streaming
        _uiState.update { state ->
            val lastMsg = state.messages.lastOrNull()
            if (lastMsg?.role is MessageRole.Assistant && lastMsg.isStreaming) {
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg.id == lastMsg.id) msg.copy(isStreaming = false) else msg
                    },
                    isLoading = false
                )
            } else {
                state.copy(isLoading = false)
            }
        }
    }

    fun startNewChat() {
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
            _uiState.update { state ->
                state.copy(messages = state.messages.filter { it.id != messageId })
            }
        }
    }

    fun regenerateMessage(messageId: Long) {
        currentStreamingJob = viewModelScope.launch {
            val conversationId = _uiState.value.currentConversationId
            val provider = _uiState.value.currentProvider ?: return@launch
            val modelId = _uiState.value.currentModelId

            messageRepository.deleteMessage(messageId)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.filter { it.id != messageId },
                    isLoading = true,
                    error = null
                )
            }

            try {
                val assistantMessageId = messageRepository.addMessage(
                    conversationId,
                    MessageRole.Assistant,
                    "",
                    isStreaming = true
                )
                var assistantMessage = Message(
                    id = assistantMessageId,
                    conversationId = conversationId,
                    role = MessageRole.Assistant,
                    content = "",
                    isStreaming = true
                )
                _uiState.update { it.copy(messages = it.messages + assistantMessage) }

                val history = messageRepository.getMessages(conversationId).first()
                    .filter { !it.isError }

                var accumulated = ""
                var chunkCount = 0
                chatService.streamResponse(
                    provider = provider,
                    modelId = modelId,
                    messages = history,
                    conversationId = conversationId
                ).collect { chunk ->
                    chunkCount++
                    accumulated += chunk
                    assistantMessage = assistantMessage.copy(content = accumulated, isStreaming = true)
                    android.util.Log.d("RelayUI", "Chunk #$chunkCount received (${chunk.length} chars). Total: ${accumulated.length}")
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) assistantMessage else msg
                            }
                        )
                    }
                    delay(16) // Force at least one frame render between chunks
                }
                android.util.Log.d("RelayUI", "Streaming complete. $chunkCount chunks, ${accumulated.length} total chars")

                messageRepository.updateMessageContent(assistantMessageId, accumulated, isStreaming = false)
                assistantMessage = assistantMessage.copy(content = accumulated, isStreaming = false)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) assistantMessage else msg
                        },
                        isLoading = false
                    )
                }
                chatRepository.updateTimestamp(conversationId)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } finally {
                currentStreamingJob = null
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
