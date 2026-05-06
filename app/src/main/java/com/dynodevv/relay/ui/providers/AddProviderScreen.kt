package com.dynodevv.relay.ui.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dynodevv.relay.domain.model.Provider
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.FlowRow

val ProviderPresets = listOf(
    Provider(name = "OpenAI", apiBaseUrl = "https://api.openai.com/v1", iconName = "openai"),
    Provider(name = "Google Gemini", apiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai", iconName = "gemini"),
    Provider(name = "Anthropic", apiBaseUrl = "https://api.anthropic.com/v1", iconName = "anthropic"),
    Provider(name = "OpenRouter", apiBaseUrl = "https://openrouter.ai/api/v1", iconName = "openrouter"),
    Provider(name = "Groq", apiBaseUrl = "https://api.groq.com/openai/v1", iconName = "groq"),
    Provider(name = "DeepSeek", apiBaseUrl = "https://api.deepseek.com/v1", iconName = "deepseek"),
    Provider(name = "Together AI", apiBaseUrl = "https://api.together.xyz/v1", iconName = "together"),
    Provider(name = "Perplexity", apiBaseUrl = "https://api.perplexity.ai", iconName = "perplexity")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderScreen(
    viewModel: ProvidersViewModel,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var apiBaseUrl by remember { mutableStateOf("") }
    var apiPath by remember { mutableStateOf("/chat/completions") }
    var apiKey by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<TestResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Provider") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Quick Setup",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProviderPresets.forEach { preset ->
                    SuggestionChip(
                        onClick = {
                            name = preset.name
                            apiBaseUrl = preset.apiBaseUrl
                            apiPath = preset.apiPath
                        },
                        label = { Text(preset.name) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Provider Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = apiBaseUrl,
                onValueChange = { apiBaseUrl = it },
                label = { Text("API Base URL") },
                placeholder = { Text("https://api.example.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = apiPath,
                onValueChange = { apiPath = it },
                label = { Text("API Path") },
                placeholder = { Text("/chat/completions") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (apiBaseUrl.isNotBlank()) {
                            scope.launch {
                                isTesting = true
                                testResult = null
                                testResult = viewModel.testConnection(
                                    Provider(
                                        name = name,
                                        apiBaseUrl = apiBaseUrl.trim(),
                                        apiPath = apiPath.trim(),
                                        apiKey = apiKey.trim().ifEmpty { null }
                                    )
                                )
                                isTesting = false
                            }
                        }
                    },
                    enabled = apiBaseUrl.isNotBlank() && !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test Connection")
                    }
                }

                when (val result = testResult) {
                    is TestResult.Success -> Text(
                        "Connected!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    is TestResult.Error -> Text(
                        result.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                    else -> {}
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && apiBaseUrl.isNotBlank()) {
                        viewModel.addProvider(
                            Provider(
                                name = name.trim(),
                                apiBaseUrl = apiBaseUrl.trim(),
                                apiPath = apiPath.trim(),
                                apiKey = apiKey.trim().ifEmpty { null }
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && apiBaseUrl.isNotBlank(),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Save Provider")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
