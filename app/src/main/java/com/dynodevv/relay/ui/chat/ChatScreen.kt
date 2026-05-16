package com.dynodevv.relay.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dynodevv.relay.R
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.MessageRole
import com.dynodevv.relay.ui.chat.components.ChatNavigationDrawer
import com.dynodevv.relay.ui.chat.components.MessageBubble
import com.dynodevv.relay.ui.chat.components.MessageInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.attachImages(uris.map { it.toString() })
        }
    }

    // Confirmation dialog states
    var messageToDelete by remember { mutableStateOf<Long?>(null) }
    var showEditConfirm by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showMessageSearch by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showExportResult by remember { mutableStateOf<String?>(null) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiState.navigateToConversationId) {
        uiState.navigateToConversationId?.let { id ->
            onNavigateToChat(id)
            viewModel.clearNavigation()
        }
    }

    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { result ->
            if (result.isNotEmpty()) {
                showExportResult = result
            }
            viewModel.clearExportResult()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ChatNavigationDrawer(
                conversations = uiState.conversations,
                folders = uiState.folders,
                tags = uiState.tags,
                currentConversationId = uiState.currentConversationId,
                currentFolderId = uiState.currentFolderId,
                showArchived = uiState.showArchived,
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                isBulkSelectionMode = uiState.isBulkSelectionMode,
                selectedConversationIds = uiState.selectedConversationIds,
                onConversationClick = { id ->
                    scope.launch { drawerState.close() }
                    onNavigateToChat(id)
                },
                onNewChat = {
                    scope.launch { drawerState.close() }
                    viewModel.startNewChat()
                },
                onRenameConversation = { id, title -> viewModel.renameConversation(id, title) },
                onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                onArchiveConversation = { id -> viewModel.archiveConversation(id) },
                onUnarchiveConversation = { id -> viewModel.unarchiveConversation(id) },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onSelectFolder = { viewModel.selectFolder(it) },
                onShowArchived = { viewModel.showArchived(it) },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onClearSearch = { viewModel.clearSearch() },
                onToggleBulkSelectionMode = { viewModel.toggleBulkSelectionMode() },
                onToggleConversationSelection = { viewModel.toggleConversationSelection(it) },
                onSelectAll = { viewModel.selectAllConversations() },
                onClearSelection = { viewModel.clearSelection() },
                onArchiveSelected = { viewModel.archiveSelectedConversations() },
                onDeleteSelected = { viewModel.deleteSelectedConversations() },
                onMoveToFolder = { viewModel.moveSelectedToFolder(it) },
                onCreateFolder = { viewModel.createFolder(it) },
                onRenameFolder = { id, name -> viewModel.renameFolder(id, name) },
                onDeleteFolder = { viewModel.deleteFolder(it) },
                onAddTagToConversation = { convId, tagId -> viewModel.addTagToConversation(convId, tagId) },
                onRemoveTagFromConversation = { convId, tagId -> viewModel.removeTagFromConversation(convId, tagId) },
                onCreateTag = { name, color -> viewModel.createTag(name, color) },
                onDeleteTag = { viewModel.deleteTag(it) },
                onExportMarkdown = { viewModel.exportConversationToMarkdown() },
                onExportJson = { viewModel.exportConversationToJson() }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.conversationTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.availableModels.isNotEmpty()) {
                                Box {
                                    TextButton(
                                        onClick = { showModelMenu = true },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Text(
                                            text = uiState.currentModel?.displayName ?: uiState.currentModelId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showModelMenu,
                                        onDismissRequest = { showModelMenu = false }
                                    ) {
                                        uiState.availableModels.forEach { model ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = model.displayName,
                                                        color = if (model.id == uiState.currentModelId)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.switchModel(model.id)
                                                    showModelMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        if (uiState.currentConversationId != 0L) {
                            IconButton(onClick = { showMessageSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search messages")
                            }
                        }
                        IconButton(onClick = { viewModel.startNewChat() }) {
                            Icon(Icons.Default.Add, contentDescription = "New chat")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Column {
                    // Template quick-access bar
                    if (uiState.templates.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.templates.take(3).forEach { template ->
                                Button(
                                    onClick = { viewModel.applyTemplate(template.content) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(template.name, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (uiState.templates.size > 3) {
                                IconButton(onClick = { showTemplates = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "More templates")
                                }
                            }
                            IconButton(onClick = { showSaveTemplateDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Save as template")
                            }
                        }
                    }

                    MessageInput(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputChange,
                        onSend = {
                            if (uiState.editingMessageId != null) {
                                showEditConfirm = true
                            } else {
                                viewModel.sendMessage()
                            }
                        },
                        onStop = { viewModel.stopGeneration() },
                        onAttach = { imagePicker.launch("image/*") },
                        onCancelEdit = viewModel::cancelEditing,
                        onRemoveImage = viewModel::removeAttachedImage,
                        onClearImages = viewModel::clearAttachedImages,
                        isLoading = uiState.isLoading,
                        supportsAttachments = uiState.currentModel?.supportsImageInput == true,
                        attachedImageUris = uiState.attachedImageUris,
                        isEditing = uiState.editingMessageId != null,
                        modifier = Modifier.imePadding()
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .imePadding()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val startX = down.position.x
                                if (startX > screenWidthPx * 0.25f) return@awaitEachGesture

                                var totalX = 0f
                                var totalY = 0f
                                var prevX = startX
                                var prevY = down.position.y

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    val pos = change.position
                                    val dx = pos.x - prevX
                                    val dy = pos.y - prevY
                                    prevX = pos.x
                                    prevY = pos.y
                                    totalX += dx
                                    totalY += kotlin.math.abs(dy)
                                    change.consume()

                                    if (totalX > 80f && totalX > totalY * 1.5f) {
                                        scope.launch { drawerState.open() }
                                        break
                                    }
                                    if (!change.pressed) break
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!uiState.hasProviders && uiState.messages.isEmpty()) {
                        EmptyState(onNavigateToSettings = onNavigateToSettings)
                    } else if (uiState.hasProviders && uiState.messages.isEmpty()) {
                        Image(
                            painter = painterResource(id = R.drawable.relay_app_icon),
                            contentDescription = "Relay",
                            modifier = Modifier.size(120.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            reverseLayout = true
                        ) {
                            items(
                                items = if (uiState.isMessageSearchActive) uiState.messageSearchResults else uiState.messages,
                                key = { it.id }
                            ) { message ->
                                val isSearchResult = uiState.isMessageSearchActive &&
                                        uiState.messageSearchQuery.isNotBlank() &&
                                        message.content.contains(uiState.messageSearchQuery, ignoreCase = true)
                                MessageBubble(
                                    message = message,
                                    onDelete = { messageToDelete = message.id },
                                    onRegenerate = {
                                        if (message.role is MessageRole.Assistant) {
                                            viewModel.regenerateMessage(message.id)
                                        }
                                    },
                                    onEdit = {
                                        if (message.role is MessageRole.User) {
                                            viewModel.startEditingMessage(message.id)
                                        }
                                    },
                                    highlightQuery = if (isSearchResult) uiState.messageSearchQuery else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    messageToDelete?.let { msgId ->
        val msg = uiState.messages.find { it.id == msgId }
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Delete Message") },
            text = {
                Text("This will also delete all messages sent after this one. Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessageAndAfter(msgId)
                        messageToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit confirmation dialog
    if (showEditConfirm && uiState.editingMessageId != null) {
        AlertDialog(
            onDismissRequest = { showEditConfirm = false },
            title = { Text("Edit Message") },
            text = {
                Text("This will remove all messages after this one and regenerate the response. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.sendMessage()
                        showEditConfirm = false
                    }
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Message search dialog
    if (showMessageSearch) {
        AlertDialog(
            onDismissRequest = {
                showMessageSearch = false
                viewModel.clearMessageSearch()
            },
            title = { Text("Search Messages") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.messageSearchQuery,
                        onValueChange = { viewModel.setMessageSearchQuery(it) },
                        placeholder = { Text("Search in this conversation...") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.isMessageSearchActive) {
                        Text(
                            "${uiState.messageSearchResults.size} results",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showMessageSearch = false
                }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearMessageSearch()
                }) {
                    Text("Clear")
                }
            }
        )
    }

    // Templates picker dialog
    if (showTemplates) {
        AlertDialog(
            onDismissRequest = { showTemplates = false },
            title = { Text("Templates") },
            text = {
                LazyColumn {
                    items(uiState.templates, key = { it.id }) { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                template.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row {
                                TextButton(onClick = {
                                    viewModel.applyTemplate(template.content)
                                    showTemplates = false
                                }) {
                                    Text("Apply")
                                }
                                IconButton(onClick = { viewModel.deleteTemplate(template.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplates = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Save as template dialog
    if (showSaveTemplateDialog) {
        var templateName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Save as Template") },
            text = {
                Column {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Template name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                    if (uiState.inputText.isNotBlank()) {
                        Text(
                            "Content preview:",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            uiState.inputText.take(100),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (templateName.isNotBlank() && uiState.inputText.isNotBlank()) {
                            viewModel.createTemplate(templateName, uiState.inputText)
                            showSaveTemplateDialog = false
                        }
                    },
                    enabled = templateName.isNotBlank() && uiState.inputText.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export result dialog
    showExportResult?.let { result ->
        AlertDialog(
            onDismissRequest = { showExportResult = null },
            title = { Text("Export") },
            text = {
                Column {
                    Text("Conversation exported. Copy to clipboard?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Exported Chat", result))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    showExportResult = null
                }) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportResult = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
private fun EmptyState(onNavigateToSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No providers configured",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add an AI provider in settings to start chatting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateToSettings) {
                Text("Go to Settings")
            }
        }
    }
}
