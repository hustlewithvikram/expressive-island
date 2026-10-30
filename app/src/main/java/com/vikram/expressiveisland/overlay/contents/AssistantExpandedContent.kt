package com.vikram.expressiveisland.overlay.contents

import android.app.SearchManager
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
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

            if (assistant.interactivePanel) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask anything…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                    )
                    IconButton(
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
                        Icon(Icons.Rounded.Mic, contentDescription = "Open voice assistant")
                    }
                    IconButton(
                        enabled = query.isNotBlank(),
                        onClick = {
                            val text = query.trim()
                            if (text.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                    putExtra(SearchManager.QUERY, text)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
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
    }
}
