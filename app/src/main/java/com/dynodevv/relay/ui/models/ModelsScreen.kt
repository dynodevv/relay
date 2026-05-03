package com.dynodevv.relay.ui.models

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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
    var modelToDelete by remember { mutableStateOf<AIModel?>(null) }
    var modelToEdit by remember { mutableStateOf<AIModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
            item { Spacer(Modifier.height(8.dp)) }

            items(models) { model ->
                ModelCard(
                    model = model,
                    onEdit = { modelToEdit = model },
                    onDelete = { modelToDelete = model }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
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
                            contextLength = contextLength.toIntOrNull()
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = model.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
