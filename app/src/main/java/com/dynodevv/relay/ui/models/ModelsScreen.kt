package com.dynodevv.relay.ui.models

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dynodevv.relay.domain.model.AIModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    providerId: Long,
    viewModel: ModelsViewModel,
    onBack: () -> Unit,
    onAddModel: () -> Unit
) {
    val models by viewModel.getModels(providerId).collectAsState(initial = emptyList())
    val isFetching by viewModel.isFetching.collectAsState()
    val fetchError by viewModel.fetchError.collectAsState()
    val fetchedModels by viewModel.fetchedModels.collectAsState()
    val defaultModelId by viewModel.defaultModelId.collectAsState()
    var modelToDelete by remember { mutableStateOf<AIModel?>(null) }
    var modelToEdit by remember { mutableStateOf<AIModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredModels = remember(models, searchQuery) {
        if (searchQuery.isBlank()) models
        else models.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(fetchError) {
        fetchError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFetchError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchModelsFromApi(providerId) }) {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = "Fetch from API"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddModel) {
                Icon(Icons.Default.Add, contentDescription = "Add model")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search models") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                Spacer(Modifier.height(4.dp))
            }

            items(filteredModels, key = { "${it.providerId}_${it.id}" }) { model ->
                ModelCard(
                    model = model,
                    isDefault = model.id == defaultModelId,
                    onEdit = { modelToEdit = model },
                    onDelete = { modelToDelete = model },
                    onToggleFavorite = { viewModel.toggleFavorite(model) },
                    onSetDefault = { viewModel.setAsDefault(model.providerId, model.id) },
                    onClearDefault = { viewModel.clearDefault() }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    fetchedModels?.let { availableModels ->
        FetchModelsDialog(
            models = availableModels,
            onDismiss = { viewModel.dismissFetchedModels() },
            onAddSelected = { selected ->
                viewModel.addFetchedModels(selected)
            }
        )
    }

    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete Model") },
            text = { Text("Delete ${model.displayName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteModel(model.id, model.providerId)
                    modelToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    modelToEdit?.let { model ->
        EditModelDialog(
            model = model,
            onDismiss = { modelToEdit = null },
            onSave = { updated ->
                viewModel.editModel(updated)
                modelToEdit = null
            }
        )
    }
}

@Composable
private fun FetchModelsDialog(
    models: List<AIModel>,
    onDismiss: () -> Unit,
    onAddSelected: (List<AIModel>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val filteredModels = remember(models, searchQuery) {
        if (searchQuery.isBlank()) models
        else models.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Models from API") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search models") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "${selectedIds.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(filteredModels) { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (selectedIds.contains(model.id)) {
                                        selectedIds - model.id
                                    } else {
                                        selectedIds + model.id
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(model.id),
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        selectedIds + model.id
                                    } else {
                                        selectedIds - model.id
                                    }
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = model.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = models.filter { selectedIds.contains(it.id) }
                    onAddSelected(selected)
                },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("Add Selected")
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
private fun EditModelDialog(
    model: AIModel,
    onDismiss: () -> Unit,
    onSave: (AIModel) -> Unit
) {
    var displayName by remember(model.id) { mutableStateOf(model.displayName) }
    var supportsImageInput by remember(model.id) { mutableStateOf(model.supportsImageInput) }
    var supportsTools by remember(model.id) { mutableStateOf(model.supportsTools) }
    var supportsReasoning by remember(model.id) { mutableStateOf(model.supportsReasoning) }
    var contextLength by remember(model.id) { mutableStateOf(model.contextLength?.toString() ?: "") }
    var temperature by remember(model.id) { mutableStateOf(model.temperature?.toString() ?: "") }
    var maxTokens by remember(model.id) { mutableStateOf(model.maxTokens?.toString() ?: "") }
    var topP by remember(model.id) { mutableStateOf(model.topP?.toString() ?: "") }
    var topK by remember(model.id) { mutableStateOf(model.topK?.toString() ?: "") }
    var presencePenalty by remember(model.id) { mutableStateOf(model.presencePenalty?.toString() ?: "") }
    var frequencyPenalty by remember(model.id) { mutableStateOf(model.frequencyPenalty?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = contextLength,
                    onValueChange = { contextLength = it },
                    label = { Text("Context Length") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = "Parameters",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Temp") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it },
                        label = { Text("Max Tok") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topP,
                        onValueChange = { topP = it },
                        label = { Text("Top P") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = topK,
                        onValueChange = { topK = it },
                        label = { Text("Top K") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = presencePenalty,
                        onValueChange = { presencePenalty = it },
                        label = { Text("Pres. Pen.") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = frequencyPenalty,
                        onValueChange = { frequencyPenalty = it },
                        label = { Text("Freq. Pen.") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Text(
                    text = "Capabilities",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = supportsImageInput, onCheckedChange = { supportsImageInput = it })
                    Text("Supports Image Input")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = supportsTools, onCheckedChange = { supportsTools = it })
                    Text("Supports Tools")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = supportsReasoning, onCheckedChange = { supportsReasoning = it })
                    Text("Supports Reasoning")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        model.copy(
                            displayName = displayName.trim(),
                            supportsImageInput = supportsImageInput,
                            supportsTools = supportsTools,
                            supportsReasoning = supportsReasoning,
                            contextLength = contextLength.toIntOrNull(),
                            temperature = temperature.toDoubleOrNull(),
                            maxTokens = maxTokens.toIntOrNull(),
                            topP = topP.toDoubleOrNull(),
                            topK = topK.toIntOrNull(),
                            presencePenalty = presencePenalty.toDoubleOrNull(),
                            frequencyPenalty = frequencyPenalty.toDoubleOrNull()
                        )
                    )
                },
                enabled = displayName.isNotBlank()
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
private fun ModelCard(
    model: AIModel,
    isDefault: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetDefault: () -> Unit,
    onClearDefault: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (isDefault) {
                            Spacer(Modifier.size(6.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Default model",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = model.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (model.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (model.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (model.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row {
                if (model.supportsImageInput) {
                    AssistChip("Vision")
                }
                if (model.supportsTools) {
                    AssistChip("Tools")
                }
                if (model.supportsReasoning) {
                    AssistChip("Reasoning")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isDefault) {
                            DropdownMenuItem(
                                text = { Text("Clear Default") },
                                onClick = {
                                    onClearDefault()
                                    showMenu = false
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Set as Default") },
                                onClick = {
                                    onSetDefault()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun AssistChip(label: String) {
    Card(
        modifier = Modifier.padding(end = 6.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
