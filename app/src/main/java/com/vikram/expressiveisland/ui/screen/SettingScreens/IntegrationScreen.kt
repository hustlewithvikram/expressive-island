package com.vikram.expressiveisland.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.data.AssistantIntegrationPreferences

@Composable
internal fun AssistantIntegrationScreen(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Integrations",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        AssistantIntegrationSection()
    }
}

@Composable
private fun AssistantIntegrationSection() {

    val context = LocalContext.current
    val integration = remember(context) { AssistantIntegrationPreferences(context) }
    var provider by remember { mutableStateOf(integration.provider) }
    var endpoint by remember { mutableStateOf(integration.endpoint) }
    var model by remember { mutableStateOf(integration.model) }
    var apiKey by remember { mutableStateOf(integration.apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var maxTokens by remember { mutableStateOf(integration.maxOutputTokens.toFloat()) }
    var temperature by remember { mutableStateOf(integration.temperature) }
    var timeout by remember { mutableStateOf(integration.requestTimeoutSeconds.toFloat()) }
    var streaming by remember { mutableStateOf(integration.streamResponses) }
    var includeContext by remember { mutableStateOf(integration.includeConversationContext) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Assistant API", style = MaterialTheme.typography.titleMedium)
        Text("Connect an OpenAI-compatible provider to generate answers inside the Assistant panel. Your API key is encrypted with Android Keystore before it is saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.material3.OutlinedTextField(
            value = provider,
            onValueChange = { provider = it; integration.provider = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Provider name") },
            supportingText = { Text("Example: OpenAI, OpenRouter, or a self-hosted provider") },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
        )
        androidx.compose.material3.OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it; integration.endpoint = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API base URL") },
            placeholder = { Text("https://api.openai.com/v1") },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
        )
        androidx.compose.material3.OutlinedTextField(
            value = model,
            onValueChange = { model = it; integration.model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model ID") },
            placeholder = { Text("gpt-4o-mini") },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
        )
        androidx.compose.material3.OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; integration.apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API key") },
            placeholder = { Text("Paste provider API key") },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                androidx.compose.material3.TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Hide" else "Show")
                }
            },
        )
        Text(
            text = "The key stays on this device and is encrypted at rest. Never share it in screenshots or logs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }

    CardSectionHeader(
        text = "Generation",
        padding = PaddingValues(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
    SettingsSliderCard(
        shape = RoundedCornerShape(24.dp),
        title = "Response length limit",
        description = "Maximum output tokens requested from the model.",
        valueText = maxTokens.toInt().toString(),
        value = maxTokens,
        valueRange = 128f..4096f,
        step = 128f,
        onValueChange = { maxTokens = it },
        onCommit = { integration.maxOutputTokens = maxTokens.toInt() },
    )
    SettingsSliderCard(
        shape = RoundedCornerShape(24.dp),
        title = "Response creativity",
        description = "Lower values are more consistent; higher values are more varied.",
        valueText = String.format(java.util.Locale.US, "%.1f", temperature),
        value = temperature,
        valueRange = 0f..2f,
        step = 0.1f,
        onValueChange = { temperature = it },
        onCommit = { integration.temperature = temperature },
    )
    SettingsSliderCard(
        shape = RoundedCornerShape(24.dp),
        title = "Request timeout",
        description = "How long to wait for the provider before reporting a timeout.",
        valueText = "${timeout.toInt()} sec",
        value = timeout,
        valueRange = 10f..120f,
        step = 5f,
        onValueChange = { timeout = it },
        onCommit = { integration.requestTimeoutSeconds = timeout.toInt() },
    )
    SettingsToggleCard(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
        title = "Stream responses",
        description = "Display generated text as it arrives instead of waiting for the full answer.",
        checked = streaming,
        onCheckedChange = { streaming = it; integration.streamResponses = it },
    )
    SettingsToggleCard(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        title = "Include conversation context",
        description = "Send recent conversation turns with each request when the chat implementation supports it.",
        checked = includeContext,
        onCheckedChange = { includeContext = it; integration.includeConversationContext = it },
    )
    Text(
        text = "Provider settings are saved locally. API request execution and streaming will be connected to these preferences in the Assistant generation service.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}
