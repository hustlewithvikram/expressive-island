package com.vikram.expressiveisland.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.vikram.expressiveisland.core.CutoutSignal
import com.vikram.expressiveisland.core.AssistantInteractionMode
import com.vikram.expressiveisland.data.AssistantTilePreferences
import com.vikram.expressiveisland.data.AssistantActivationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.vikram.expressiveisland.core.ForegroundAppBus
import com.vikram.expressiveisland.core.IslandEventBus
import com.vikram.expressiveisland.events.MediaPlaybackMonitor
import com.vikram.expressiveisland.events.SystemEventMonitor
import com.vikram.expressiveisland.overlay.island.IslandOverlayController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CutoutAccessibilityService : AccessibilityService() {
    private var overlay: IslandOverlayController? = null
    private var systemEvents: SystemEventMonitor? = null
    private var mediaPlayback: MediaPlaybackMonitor? = null
    private var lastAssistantKey: String? = null
    private var lastAssistantPackage: String? = null
    private var autoReadAssistantEvents = true
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var assistantSettingsJob: Job? = null
    private var lastAssistantInspectionAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        teardownComponents()
        overlay = IslandOverlayController(this).also { it.start() }
        systemEvents = SystemEventMonitor(this).also { it.start() }
        mediaPlayback = MediaPlaybackMonitor(this).also { it.start() }
        instance = this
        assistantSettingsJob?.cancel()
        assistantSettingsJob = serviceScope.launch {
            AssistantTilePreferences(this@CutoutAccessibilityService).settings
                .map { it.activationMode == AssistantActivationMode.AUTO && it.autoReadAssistantEvents }
                .distinctUntilChanged()
                .collect { enabled ->
                    autoReadAssistantEvents = enabled
                    if (!enabled) lastAssistantPackage?.let(::clearAssistant)
                }
        }
        _bound.value = true
    }

    private fun isAssistantPackage(packageName: String): Boolean =
        packageName.lowercase() in ASSISTANT_PACKAGES

    private fun isDisclaimer(text: String): Boolean {
        val lower = text.lowercase()
        return DISCLAIMER_PATTERNS.any { lower.contains(it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkg = ev.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) ForegroundAppBus.update(
            pkg
        )

        if (!isAssistantPackage(pkg)) {
            if (lastAssistantKey != null && ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                clearAssistant(pkg)
            }
            return
        }

        if (ev.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val now = SystemClock.uptimeMillis()
            if (now - lastAssistantInspectionAt < ASSISTANT_INSPECTION_INTERVAL_MS) return
            lastAssistantInspectionAt = now
        }
        inspectAssistantWindow(pkg, ev)
    }

    private fun inspectAssistantWindow(pkg: String, event: AccessibilityEvent) {
        if (!autoReadAssistantEvents) {
            clearAssistant(pkg)
            return
        }
        val rootNode = rootInActiveWindow ?: event.source
        if (rootNode == null) {
            clearAssistant(pkg)
            return
        }

        val textList = ArrayList<String>(MAX_TEXT_NODES)
        val modeHints = ArrayList<String>(MAX_TEXT_NODES)
        collectTextNodes(rootNode, textList, modeHints, 0, 0)
        if (textList.isEmpty() && modeHints.isEmpty()) {
            clearAssistant(pkg)
            return
        }

        val interactionMode = detectAssistantInteractionMode(textList + modeHints, event.eventType)
        val title = textList.firstOrNull()?.take(MAX_TITLE_LENGTH) ?: when (interactionMode) {
            AssistantInteractionMode.VOICE -> "Listening"
            AssistantInteractionMode.PROMPT -> "Prompt"
            AssistantInteractionMode.UNKNOWN -> null
        }
        val responseText = textList.asSequence()
            .drop(1)
            .joinToString("\n")
            .take(MAX_RESPONSE_LENGTH)
            .ifBlank { title.orEmpty() }
        if (title.isNullOrBlank() && responseText.isBlank()) {
            clearAssistant(pkg)
            return
        }

        val key = "$pkg|$interactionMode|$title|$responseText".take(MAX_KEY_LENGTH)
        if (key != lastAssistantKey) {
            lastAssistantKey = key
            lastAssistantPackage = pkg
            IslandEventBus.emit(
                CutoutSignal.Assistant(
                    packageName = pkg,
                    title = title,
                    text = responseText,
                    contentIntent = null,
                    active = true,
                    interactionMode = interactionMode,
                )
            )
        }
    }

    private fun detectAssistantInteractionMode(
        text: List<String>,
        eventType: Int,
    ): AssistantInteractionMode {
        val content = text.joinToString(" ").lowercase()
        return when {
            VOICE_MODE_PATTERNS.any(content::contains) -> AssistantInteractionMode.VOICE
            eventType in PROMPT_INPUT_EVENT_TYPES && PROMPT_MODE_PATTERNS.any(content::contains) ->
                AssistantInteractionMode.PROMPT
            else -> AssistantInteractionMode.UNKNOWN
        }
    }

    private fun clearAssistant(pkg: String) {
        if (lastAssistantKey == null) return
        lastAssistantKey = null
        lastAssistantPackage = null
        IslandEventBus.emit(CutoutSignal.Assistant(packageName = pkg, active = false))
    }

    private fun collectTextNodes(
        node: AccessibilityNodeInfo?,
        list: MutableList<String>,
        modeHints: MutableList<String>,
        depth: Int,
        totalChars: Int,
    ): Int {
        if (node == null || depth > MAX_NODE_DEPTH || list.size >= MAX_TEXT_NODES || totalChars >= MAX_RESPONSE_LENGTH) return totalChars
        var chars = totalChars
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank() &&
            PROMPT_MODE_PATTERNS.any(text.lowercase()::contains) &&
            text !in modeHints
        ) {
            modeHints.add(text)
        }
        if (!text.isNullOrBlank() && text.length > 1 && !isDisclaimer(text)) {
            val remaining = MAX_RESPONSE_LENGTH - chars
            if (remaining > 0) {
                val value = text.take(remaining)
                list.add(value)
                chars += value.length
            }
        }
        val description = node.contentDescription?.toString()?.trim()
        if (!description.isNullOrBlank() && description.length > 1 && description !in modeHints) {
            modeHints.add(description)
        }
        for (i in 0 until minOf(node.childCount, MAX_CHILDREN_PER_NODE)) {
            if (list.size >= MAX_TEXT_NODES || chars >= MAX_RESPONSE_LENGTH) break
            chars = collectTextNodes(node.getChild(i), list, modeHints, depth + 1, chars)
        }
        return chars
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onOrientationChanged(newConfig.orientation)
    }

    override fun onInterrupt() = Unit
    override fun onUnbind(intent: Intent?): Boolean {
        teardown(); return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown(); super.onDestroy()
    }

    private fun teardownComponents() {
        mediaPlayback?.stop(); mediaPlayback = null
        systemEvents?.stop(); systemEvents = null
        overlay?.stop(); overlay = null
    }

    private fun teardown() {
        _bound.value = false
        instance = null
        assistantSettingsJob?.cancel()
        assistantSettingsJob = null
        teardownComponents()
        lastAssistantKey = null
        lastAssistantPackage = null
        lastAssistantInspectionAt = 0L
    }

    companion object {
        private const val ASSISTANT_INSPECTION_INTERVAL_MS = 150L
        private const val MAX_TEXT_NODES = 80
        private const val MAX_NODE_DEPTH = 24
        private const val MAX_CHILDREN_PER_NODE = 64
        private const val MAX_RESPONSE_LENGTH = 4_000
        private const val MAX_TITLE_LENGTH = 160
        private const val MAX_KEY_LENGTH = 4_500
        private val ASSISTANT_PACKAGES = setOf(
            "com.google.android.googlequicksearchbox", "com.google.android.apps.googleassistant",
            "com.google.android.apps.bard", "com.google.android.apps.gemini",
            "com.samsung.android.bixby.agent", "com.samsung.android.bixby.service",
            "com.amazon.dee.app", "com.openai.chatgpt", "com.microsoft.copilot",
        )
        private val VOICE_MODE_PATTERNS = listOf(
            "listening", "speak now", "stop listening", "microphone is on",
            "voice mode active", "end live", "pause live", "live conversation",
            "voice chat active", "listening to you",
        )
        private val PROMPT_INPUT_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
        )
        private val PROMPT_MODE_PATTERNS = listOf(
            "ask gemini", "type a prompt", "enter a prompt", "message gemini",
            "ask anything", "type your message", "enter a message",
        )
        private val DISCLAIMER_PATTERNS = listOf(
            "can make mistakes", "gemini is ai", "gemini is an ai", "display inaccurate info",
            "check responses", "type, talk, or share", "ask gemini", "gemini advanced",
            "share screen with live",
        )
        private var instance: CutoutAccessibilityService? = null
        private val _bound = MutableStateFlow(false)
        val bound: StateFlow<Boolean> = _bound.asStateFlow()
        fun performGlobal(action: Int): Boolean =
            runCatching { instance?.performGlobalAction(action) }.getOrNull() ?: false
    }
}
