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
import com.dynodevv.relay.domain.model.AIModel
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
    val currentModel: AIModel? = null,
    val availableModels: List<AIModel> = emptyList(),
    val attachedImageUris: List<String> = emptyList(),
    val editingMessageId: Long? = null,
    val hasProviders: Boolean = true,
    val navigateToConversationId: Long? = null
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
                        currentModel = defaultModel,
                        availableModels = defaultProvider?.let { p ->
                            providerRepository.getModels(p.id).first()
                        } ?: emptyList(),
                        error = null,
                        attachedImageUris = emptyList(),
                        editingMessageId = null,
                        navigateToConversationId = null
                    )
                }
            } else {
                val conversation = chatRepository.getConversation(conversationId)
                conversation?.let { conv ->
                    val provider = providerRepository.getProvider(conv.providerId)
                    val messages = messageRepository.getMessages(conv.id).first()
                    val models = provider?.let { p ->
                        providerRepository.getModels(p.id).first()
                    } ?: emptyList()
                    val currentModel = models.find { it.id == conv.modelId }
                    _uiState.update {
                        it.copy(
                            currentConversationId = conv.id,
                            conversationTitle = conv.title,
                            messages = messages,
                            currentProvider = provider,
                            currentModelId = conv.modelId,
                            currentModel = currentModel,
                            availableModels = models,
                            error = null,
                            attachedImageUris = emptyList(),
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

    fun attachImages(uris: List<String>) {
        _uiState.update { it.copy(attachedImageUris = it.attachedImageUris + uris) }
    }

    fun removeAttachedImage(index: Int) {
        _uiState.update { state ->
            val newList = state.attachedImageUris.toMutableList().apply { removeAt(index) }
            state.copy(attachedImageUris = newList)
        }
    }

    fun clearAttachedImages() {
        _uiState.update { it.copy(attachedImageUris = emptyList()) }
    }

    fun startEditingMessage(messageId: Long) {
        val message = _uiState.value.messages.find { it.id == messageId && it.role is MessageRole.User }
        message?.let {
            _uiState.update { state ->
                state.copy(
                    inputText = it.content,
                    editingMessageId = it.id,
                    attachedImageUris = it.imageUris
                )
            }
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(inputText = "", editingMessageId = null, attachedImageUris = emptyList()) }
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
        val imageUris = _uiState.value.attachedImageUris
        val imageBase64s = imageUris.map {
            if (it.startsWith("content://")) uriToBase64(it) else it
        }.filterNotNull()

        if (editingMessageId != null) {
            editInPlace(editingMessageId, text, imageBase64s)
            return
        }

        currentStreamingJob = viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null, attachedImageUris = emptyList()) }

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
                    imageUris = imageBase64s,
                    isStreaming = false
                )
                chatRepository.updateTimestamp(conversationId)

                val userMessage = Message(
                    id = userMessageId,
                    conversationId = conversationId,
                    role = MessageRole.User,
                    content = text,
                    imageUris = imageBase64s
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
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } finally {
                currentStreamingJob = null
            }
        }
    }

    private fun editInPlace(messageId: Long, newText: String, imageBase64s: List<String>) {
        currentStreamingJob = viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null, editingMessageId = null, attachedImageUris = emptyList()) }

            try {
                val conversationId = _uiState.value.currentConversationId
                val provider = _uiState.value.currentProvider ?: return@launch
                val modelId = _uiState.value.currentModelId

                // Update the edited message
                messageRepository.updateMessage(messageId, newText, imageBase64s)

                // Delete all messages after the edited one
                messageRepository.deleteMessagesAfter(conversationId, messageId)

                // Update UI: replace edited message and remove all after it
                val editedIndex = _uiState.value.messages.indexOfFirst { it.id == messageId }
                val newMessages = if (editedIndex != -1) {
                    _uiState.value.messages.take(editedIndex + 1).map { msg ->
                        if (msg.id == messageId) msg.copy(content = newText, imageUris = imageBase64s) else msg
                    }
                } else {
                    _uiState.value.messages
                }
                _uiState.update { it.copy(messages = newMessages) }

                streamAssistantResponse(conversationId, provider, modelId)
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
        _uiState.update { it.copy(messages = it.messages + assistantMessage) }

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
                error = null,
                attachedImageUris = emptyList(),
                editingMessageId = null,
                navigateToConversationId = null
            )
        }
    }

    fun deleteMessageAndAfter(messageId: Long) {
        viewModelScope.launch {
            val conversationId = _uiState.value.currentConversationId
            messageRepository.deleteMessagesAfter(conversationId, messageId)
            messageRepository.deleteMessage(messageId)
            _uiState.update { state ->
                val index = state.messages.indexOfFirst { it.id == messageId }
                val newMessages = if (index != -1) state.messages.take(index) else state.messages
                state.copy(messages = newMessages)
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
                streamAssistantResponse(conversationId, provider, modelId)
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

    fun retryLastMessage() {
        val lastAssistant = _uiState.value.messages.lastOrNull { it.role is MessageRole.Assistant && !it.isError }
        lastAssistant?.let { regenerateMessage(it.id) }
    }

    fun switchModel(modelId: String) {
        viewModelScope.launch {
            val conversationId = _uiState.value.currentConversationId
            if (conversationId != 0L) {
                chatRepository.updateModel(conversationId, modelId)
            }
            val newModel = _uiState.value.availableModels.find { it.id == modelId }
            _uiState.update { it.copy(currentModelId = modelId, currentModel = newModel) }
        }
    }

    fun setProviderAndModel(providerId: Long, modelId: String) {
        viewModelScope.launch {
            val provider = providerRepository.getProvider(providerId)
            val models = provider?.let { p ->
                providerRepository.getModels(p.id).first()
            } ?: emptyList()
            val currentModel = models.find { it.id == modelId }
            _uiState.update {
                it.copy(
                    currentProvider = provider,
                    currentModelId = modelId,
                    currentModel = currentModel,
                    availableModels = models
                )
            }
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
