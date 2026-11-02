package com.vikram.expressiveisland.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.data.AssistantIntegrationPreferences

private fun groupedShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) 24.dp else 6.dp,
    topEnd = if (isFirst) 24.dp else 6.dp,
    bottomStart = if (isLast) 24.dp else 6.dp,
    bottomEnd = if (isLast) 24.dp else 6.dp,
)

private data class AiProvider(
    val id: String,
    val name: String,
    val description: String,
    val endpoint: String,
    val defaultModel: String,
    val format: String = "OpenAI-compatible API",
    val available: Boolean = true,
    val unavailableReason: String = "",
)

private val aiProviders = listOf(
    AiProvider("openai", "ChatGPT / OpenAI", "GPT models · Official API key", "https://api.openai.com/v1", "gpt-4o-mini"),
    AiProvider("gemini", "Google Gemini", "Gemini models · Google AI Studio key", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", "Gemini API"),
    AiProvider("anthropic", "Anthropic Claude", "Claude models · Anthropic API key", "https://api.anthropic.com/v1", "claude-3-5-haiku-latest", "Anthropic Messages API"),
    AiProvider("deepseek", "DeepSeek", "DeepSeek Chat and Reasoner", "https://api.deepseek.com/v1", "deepseek-chat"),
    AiProvider("groq", "Groq", "Fast hosted open models", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    AiProvider("openrouter", "OpenRouter", "One API for models from many providers", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
    AiProvider("mistral", "Mistral AI", "Mistral and Ministral models", "https://api.mistral.ai/v1", "mistral-small-latest"),
    AiProvider("xai", "xAI", "Grok models", "https://api.x.ai/v1", "grok-3-mini"),
    AiProvider("custom", "Custom API", "OpenAI-compatible or self-hosted endpoint", "", "", "Custom API"),
)

@Composable
internal fun IntegrationsScreen(
    contentPadding: PaddingValues,
    onOpenAI: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenDevice: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Integrations", style = MaterialTheme.typography.headlineSmall)
            Text("Connect the services that make your island more capable.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IntegrationCategoryCard(Icons.Rounded.AutoAwesome, "AI Assistant", "Choose the AI provider that powers Assistant responses.", groupedShape(true, false), onOpenAI)
        IntegrationCategoryCard(Icons.Rounded.NotificationsActive, "Notification intelligence", "Smart summaries, suggested replies and contextual actions.", groupedShape(false, false), onOpenNotifications)
        IntegrationCategoryCard(Icons.Rounded.Webhook, "Automation & webhooks", "Connect workflows to supported island events.", groupedShape(false, false), onOpenAutomation)
        IntegrationCategoryCard(Icons.Rounded.Devices, "Device services", "Connect Android system capabilities and companion services.", groupedShape(false, true), onOpenDevice)
    }
}

@Composable
internal fun IntegrationDetailScreen(contentPadding: PaddingValues, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("This integration is planned for a future update.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun AssistantIntegrationScreen(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("AI Assistant", style = MaterialTheme.typography.headlineSmall)
            Text("Configure the provider and response behaviour.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AssistantIntegrationSection()
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun IntegrationCategoryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    val content = MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = content, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssistantIntegrationSection() {
    val context = LocalContext.current
    val integration = remember(context) { AssistantIntegrationPreferences(context) }
    var selectedId by remember { mutableStateOf(integration.selectedProviderId) }
    var endpoint by remember { mutableStateOf(integration.endpoint) }
    var model by remember { mutableStateOf(integration.model) }
    var apiKey by remember { mutableStateOf(integration.getApiKey(integration.selectedProviderId)) }
    var showKey by remember { mutableStateOf(false) }
    var providerDialogOpen by remember { mutableStateOf(false) }
    var pendingProviderId by remember { mutableStateOf(selectedId) }
    var maxTokens by remember { mutableStateOf(integration.maxOutputTokens.toFloat()) }
    var temperature by remember { mutableStateOf(integration.temperature) }
    var timeout by remember { mutableStateOf(integration.requestTimeoutSeconds.toFloat()) }
    var streaming by remember { mutableStateOf(integration.streamResponses) }
    var includeContext by remember { mutableStateOf(integration.includeConversationContext) }
    val selectedProvider = aiProviders.firstOrNull { it.id == selectedId } ?: aiProviders.first()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("AI Assistant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Provider and model configuration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("AI PROVIDER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        pendingProviderId = selectedId
                        providerDialogOpen = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(selectedProvider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            Text(selectedProvider.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(androidx.compose.material.icons.rounded.ExpandMore, contentDescription = "Choose provider")
                    }
                }
                if (providerDialogOpen) {
                    AlertDialog(
                        onDismissRequest = { providerDialogOpen = false },
                        title = { Text("Choose AI provider") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                aiProviders.filter { it.available }.forEach { provider ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                            .clickable { pendingProviderId = provider.id }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        RadioButton(selected = pendingProviderId == provider.id, onClick = { pendingProviderId = provider.id })
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(provider.name, style = MaterialTheme.typography.titleSmall)
                                            Text(provider.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val provider = aiProviders.first { it.id == pendingProviderId && it.available }
                                selectedId = provider.id
                                integration.selectedProviderId = provider.id
                                apiKey = integration.getApiKey(provider.id)
                                showKey = false
                                integration.provider = provider.name
                                endpoint = if (provider.id == "custom") integration.endpoint else provider.endpoint
                                model = if (provider.id == "custom") integration.model else provider.defaultModel
                                integration.endpoint = endpoint
                                integration.model = model
                                providerDialogOpen = false
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            TextButton(onClick = { providerDialogOpen = false }) { Text("Cancel") }
                        },
                    )
                }
                Text(
                    "Choose a provider with an API key. Custom API supports compatible self-hosted endpoints.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("CONNECTION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "API format · ${selectedProvider.format}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (selectedId == "custom") {
                TextField(
                    value = endpoint,
                    onValueChange = { endpoint = it; integration.endpoint = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API base URL") },
                    placeholder = { Text("https://your-provider.example/v1") },
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    singleLine = true,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("API endpoint", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        endpoint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp),
                    )
                }
            }
            TextField(
                value = model,
                onValueChange = { model = it; integration.model = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model ID") },
                placeholder = { Text(selectedProvider.defaultModel.ifBlank { "Enter model ID" }) },
                supportingText = { Text("Use the exact model identifier supported by your provider.") },
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                singleLine = true,
            )
            TextField(
                value = apiKey,
                onValueChange = { apiKey = it; integration.setApiKey(selectedId, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                placeholder = { Text("Paste your provider API key") },
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (showKey) "Hide API key" else "Show API key")
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)).padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Your key is encrypted with Android Keystore and stored on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }

    CardSectionHeader(text = "Response generation", padding = PaddingValues(start = 4.dp, top = 4.dp, bottom = 4.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SettingsSliderCard(
        shape = groupedShape(true, false),
        title = "Response length",
        description = "Maximum output tokens requested from the selected model.",
        valueText = maxTokens.toInt().toString(),
        value = maxTokens,
        valueRange = 128f..4096f,
        step = 128f,
        onValueChange = { maxTokens = it },
        onCommit = { integration.maxOutputTokens = maxTokens.toInt() },
    )
    SettingsSliderCard(
        shape = groupedShape(false, false),
        title = "Creativity",
        description = "Lower values are more focused; higher values allow more varied responses.",
        valueText = String.format(java.util.Locale.US, "%.1f", temperature),
        value = temperature,
        valueRange = 0f..2f,
        step = 0.1f,
        onValueChange = { temperature = it },
        onCommit = { integration.temperature = temperature },
    )
    SettingsSliderCard(
        shape = groupedShape(false, true),
        title = "Request timeout",
        description = "Maximum time to wait for a provider response.",
        valueText = "${timeout.toInt()} sec",
        value = timeout,
        valueRange = 10f..120f,
        step = 5f,
        onValueChange = { timeout = it },
        onCommit = { integration.requestTimeoutSeconds = timeout.toInt() },
    )
    }

    CardSectionHeader(text = "Assistant behaviour", padding = PaddingValues(start = 4.dp, top = 8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SettingsToggleCard(
        shape = groupedShape(true, false),
        title = "Stream responses",
        description = "Show generated text progressively as it arrives.",
        checked = streaming,
        onCheckedChange = { streaming = it; integration.streamResponses = it },
    )
    SettingsToggleCard(
        shape = groupedShape(false, true),
        title = "Include conversation context",
        description = "Include recent conversation turns when supported by the generation service.",
        checked = includeContext,
        onCheckedChange = { includeContext = it; integration.includeConversationContext = it },
    )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            "Provider selection and credentials are saved locally. Provider-specific request execution is not yet connected to the Assistant generation service.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
