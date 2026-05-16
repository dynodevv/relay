package com.dynodevv.relay.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynodevv.relay.data.repository.BackupRepository
import com.dynodevv.relay.data.repository.ChatRepository
import com.dynodevv.relay.data.repository.ChatService
import com.dynodevv.relay.data.repository.MessageRepository
import com.dynodevv.relay.data.repository.ProviderRepository
import com.dynodevv.relay.data.repository.SettingsRepository
import com.dynodevv.relay.data.repository.TagRepository
import com.dynodevv.relay.data.repository.TemplateRepository
import com.dynodevv.relay.ui.settings.RelayDefaultSystemPrompt
import com.dynodevv.relay.domain.model.AIModel
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.MessageRole
import com.dynodevv.relay.domain.model.Provider
import com.dynodevv.relay.domain.model.Tag
import com.dynodevv.relay.domain.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ChatUiState(
    val currentConversationId: Long = 0L,
    val conversationTitle: String = "New Chat",
    val messages: List<Message> = emptyList(),
    val conversations: List<com.dynodevv.relay.domain.model.Conversation> = emptyList(),
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
    val navigateToConversationId: Long? = null,
    // Phase 3: Organization
    val tags: List<Tag> = emptyList(),
    val templates: List<Template> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val messageSearchQuery: String = "",
    val messageSearchResults: List<Message> = emptyList(),
    val isMessageSearchActive: Boolean = false,
    val exportResult: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val providerRepository: ProviderRepository,
    private val chatService: ChatService,
    private val settingsRepository: SettingsRepository,
    private val tagRepository: TagRepository,
    private val templateRepository: TemplateRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var currentStreamingJob: Job? = null
    private var conversationsCollectionJob: Job? = null

    init {
        startConversationsCollection { chatRepository.getConversations() }
        viewModelScope.launch {
            providerRepository.getProviders().collectLatest { providers ->
                _uiState.update { it.copy(hasProviders = providers.isNotEmpty()) }
            }
        }
        viewModelScope.launch {
            tagRepository.getTags().collectLatest { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
        viewModelScope.launch {
            templateRepository.getTemplates().collectLatest { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    private fun startConversationsCollection(flowProvider: () -> kotlinx.coroutines.flow.Flow<List<com.dynodevv.relay.domain.model.Conversation>>) {
        conversationsCollectionJob?.cancel()
        conversationsCollectionJob = viewModelScope.launch {
            flowProvider().collectLatest { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            if (conversationId == 0L) {
                val providers = providerRepository.getProviders().first()
                val defaultProviderId = settingsRepository.defaultProviderId.first()
                val defaultModelId = settingsRepository.defaultModelId.first()

                val provider = if (defaultProviderId != null) {
                    providers.find { it.id == defaultProviderId } ?: providers.firstOrNull()
                } else {
                    providers.firstOrNull()
                }

                val models = provider?.let { p ->
                    providerRepository.getModels(p.id).first()
                } ?: emptyList()

                val defaultModel = if (defaultModelId != null && provider != null) {
                    models.find { it.id == defaultModelId }
                } else {
                    models.firstOrNull()
                }

                _uiState.update {
                    it.copy(
                        currentConversationId = 0L,
                        conversationTitle = "New Chat",
                        messages = emptyList(),
                        currentProvider = provider,
                        currentModelId = defaultModel?.id ?: "",
                        currentModel = defaultModel,
                        availableModels = models,
                        error = null,
                        attachedImageUris = emptyList(),
                        editingMessageId = null,
                        navigateToConversationId = null,
                        messageSearchQuery = "",
                        messageSearchResults = emptyList(),
                        isMessageSearchActive = false
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
                            navigateToConversationId = null,
                            messageSearchQuery = "",
                            messageSearchResults = emptyList(),
                            isMessageSearchActive = false
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
        viewModelScope.launch {
            val filePaths = uris.mapNotNull { copyUriToFile(it) }
            _uiState.update { it.copy(attachedImageUris = it.attachedImageUris + filePaths) }
        }
    }

    private fun copyUriToFile(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val dir = File(context.filesDir, "attached_images").apply { mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}_${(0..9999).random()}.jpg"
            val file = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
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
        val imagePaths = _uiState.value.attachedImageUris

        if (editingMessageId != null) {
            editInPlace(editingMessageId, text, imagePaths)
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
                    imageUris = imagePaths,
                    isStreaming = false
                )
                chatRepository.updateTimestamp(conversationId)

                val userMessage = com.dynodevv.relay.domain.model.Message(
                    id = userMessageId,
                    conversationId = conversationId,
                    role = MessageRole.User,
                    content = text,
                    imageUris = imagePaths
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

    private fun editInPlace(messageId: Long, newText: String, imagePaths: List<String>) {
        currentStreamingJob = viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isLoading = true, error = null, editingMessageId = null, attachedImageUris = emptyList()) }

            try {
                val conversationId = _uiState.value.currentConversationId
                val provider = _uiState.value.currentProvider ?: return@launch
                val modelId = _uiState.value.currentModelId

                messageRepository.updateMessage(messageId, newText, imagePaths)
                messageRepository.deleteMessagesAfter(conversationId, messageId)

                val editedIndex = _uiState.value.messages.indexOfFirst { it.id == messageId }
                val newMessages = if (editedIndex != -1) {
                    _uiState.value.messages.take(editedIndex + 1).map { msg ->
                        if (msg.id == messageId) msg.copy(content = newText, imageUris = imagePaths) else msg
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
        var assistantMessage = com.dynodevv.relay.domain.model.Message(
            id = assistantMessageId,
            conversationId = conversationId,
            role = MessageRole.Assistant,
            content = "",
            isStreaming = true
        )
        _uiState.update { it.copy(messages = it.messages + assistantMessage) }

        val history = messageRepository.getMessages(conversationId).first()
            .filter { !it.isError && it.id != assistantMessageId }
            .map { msg ->
                if (msg.imageUris.isNotEmpty()) {
                    msg.copy(imageUris = msg.imageUris.mapNotNull { pathToBase64(it) })
                } else msg
            }

        val systemPrompt = settingsRepository.globalSystemPrompt.first() ?: RelayDefaultSystemPrompt
        var accumulated = ""
        val currentModel = _uiState.value.currentModel
        chatService.streamResponse(
            provider = provider,
            modelId = modelId,
            messages = history,
            conversationId = conversationId,
            systemPrompt = systemPrompt,
            modelParams = currentModel
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
                navigateToConversationId = null,
                messageSearchQuery = "",
                messageSearchResults = emptyList(),
                isMessageSearchActive = false
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

    fun archiveConversation(id: Long) {
        viewModelScope.launch {
            chatRepository.archiveConversation(id)
            if (_uiState.value.currentConversationId == id) {
                startNewChat()
            }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToConversationId = null) }
    }

    // --- Phase 3: Search ---

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            startConversationsCollection { chatRepository.getConversations() }
            _uiState.update { it.copy(isSearchActive = false) }
            return
        }
        startConversationsCollection { chatRepository.searchConversations(query) }
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearchActive = false) }
        startConversationsCollection { chatRepository.getConversations() }
    }

    // --- Phase 3: Message Search ---

    fun toggleMessageSearch() {
        val currentlyActive = _uiState.value.isMessageSearchActive
        if (currentlyActive) {
            clearMessageSearch()
        } else {
            _uiState.update { it.copy(isMessageSearchActive = true) }
        }
    }

    fun setMessageSearchQuery(query: String) {
        _uiState.update { it.copy(messageSearchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(messageSearchResults = emptyList()) }
            return
        }
        val conversationId = _uiState.value.currentConversationId
        if (conversationId == 0L) return
        viewModelScope.launch {
            messageRepository.searchMessages(conversationId, query).collectLatest { messages ->
                _uiState.update { it.copy(messageSearchResults = messages, isMessageSearchActive = true) }
            }
        }
    }

    fun clearMessageSearch() {
        _uiState.update { it.copy(messageSearchQuery = "", messageSearchResults = emptyList(), isMessageSearchActive = false) }
    }

    // --- Phase 3: Tags ---

    fun createTag(name: String, colorHex: String) {
        viewModelScope.launch {
            tagRepository.createTag(name, colorHex)
        }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch {
            tagRepository.deleteTag(id)
        }
    }

    fun addTagToConversation(conversationId: Long, tagId: Long) {
        viewModelScope.launch {
            tagRepository.addTagToConversation(conversationId, tagId)
        }
    }

    fun removeTagFromConversation(conversationId: Long, tagId: Long) {
        viewModelScope.launch {
            tagRepository.removeTagFromConversation(conversationId, tagId)
        }
    }

    // --- Phase 3: Templates ---

    fun createTemplate(name: String, content: String) {
        viewModelScope.launch {
            templateRepository.createTemplate(name, content)
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(id)
        }
    }

    fun applyTemplate(content: String) {
        _uiState.update { it.copy(inputText = content) }
    }

    // --- Phase 3: Export ---

    fun exportConversationToMarkdown() {
        val conversationId = _uiState.value.currentConversationId
        if (conversationId == 0L) return
        viewModelScope.launch {
            val result = backupRepository.exportConversationToMarkdown(conversationId)
            _uiState.update { it.copy(exportResult = result) }
        }
    }

    fun exportConversationToJson() {
        val conversationId = _uiState.value.currentConversationId
        if (conversationId == 0L) return
        viewModelScope.launch {
            val result = backupRepository.exportConversationToJson(conversationId)
            _uiState.update { it.copy(exportResult = result) }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    private fun pathToBase64(path: String): String? {
        return try {
            val inputStream = if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path))
            } else {
                File(path).inputStream()
            }
            inputStream?.use { input ->
                val bytes = input.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }
}
