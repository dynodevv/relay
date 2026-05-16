package com.dynodevv.relay.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.dynodevv.relay.domain.model.Conversation
import com.dynodevv.relay.domain.model.Folder
import com.dynodevv.relay.domain.model.Tag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatNavigationDrawer(
    conversations: List<Conversation>,
    folders: List<Folder>,
    tags: List<Tag>,
    currentConversationId: Long,
    currentFolderId: Long?,
    showArchived: Boolean,
    searchQuery: String,
    isSearchActive: Boolean,
    isBulkSelectionMode: Boolean,
    selectedConversationIds: Set<Long>,
    onConversationClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    onRenameConversation: (Long, String) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onArchiveConversation: (Long) -> Unit,
    onUnarchiveConversation: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectFolder: (Long?) -> Unit,
    onShowArchived: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleBulkSelectionMode: () -> Unit,
    onToggleConversationSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onArchiveSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveToFolder: (Long?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onAddTagToConversation: (Long, Long) -> Unit,
    onRemoveTagFromConversation: (Long, Long) -> Unit,
    onCreateTag: (String, String) -> Unit,
    onDeleteTag: (Long) -> Unit,
    onExportMarkdown: () -> Unit,
    onExportJson: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    var conversationMenuId by remember { mutableStateOf<Long?>(null) }
    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }
    var conversationToArchive by remember { mutableStateOf<Long?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var conversationForTags by remember { mutableStateOf<Conversation?>(null) }

    ModalDrawerSheet {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Relay",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search conversations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Text("\u2715")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        // New Chat
        NavigationDrawerItem(
            label = { Text("New Chat") },
            selected = false,
            onClick = onNewChat,
            icon = { Icon(Icons.Default.Add, contentDescription = null) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Folders
        Text(
            text = "Folders",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            label = { Text("All Conversations") },
            selected = currentFolderId == null && !showArchived,
            onClick = { onSelectFolder(null) },
            icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
        )

        folders.forEach { folder ->
            Box {
                NavigationDrawerItem(
                    label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selected = currentFolderId == folder.id && !showArchived,
                    onClick = { onSelectFolder(folder.id) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) }
                )
            }
        }

        TextButton(
            onClick = { showCreateFolderDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Create Folder")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Conversations header with bulk actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showArchived) "Archived" else if (isSearchActive) "Search Results" else "Conversations",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isBulkSelectionMode) {
                IconButton(onClick = onToggleBulkSelectionMode, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Bulk select", modifier = Modifier.size(18.dp))
                }
            } else {
                TextButton(onClick = onToggleBulkSelectionMode) {
                    Text("Done", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Bulk action bar
        if (isBulkSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onSelectAll) { Text("All") }
                TextButton(onClick = onClearSelection) { Text("None") }
                TextButton(onClick = onArchiveSelected) { Text("Archive") }
                TextButton(onClick = onDeleteSelected) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
            // Move to folder dropdown
            if (selectedConversationIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { onMoveToFolder(null) }) { Text("Remove from folder") }
                    folders.forEach { folder ->
                        TextButton(onClick = { onMoveToFolder(folder.id) }) { Text(folder.name) }
                    }
                }
            }
        }

        LazyColumn {
            items(conversations, key = { it.id }) { conversation ->
                ConversationDrawerItem(
                    conversation = conversation,
                    isSelected = conversation.id == currentConversationId,
                    isInBulkMode = isBulkSelectionMode,
                    isChecked = selectedConversationIds.contains(conversation.id),
                    dateFormat = dateFormat,
                    onClick = {
                        if (isBulkSelectionMode) {
                            onToggleConversationSelection(conversation.id)
                        } else {
                            onConversationClick(conversation.id)
                        }
                    },
                    onLongPress = {
                        if (!isBulkSelectionMode) {
                            conversationMenuId = conversation.id
                        }
                    },
                    onRename = {
                        conversationToRename = conversation
                        conversationMenuId = null
                    },
                    onDelete = {
                        conversationToDelete = conversation
                        conversationMenuId = null
                    },
                    onArchive = {
                        if (conversation.isArchived) {
                            onUnarchiveConversation(conversation.id)
                        } else {
                            conversationToArchive = conversation.id
                        }
                        conversationMenuId = null
                    },
                    onManageTags = {
                        conversationForTags = conversation
                        conversationMenuId = null
                    },
                    onExportMarkdown = {
                        onConversationClick(conversation.id)
                        onExportMarkdown()
                        conversationMenuId = null
                    },
                    onExportJson = {
                        onConversationClick(conversation.id)
                        onExportJson()
                        conversationMenuId = null
                    },
                    menuOpen = conversationMenuId == conversation.id,
                    onDismissMenu = { conversationMenuId = null }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider()

        // Archive toggle
        NavigationDrawerItem(
            label = { Text(if (showArchived) "Back to Active" else "Archived") },
            selected = false,
            onClick = { onShowArchived(!showArchived) },
            icon = {
                Icon(
                    if (showArchived) Icons.Default.FolderOpen else Icons.Default.Archive,
                    contentDescription = null
                )
            }
        )

        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = onNavigateToSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))
    }

    // Rename dialog
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

    // Delete dialog
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

    // Archive confirm dialog
    conversationToArchive?.let { id ->
        AlertDialog(
            onDismissRequest = { conversationToArchive = null },
            title = { Text("Archive Conversation") },
            text = { Text("Archive this conversation? You can find it later in the Archived section.") },
            confirmButton = {
                TextButton(onClick = {
                    onArchiveConversation(id)
                    conversationToArchive = null
                }) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToArchive = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create folder dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    // Rename folder dialog
    folderToRename?.let { folder ->
        RenameDialog(
            currentName = folder.name,
            onDismiss = { folderToRename = null },
            onConfirm = { newName ->
                onRenameFolder(folder.id, newName)
                folderToRename = null
            }
        )
    }

    // Delete folder dialog
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = { Text("Delete \"${folder.name}\"? Conversations will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder(folder.id)
                    folderToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manage tags dialog
    conversationForTags?.let { conv ->
        ManageTagsDialog(
            conversation = conv,
            allTags = tags,
            onDismiss = { conversationForTags = null },
            onAddTag = { tagId -> onAddTagToConversation(conv.id, tagId) },
            onRemoveTag = { tagId -> onRemoveTagFromConversation(conv.id, tagId) },
            onCreateTag = { name, color -> onCreateTag(name, color) },
            onDeleteTag = { id -> onDeleteTag(id) }
        )
    }
}

@Composable
private fun ConversationDrawerItem(
    conversation: Conversation,
    isSelected: Boolean,
    isInBulkMode: Boolean,
    isChecked: Boolean,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onManageTags: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportJson: () -> Unit,
    menuOpen: Boolean,
    onDismissMenu: () -> Unit
) {
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box {
        NavigationDrawerItem(
            label = {
                Column {
                    Text(
                        text = conversation.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateFormat.format(Date(conversation.updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Tags
                        if (conversation.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            conversation.tags.take(3).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(parseColor(tag.colorHex))
                                )
                            }
                        }
                    }
                }
            },
            selected = isSelected && !isInBulkMode,
            onClick = onClick,
            badge = {
                if (isInBulkMode) {
                    if (isChecked) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Outlined.Circle, contentDescription = "Not selected", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )

        if (!isInBulkMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { offset ->
                                menuOffset = with(density) {
                                    DpOffset(offset.x.toDp(), offset.y.toDp())
                                }
                                onLongPress()
                            }
                        )
                    }
            )
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = onDismissMenu,
            offset = menuOffset
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = onRename
            )
            DropdownMenuItem(
                text = { Text("Manage Tags") },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                onClick = onManageTags
            )
            DropdownMenuItem(
                text = { Text(if (conversation.isArchived) "Unarchive" else "Archive") },
                leadingIcon = { Icon(if (conversation.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) },
                onClick = onArchive
            )
            DropdownMenuItem(
                text = { Text("Export Markdown") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = onExportMarkdown
            )
            DropdownMenuItem(
                text = { Text("Export JSON") },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                onClick = onExportJson
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = onDelete
            )
        }
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
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
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

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Folder name") },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotBlank()
            ) {
                Text("Create")
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
private fun ManageTagsDialog(
    conversation: Conversation,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onAddTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onCreateTag: (String, String) -> Unit,
    onDeleteTag: (Long) -> Unit
) {
    var showCreateTag by remember { mutableStateOf(false) }
    val conversationTagIds = conversation.tags.map { it.id }.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(allTags, key = { it.id }) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(parseColor(tag.colorHex))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tag.name)
                            }
                            Row {
                                if (conversationTagIds.contains(tag.id)) {
                                    TextButton(onClick = { onRemoveTag(tag.id) }) {
                                        Text("Remove")
                                    }
                                } else {
                                    TextButton(onClick = { onAddTag(tag.id) }) {
                                        Text("Add")
                                    }
                                }
                                IconButton(onClick = { onDeleteTag(tag.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete tag", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { showCreateTag = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Create New Tag")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    if (showCreateTag) {
        var tagName by remember { mutableStateOf("") }
        val colors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F")
        var selectedColor by remember { mutableStateOf(colors[0]) }

        AlertDialog(
            onDismissRequest = { showCreateTag = false },
            title = { Text("Create Tag") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = { Text("Tag name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Color:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(color))
                                    .then(
                                        if (selectedColor == color) {
                                            Modifier.padding(2.dp)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tagName.isNotBlank()) {
                            onCreateTag(tagName.trim(), selectedColor)
                            showCreateTag = false
                        }
                    },
                    enabled = tagName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTag = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
}
