package com.dynodevv.relay.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.dynodevv.relay.domain.model.Tag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatNavigationDrawer(
    conversations: List<Conversation>,
    tags: List<Tag>,
    currentConversationId: Long,
    searchQuery: String,
    isSearchActive: Boolean,
    onConversationClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    onRenameConversation: (Long, String) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onArchiveConversation: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
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

        Text(
            text = if (isSearchActive) "Search Results" else "Conversations",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn {
            items(conversations, key = { it.id }) { conversation ->
                ConversationDrawerItem(
                    conversation = conversation,
                    isSelected = conversation.id == currentConversationId,
                    dateFormat = dateFormat,
                    onClick = { onConversationClick(conversation.id) },
                    onLongPress = { conversationMenuId = conversation.id },
                    onRename = {
                        conversationToRename = conversation
                        conversationMenuId = null
                    },
                    onDelete = {
                        conversationToDelete = conversation
                        conversationMenuId = null
                    },
                    onArchive = {
                        conversationToArchive = conversation.id
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

        // Bottom action buttons: Folders | Archive
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Folders button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onNavigateToFolders() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folders",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Archive button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onNavigateToArchive() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archived",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
            selected = isSelected,
            onClick = { }
        )

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
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = onManageTags
            )
            DropdownMenuItem(
                text = { Text("Archive") },
                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                onClick = onArchive
            )
            DropdownMenuItem(
                text = { Text("Export Markdown") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = onExportMarkdown
            )
            DropdownMenuItem(
                text = { Text("Export JSON") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
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
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
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
