package com.vikram.expressiveisland.overlay.contents

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.launch
import com.vikram.expressiveisland.core.AssistantInteractionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.data.ActionButtonAlignment
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.resolve
import com.vikram.expressiveisland.overlay.toHorizontal

/**
 * Assistant expanded layout. On-demand activation renders an interactive prompt composer; assistant
 * response events keep the legacy response view. The whole content is measured independently of the
 * animated viewport and capped to the user's configured share of screen height.
 */
@Composable
fun AssistantExpandedContent(
    event: IslandEvent,
    showActions: Boolean,
    appearance: AppearanceSettings,
    collapsedHeightDp: Int,
    onDismiss: () -> Unit,
    onHeightMeasured: ((Int) -> Unit)? = null,
    onInputFocusChange: (Boolean) -> Unit = {},
    onAssistantPrompt: (String, (String) -> Unit) -> Unit = { _, complete -> complete("Assistant generation is unavailable.") },
    onNewAssistantChat: () -> Unit = {},
    onSelectAssistantChat: (String) -> Unit = {},
) {
    val assistant = event.assistant ?: return
    val contentColor = LocalContentColor.current
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val maxCutoutHeightDp =
        (configuration.screenHeightDp * assistant.maxCutoutHeightPercent / 100).dp
    val maxHeaderWidthDp = (configuration.screenWidthDp * 0.47f).dp
    var query by remember(event.id, assistant.activeSessionId) { mutableStateOf("") }
    var showChatHistory by remember { mutableStateOf(false) }
    val conversationScrollState = rememberScrollState()
    LaunchedEffect(assistant.conversation) {
        conversationScrollState.scrollTo(conversationScrollState.maxValue)
    }
    val submitPrompt: () -> Unit = {
        val prompt = query.trim()
        if (prompt.isNotEmpty()) {
            query = ""
            focusManager.clearFocus(force = true)
            onInputFocusChange(false)
            onAssistantPrompt(prompt) { /* The controller owns the persistent conversation state. */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minOf((collapsedHeightDp + 210).dp, maxCutoutHeightDp), max = maxCutoutHeightDp)
            .then(assistant.expandedBackgroundColor?.let { Modifier.background(it.resolve(), RoundedCornerShape(30.dp)) } ?: Modifier)
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp + collapsedHeightDp.dp,
                bottom = 14.dp,
            )
            .onGloballyPositioned { coordinates ->
                val hDp = (coordinates.size.height / density).toInt()
                if (hDp > 0) onHeightMeasured?.invoke(hDp + 28 + collapsedHeightDp)
            },
    ) {
        // Fixed header: it must never scroll away with the conversation.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).widthIn(max = maxHeaderWidthDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(event = event, badgeSize = 36.dp, iconSize = 22.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    TileTitle(title = event.label)
                    if (assistant.interactivePanel) {
                        Text(
                            text = "Ready when you are",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.68f),
                        )
                    }
                }
            }
            if (assistant.interactivePanel) {
                IconButton(onClick = { showChatHistory = true }) { Icon(Icons.Rounded.History, contentDescription = "Chat history") }
                IconButton(onClick = onNewAssistantChat) { Icon(Icons.Rounded.Add, contentDescription = "New chat") }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Close Assistant")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!assistant.interactivePanel && assistant.interactionMode != AssistantInteractionMode.UNKNOWN) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AssistantSiriVisual(
                    modifier = Modifier.width(if (assistant.interactionMode == AssistantInteractionMode.VOICE) 112.dp else 64.dp)
                        .height(if (assistant.interactionMode == AssistantInteractionMode.VOICE) 112.dp else 64.dp),
                )
                Text(
                    text = if (assistant.interactionMode == AssistantInteractionMode.VOICE) "Listening" else "Prompt mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.76f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        if (showChatHistory && assistant.interactivePanel) {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your conversations", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("${assistant.sessions.size} saved chats", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                        }
                        IconButton(onClick = { showChatHistory = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close chat history", tint = Color.White)
                        }
                    }
                    if (assistant.sessions.isEmpty()) {
                        Text("Your previous chats will appear here.", color = Color.White.copy(alpha = 0.7f))
                    } else {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            assistant.sessions.sortedByDescending { it.updatedAt }.forEach { session ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        .clickable {
                                            onSelectAssistantChat(session.id)
                                            showChatHistory = false
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                ) {
                                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Text(session.title.ifBlank { "New chat" }, color = Color.White, maxLines = 1)
                                        Text(session.turns.lastOrNull()?.content?.takeIf { it.isNotBlank() } ?: "No messages yet", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(5.dp))
                                        Text("${session.turns.size} messages", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (assistant.interactivePanel) {
            // Scrollable conversation only. The header above and composer below stay fixed.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .fillMaxHeight()
                    .verticalScroll(conversationScrollState),
            ) {
                if (assistant.conversation.isEmpty()) {
                    Text(
                        text = "Type something...",
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                assistant.conversation.chunked(2).forEach { messages ->
                    val prompt = messages.firstOrNull { it.role == "user" }?.content
                    val response = messages.firstOrNull { it.role == "assistant" }?.content
                    prompt?.let {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Surface(
                                modifier = Modifier.widthIn(max = maxHeaderWidthDp * 1.65f),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
                                color = Color(0xFF193B78),
                            ) {
                                Text(it, Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    response?.let {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            if (it == "Thinking…") {
                                ThreeDotThinkingLoader()
                            } else {
                                Text(it, modifier = Modifier.widthIn(max = maxHeaderWidthDp * 1.8f).padding(horizontal = 4.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (assistant.conversation.isEmpty()) {
                    assistant.promptText?.let { prompt ->
                        Spacer(Modifier.height(12.dp))
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.padding(14.dp)) {
                                Text(prompt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = contentColor)
                                Spacer(Modifier.height(10.dp))
                                Text(assistant.answerText ?: "Thinking…", style = MaterialTheme.typography.bodyMedium, color = contentColor.copy(alpha = 0.82f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Fixed composer: always stays at the bottom, outside the conversation scroll.
            if (assistant.showPromptInput || assistant.showMicButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (assistant.showPromptInput) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f).height(56.dp).onFocusChanged { onInputFocusChange(it.isFocused) },
                            placeholder = { Text("Type your message...", color = Color.White.copy(alpha = 0.58f)) },
                            shape = RoundedCornerShape(28.dp),
                            singleLine = !assistant.showSendButton,
                            maxLines = if (assistant.showSendButton) 3 else 1,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF17233D),
                                unfocusedContainerColor = Color(0xFF121D33),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear text", tint = Color.White.copy(alpha = 0.75f))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = if (assistant.showSendButton) ImeAction.Default else ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { if (!assistant.showSendButton) submitPrompt() }),
                        )
                    }
                    if (assistant.showMicButton) {
                        ExpressiveAssistantButton(
                            contentDescription = "Open voice assistant",
                            circular = true,
                            enabled = true,
                            onClick = {
                                val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(voiceIntent) }.onFailure {
                                    val fallback = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(fallback) }
                                }
                            },
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White)
                        }
                    }
                    if (assistant.showPromptInput && assistant.showSendButton) {
                        ExpressiveAssistantButton(
                            contentDescription = "Send prompt",
                            circular = false,
                            enabled = query.isNotBlank(),
                            onClick = submitPrompt,
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        } else if (assistant.displayAnswerInCutout) {
            val textToDisplay =
                assistant.answerText.takeIf { !it.isNullOrBlank() } ?: "Assistant response received."
            Text(
                text = textToDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.88f),
            )
        }

        if (showActions && !assistant.interactivePanel) {
            Spacer(Modifier.height(14.dp))
            val chipFill = appearance.actionButtonColor?.resolve() ?: event.accent
            val full = appearance.actionButtonAlignment == ActionButtonAlignment.FULL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp,
                    appearance.actionButtonAlignment.toHorizontal(),
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionChip(
                    action = IslandAction(label = "Close"),
                    style = appearance.actionButtonStyle,
                    fill = chipFill,
                    heightDp = appearance.actionButtonHeightDp,
                    onClick = onDismiss,
                    modifier = if (full) Modifier.weight(1f) else Modifier,
                )
            }
        }
    }

}

@Composable
private fun ThreeDotThinkingLoader() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "assistantThinking")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 10.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.35f, targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(520, delayMillis = index * 150),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ), label = "thinkingDot$index",
            )
            Box(Modifier.width(7.dp).height(7.dp).background(Color.White.copy(alpha = alpha), RoundedCornerShape(50)))
        }
    }
}

@Composable
private fun ExpressiveAssistantButton(
    contentDescription: String,
    circular: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f), label = "assistantButtonScale")
    Surface(
        modifier = Modifier
            .then(if (circular) Modifier.width(56.dp).height(56.dp) else Modifier.width(56.dp).height(56.dp))
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (enabled) Color(0xFF344F91) else Color(0xFF283044),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * A compact Siri-inspired spectral orb used while voice input is starting/listening and while a
 * typed prompt is being prepared. It is intentionally drawn in Compose so it scales with the island.
 */
@Composable
private fun AssistantSiriVisual(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "assistantSiriVisual")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "assistantSiriRotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1050, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "assistantSiriPulse",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.29f * pulse
        val glowRadius = radius * 1.9f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.96f),
                    androidx.compose.ui.graphics.Color(0xFF72E4FF).copy(alpha = 0.78f),
                    androidx.compose.ui.graphics.Color(0xFF8D75FF).copy(alpha = 0.48f),
                    androidx.compose.ui.graphics.Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color(0xFF70E7FF),
                    androidx.compose.ui.graphics.Color(0xFF9D83FF),
                    androidx.compose.ui.graphics.Color(0xFFFF83C8),
                    androidx.compose.ui.graphics.Color(0xFF70E7FF),
                ),
                center = center,
            ),
            radius = radius,
            center = center,
            style = Stroke(width = size.minDimension * 0.075f, cap = StrokeCap.Round),
        )
        rotate(rotation, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        androidx.compose.ui.graphics.Color.Transparent,
                        androidx.compose.ui.graphics.Color.White,
                        androidx.compose.ui.graphics.Color(0xFF73E4FF),
                        androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    center = center,
                ),
                startAngle = 18f,
                sweepAngle = 225f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.28f, center.y - radius * 1.28f),
                size = Size(radius * 2.56f, radius * 2.56f),
                style = Stroke(width = size.minDimension * 0.045f, cap = StrokeCap.Round),
            )
        }
        drawCircle(
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f),
            radius = radius * 0.24f,
            center = center,
        )
    }
}
