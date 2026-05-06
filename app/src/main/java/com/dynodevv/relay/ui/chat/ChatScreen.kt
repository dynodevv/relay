package com.dynodevv.relay.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.dynodevv.relay.domain.model.Conversation
import com.dynodevv.relay.domain.model.MessageRole
import com.dynodevv.relay.ui.chat.components.MessageBubble
import com.dynodevv.relay.ui.chat.components.MessageInput
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showChatMenu by remember { mutableStateOf(false) }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ChatNavigationDrawer(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                onConversationClick = { id ->
                    scope.launch { drawerState.close() }
                    onNavigateToChat(id)
                },
                onNewChat = {
                    scope.launch { drawerState.close() }
                    viewModel.startNewChat()
                },
                onRenameConversation = { id, title ->
                    viewModel.renameConversation(id, title)
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
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
                        IconButton(onClick = { viewModel.startNewChat() }) {
                            Icon(Icons.Default.Add, contentDescription = "New chat")
                        }
                        Box {
                            IconButton(onClick = { showChatMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Chat options")
                            }
                            DropdownMenu(
                                expanded = showChatMenu,
                                onDismissRequest = { showChatMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Prompt") },
                                    onClick = {
                                        viewModel.showSystemPromptDialog()
                                        showChatMenu = false
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
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
                        }
                ) {
                    if (!uiState.hasProviders && uiState.messages.isEmpty()) {
                        EmptyState(onNavigateToSettings = onNavigateToSettings)
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            reverseLayout = true
                        ) {
                            items(
                                items = uiState.messages.reversed(),
                                key = { it.id }
                            ) { message ->
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
                                    }
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

    // Edit confirmation dialog (shown when sending an edit)
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

    // System Prompt dialog
    if (uiState.showSystemPromptDialog) {
        SystemPromptDialog(
            currentPrompt = uiState.systemPrompt,
            onDismiss = { viewModel.dismissSystemPromptDialog() },
            onSave = { viewModel.updateSystemPrompt(it) }
        )
    }
}

@Composable
private fun SystemPromptDialog(
    currentPrompt: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var text by remember { mutableStateOf(currentPrompt ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("System Prompt") },
        text = {
            Column {
                Text(
                    text = "Custom instructions for this conversation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.trim().takeIf { it.isNotEmpty() }) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@Composable
private fun ChatNavigationDrawer(
    conversations: List<Conversation>,
    currentConversationId: Long,
    onConversationClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    onRenameConversation: (Long, String) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    var conversationMenuId by remember { mutableStateOf<Long?>(null) }
    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    ModalDrawerSheet {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Relay",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        NavigationDrawerItem(
            label = { Text("New Chat") },
            selected = false,
            onClick = onNewChat,
            icon = { Icon(Icons.Default.Add, contentDescription = null) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Conversations",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        val density = LocalDensity.current
        LazyColumn {
            items(conversations, key = { it.id }) { conversation ->
                var conversationMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
                Box {
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(
                                    text = conversation.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormat.format(Date(conversation.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = conversation.id == currentConversationId,
                        onClick = { }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onConversationClick(conversation.id) },
                                    onLongPress = { offset ->
                                        conversationMenuOffset = with(density) {
                                            DpOffset(offset.x.toDp(), offset.y.toDp())
                                        }
                                        conversationMenuId = conversation.id
                                    }
                                )
                            }
                    )

                    DropdownMenu(
                        expanded = conversationMenuId == conversation.id,
                        onDismissRequest = { conversationMenuId = null },
                        offset = conversationMenuOffset
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                conversationMenuId = null
                                conversationToRename = conversation
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                conversationMenuId = null
                                conversationToDelete = conversation
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider()

        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = onNavigateToSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))
    }

    conversationToRename?.let { conv ->
        RenameDialog(
            currentName = conv.title,
            onDismiss = { conversationToRename = null },
            onConfirm = { newName ->
                onRenameConversation(conv.id, newName)
                conversationToRename = null
            }
        )
    }

    conversationToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete \"${conv.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConversation(conv.id)
                    conversationToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Conversation") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
