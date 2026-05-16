package com.dynodevv.relay.ui.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynodevv.relay.data.repository.ChatRepository
import com.dynodevv.relay.data.repository.FolderRepository
import com.dynodevv.relay.domain.model.Conversation
import com.dynodevv.relay.domain.model.Folder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrganizationUiState(
    val folders: List<Folder> = emptyList(),
    val archivedConversations: List<Conversation> = emptyList(),
    val folderConversations: List<Conversation> = emptyList(),
    val isBulkSelectionMode: Boolean = false,
    val selectedConversationIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false
)

@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizationUiState())
    val uiState: StateFlow<OrganizationUiState> = _uiState

    init {
        viewModelScope.launch {
            folderRepository.getFolders().collect { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }
        viewModelScope.launch {
            chatRepository.getArchivedConversations().collect { conversations ->
                _uiState.update { it.copy(archivedConversations = conversations) }
            }
        }
    }

    fun loadFolderConversations(folderId: Long) {
        viewModelScope.launch {
            chatRepository.getConversationsByFolder(folderId).collect { conversations ->
                _uiState.update { it.copy(folderConversations = conversations) }
            }
        }
    }

    // Folder CRUD
    fun createFolder(name: String) {
        viewModelScope.launch { folderRepository.createFolder(name) }
    }

    fun renameFolder(id: Long, name: String) {
        viewModelScope.launch { folderRepository.updateFolder(id, name) }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch { folderRepository.deleteFolder(id) }
    }

    // Archive operations
    fun unarchiveConversation(id: Long) {
        viewModelScope.launch { chatRepository.unarchiveConversation(id) }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch { chatRepository.deleteConversation(id) }
    }

    // Bulk selection
    fun toggleBulkSelectionMode() {
        _uiState.update {
            it.copy(
                isBulkSelectionMode = !it.isBulkSelectionMode,
                selectedConversationIds = emptySet()
            )
        }
    }

    fun toggleConversationSelection(id: Long) {
        _uiState.update { state ->
            val newSet = if (state.selectedConversationIds.contains(id)) {
                state.selectedConversationIds - id
            } else {
                state.selectedConversationIds + id
            }
            state.copy(selectedConversationIds = newSet)
        }
    }

    fun selectAllConversations(conversationIds: List<Long>) {
        _uiState.update { it.copy(selectedConversationIds = conversationIds.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedConversationIds = emptySet()) }
    }

    fun unarchiveSelectedConversations() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedConversationIds.toList()
            if (ids.isNotEmpty()) {
                chatRepository.unarchiveConversations(ids)
                _uiState.update {
                    it.copy(
                        selectedConversationIds = emptySet(),
                        isBulkSelectionMode = false
                    )
                }
            }
        }
    }

    fun deleteSelectedConversations() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedConversationIds.toList()
            if (ids.isNotEmpty()) {
                chatRepository.deleteConversations(ids)
                _uiState.update {
                    it.copy(
                        selectedConversationIds = emptySet(),
                        isBulkSelectionMode = false
                    )
                }
            }
        }
    }
}
