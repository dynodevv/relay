package com.dynodevv.relay.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
    val currentModelId: String = "",
    val attachedImageUri: String? = null,
    val editingMessageId: Long? = null,
    val hasProviders: Boolean = true,
    val navigateToConversationId: Long? = null,
    val isTyping: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
        viewModelScope.launch {
            providerRepository.getProviders().collectLatest { providers ->
                _uiState.update { it.copy(hasProviders = providers.isNotEmpty()) }
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
                        error = null,
                        attachedImageUri = null,
                        editingMessageId = null,
                        navigateToConversationId = null
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
                            error = null,
                            attachedImageUri = null,
                            editingMessageId = null,
                            navigateToConversationId = null
                        )
                    }
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun attachImage(uri: String) {
        _uiState.update { it.copy(attachedImageUri = uri) }
    }

    fun clearAttachedImage() {
        _uiState.update { it.copy(attachedImageUri = null) }
    }

    fun startEditingMessage(messageId: Long) {
        val message = _uiState.value.messages.find { it.id == messageId && it.role is MessageRole.User }
        message?.let {
            _uiState.update { state ->
                state.copy(
                    inputText = it.content,
                    editingMessageId = it.id,
                    attachedImageUri = it.imageUri
                )
            }
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(inputText = "", editingMessageId = null, attachedImageUri = null) }
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

        val editingMessageId = _uiState.value.editingMessageId
        val imageUri = _uiState.value.attachedImageUri
        val imageBase64 = imageUri?.let { uriToBase64(it) }

        if (editingMessageId != null) {
            editAndBranch(editingMessageId, text, imageBase64)
            return
        }

        currentStreamingJob = viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null, isTyping = true, attachedImageUri = null) }

            try {
                var conversationId = _uiState.value.currentConversationId
                if (conversationId == 0L) {
                    conversationId = chatRepository.createConversation(provider.id, modelId)
                    _uiState.update { it.copy(currentConversationId = conversationId) }
                }

                val userMessageId = messageRepository.addMessage(
                    conversationId = conversationId,
                    role = MessageRole.User,
                    content = text,
                    imageUri = imageBase64,
                    isStreaming = false
                )
                chatRepository.updateTimestamp(conversationId)

                val userMessage = Message(
                    id = userMessageId,
                    conversationId = conversationId,
                    role = MessageRole.User,
                    content = text,
                    imageUri = imageBase64
                )
                _uiState.update { it.copy(messages = it.messages + userMessage) }

                if (_uiState.value.messages.size == 1) {
                    val title = chatService.generateTitle(text, provider, modelId)
                    chatRepository.updateTitle(conversationId, title)
                    _uiState.update { it.copy(conversationTitle = title) }
                }

                streamAssistantResponse(conversationId, provider, modelId)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(isLoading = false, isTyping = false, error = e.message) }
                } else {
                    _uiState.update { it.copy(isLoading = false, isTyping = false) }
                }
            } finally {
                currentStreamingJob = null
            }
        }
    }

    private fun editAndBranch(messageId: Long, newText: String, imageBase64: String?) {
        currentStreamingJob = viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null, isTyping = true, editingMessageId = null, attachedImageUri = null) }

            try {
                val originalConversationId = _uiState.value.currentConversationId
                val provider = _uiState.value.currentProvider ?: return@launch
                val modelId = _uiState.value.currentModelId

                val originalMessages = messageRepository.getMessagesOnce(originalConversationId)
                val messageIndex = originalMessages.indexOfFirst { it.id == messageId }
                if (messageIndex == -1) {
                    _uiState.update { it.copy(isLoading = false, isTyping = false, error = "Message not found") }
                    return@launch
                }

                // Create new conversation (branch)
                val newConversationId = chatRepository.createConversation(provider.id, modelId)
                val prefixMessages = originalMessages.take(messageIndex)

                // Copy prefix messages to new conversation
                prefixMessages.forEach { msg ->
                    messageRepository.addMessage(
                        conversationId = newConversationId,
                        role = msg.role,
                        content = msg.content,
                        imageUri = msg.imageUri,
                        isStreaming = false
                    )
                }

                // Add edited message
                val editedMessageId = messageRepository.addMessage(
                    conversationId = newConversationId,
                    role = MessageRole.User,
                    content = newText,
                    imageUri = imageBase64,
                    isStreaming = false
                )
                chatRepository.updateTimestamp(newConversationId)

                // Update UI state to new conversation
                val newMessages = prefixMessages + Message(
                    id = editedMessageId,
                    conversationId = newConversationId,
                    role = MessageRole.User,
                    content = newText,
                    imageUri = imageBase64
                )
                _uiState.update {
                    it.copy(
                        currentConversationId = newConversationId,
                        conversationTitle = "New Chat",
                        messages = newMessages,
                        navigateToConversationId = newConversationId
                    )
                }

                streamAssistantResponse(newConversationId, provider, modelId)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(isLoading = false, isTyping = false, error = e.message) }
                } else {
                    _uiState.update { it.copy(isLoading = false, isTyping = false) }
                }
            } finally {
                currentStreamingJob = null
            }
        }
    }

    private suspend fun streamAssistantResponse(
        conversationId: Long,
        provider: Provider,
        modelId: String
    ) {
        val assistantMessageId = messageRepository.addMessage(
            conversationId = conversationId,
            role = MessageRole.Assistant,
            content = "",
            isStreaming = true
        )
        var assistantMessage = Message(
            id = assistantMessageId,
            conversationId = conversationId,
            role = MessageRole.Assistant,
            content = "",
            isStreaming = true
        )
        _uiState.update { it.copy(messages = it.messages + assistantMessage, isTyping = false) }

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
            assistantMessage = assistantMessage.copy(content = accumulated, isStreaming = true)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) assistantMessage else msg
                    }
                )
            }
        }

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
    }

    fun stopGeneration() {
        currentStreamingJob?.cancel()
        currentStreamingJob = null
        _uiState.update { state ->
            val lastMsg = state.messages.lastOrNull()
            if (lastMsg?.role is MessageRole.Assistant && lastMsg.isStreaming) {
                viewModelScope.launch {
                    messageRepository.updateMessageContent(lastMsg.id, lastMsg.content, isStreaming = false)
                }
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg.id == lastMsg.id) msg.copy(isStreaming = false) else msg
                    },
                    isLoading = false,
                    isTyping = false
                )
            } else {
                state.copy(isLoading = false, isTyping = false)
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
                error = null,
                attachedImageUri = null,
                editingMessageId = null,
                navigateToConversationId = null
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
                    error = null,
                    isTyping = true
                )
            }

            try {
                streamAssistantResponse(conversationId, provider, modelId)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(isLoading = false, isTyping = false, error = e.message) }
                } else {
                    _uiState.update { it.copy(isLoading = false, isTyping = false) }
                }
            } finally {
                currentStreamingJob = null
            }
        }
    }

    fun retryLastMessage() {
        val lastAssistant = _uiState.value.messages.lastOrNull { it.role is MessageRole.Assistant && !it.isError }
        lastAssistant?.let { regenerateMessage(it.id) }
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

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToConversationId = null) }
    }

    private fun uriToBase64(uriString: String): String? {
        return try {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                val bytes = input.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }
}
