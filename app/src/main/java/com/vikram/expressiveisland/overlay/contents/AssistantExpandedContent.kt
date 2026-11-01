package com.vikram.expressiveisland.overlay.contents

import android.app.SearchManager
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.vikram.expressiveisland.core.AssistantInteractionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.onFocusChanged
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
) {
    val assistant = event.assistant ?: return
    val contentColor = LocalContentColor.current
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val maxCutoutHeightDp =
        (configuration.screenHeightDp * assistant.maxCutoutHeightPercent / 100).dp
    val maxHeaderWidthDp = (configuration.screenWidthDp * 0.47f).dp
    var query by remember(event.id) { mutableStateOf("") }
    var voiceMode by remember(event.id) { mutableStateOf(assistant.interactionMode == AssistantInteractionMode.VOICE) }
    var promptMode by remember(event.id) { mutableStateOf(assistant.interactionMode == AssistantInteractionMode.PROMPT) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxCutoutHeightDp)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp + collapsedHeightDp.dp,
                bottom = 14.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val hDp = (coordinates.size.height / density).toInt()
                    if (hDp > 0) onHeightMeasured?.invoke(hDp + 28 + collapsedHeightDp)
                },
        ) {
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

            if (assistant.interactivePanel) {
                if (voiceMode || promptMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AssistantSiriVisual(
                            modifier = Modifier.width(if (voiceMode) 112.dp else 64.dp)
                                .height(if (voiceMode) 112.dp else 64.dp),
                        )
                        Text(
                            text = if (voiceMode) "Starting voice assistant…" else "Preparing your prompt…",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.76f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Hi! What can I help you with?",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = contentColor,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Type a question below, or use the microphone to open your device's voice assistant.",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.72f),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            promptMode = it.isNotBlank()
                            if (it.isNotBlank()) voiceMode = false
                        },
                        modifier = Modifier.weight(1f).onFocusChanged {
                            onInputFocusChange(it.isFocused)
                            if (it.isFocused) promptMode = true
                        },
                        placeholder = { Text("Ask anything…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                    )
                    IconButton(
                        onClick = {
                            voiceMode = true
                            promptMode = false
                            scope.launch {
                                delay(650)
                                val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(voiceIntent) }.onFailure {
                                    val fallback = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(fallback) }
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Open voice assistant")
                    }
                    IconButton(
                        enabled = query.isNotBlank(),
                        onClick = {
                            val text = query.trim()
                            if (text.isNotEmpty()) {
                                promptMode = true
                                voiceMode = false
                                scope.launch {
                                    delay(420)
                                    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                        putExtra(SearchManager.QUERY, text)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    runCatching { context.startActivity(intent) }
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = "Search question")
                    }
                }
                Text(
                    text = "Text questions open web search; voice uses your configured Android assistant.",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.58f),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
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
    }
}
