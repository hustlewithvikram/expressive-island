package com.vikram.expressiveisland.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

enum class AssistantActivationMode { AUTO, MANUAL }

private val Context.assistantTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_tile_prefs")

/** The assistant tile's settings, edited on its dedicated settings screen. */
data class AssistantTileSettings(
    /** AUTO reads supported assistant windows; MANUAL only opens from explicit user actions. */
    val activationMode: AssistantActivationMode = AssistantActivationMode.AUTO,
    /** Whether to display the text response of the assistant in the cutout. */
    val displayAnswerInCutout: Boolean = DEFAULT_DISPLAY_ANSWER_IN_CUTOUT,
    /** Maximum cutout height in percent of the screen height (10..80). */
    val maxCutoutHeightPercent: Int = DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT,
    /** Colour of the icon container (the disc behind the assistant glyph). Null = default. */
    val iconContainerColor: CutoutColor? = null,
    /** Whether to use the animated sparkles Lottie icon instead of the static glyph. */
    val useAnimatedIcon: Boolean = DEFAULT_USE_ANIMATED_ICON,
    /** Allow a tap on the otherwise-hidden cutout to open the interactive assistant panel. */
    val tapToOpenAssistant: Boolean = DEFAULT_TAP_TO_OPEN_ASSISTANT,
    /** Automatically expand assistant response events instead of only showing the collapsed pill. */
    val autoExpandAssistantResponses: Boolean = DEFAULT_AUTO_EXPAND_ASSISTANT_RESPONSES,
    /** Read assistant app windows and publish their visible text into the island. */
    val autoReadAssistantEvents: Boolean = DEFAULT_AUTO_READ_ASSISTANT_EVENTS,
    /** Seconds a collapsed assistant response stays visible before it is dismissed. */
    val pillTimeoutSeconds: Int = DEFAULT_PILL_TIMEOUT_SECONDS,
    /** Seconds an expanded assistant response stays open before collapsing. */
    val expandedTimeoutSeconds: Int = DEFAULT_EXPANDED_TIMEOUT_SECONDS,
    val manualExpandedToPillTimeoutSeconds: Int = DEFAULT_MANUAL_EXPANDED_TO_PILL_TIMEOUT_SECONDS,
    val manualAutoHideEnabled: Boolean = DEFAULT_MANUAL_AUTO_HIDE_ENABLED,
    val manualPillToHiddenTimeoutSeconds: Int = DEFAULT_MANUAL_PILL_TO_HIDDEN_TIMEOUT_SECONDS,
    val showAssistantMicButton: Boolean = DEFAULT_SHOW_ASSISTANT_MIC_BUTTON,
    val showAssistantPromptInput: Boolean = DEFAULT_SHOW_ASSISTANT_PROMPT_INPUT,
    val showAssistantSendButton: Boolean = DEFAULT_SHOW_ASSISTANT_SEND_BUTTON,
    /** Collapse the expanded assistant when the user swipes upward. */
    val swipeUpToCollapse: Boolean = DEFAULT_SWIPE_UP_TO_COLLAPSE,
    /** Collapse the expanded assistant when the user taps its background. */
    val tapToCollapse: Boolean = DEFAULT_TAP_TO_COLLAPSE,
) {
    companion object {
        const val DEFAULT_DISPLAY_ANSWER_IN_CUTOUT = true
        const val DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT = 35
        const val DEFAULT_USE_ANIMATED_ICON = false
        const val DEFAULT_TAP_TO_OPEN_ASSISTANT = false
        const val DEFAULT_AUTO_EXPAND_ASSISTANT_RESPONSES = false
        const val DEFAULT_AUTO_READ_ASSISTANT_EVENTS = true
        const val DEFAULT_PILL_TIMEOUT_SECONDS = 6
        const val DEFAULT_EXPANDED_TIMEOUT_SECONDS = 18
        const val DEFAULT_MANUAL_EXPANDED_TO_PILL_TIMEOUT_SECONDS = 15
        const val DEFAULT_MANUAL_AUTO_HIDE_ENABLED = true
        const val DEFAULT_MANUAL_PILL_TO_HIDDEN_TIMEOUT_SECONDS = 10
        const val DEFAULT_SHOW_ASSISTANT_MIC_BUTTON = true
        const val DEFAULT_SHOW_ASSISTANT_PROMPT_INPUT = true
        const val DEFAULT_SHOW_ASSISTANT_SEND_BUTTON = true
        const val DEFAULT_SWIPE_UP_TO_COLLAPSE = true
        const val DEFAULT_TAP_TO_COLLAPSE = false
    }
}

/** Persists the assistant tile's options (answer display, max cutout height, container colour). */
class AssistantTilePreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<AssistantTileSettings> = context.assistantTileDataStore.data.map { prefs ->
        AssistantTileSettings(
            activationMode = runCatching { AssistantActivationMode.valueOf(prefs[ACTIVATION_MODE] ?: AssistantActivationMode.AUTO.name) }.getOrDefault(AssistantActivationMode.AUTO),
            displayAnswerInCutout = prefs[DISPLAY_ANSWER_IN_CUTOUT] ?: AssistantTileSettings.DEFAULT_DISPLAY_ANSWER_IN_CUTOUT,
            maxCutoutHeightPercent = prefs[MAX_CUTOUT_HEIGHT_PERCENT] ?: AssistantTileSettings.DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT,
            iconContainerColor = CutoutColor.deserialize(prefs[ICON_CONTAINER_COLOR]),
            useAnimatedIcon = prefs[USE_ANIMATED_ICON] ?: AssistantTileSettings.DEFAULT_USE_ANIMATED_ICON,
            tapToOpenAssistant = prefs[TAP_TO_OPEN_ASSISTANT] ?: AssistantTileSettings.DEFAULT_TAP_TO_OPEN_ASSISTANT,
            autoExpandAssistantResponses = prefs[AUTO_EXPAND_ASSISTANT_RESPONSES] ?: AssistantTileSettings.DEFAULT_AUTO_EXPAND_ASSISTANT_RESPONSES,
            autoReadAssistantEvents = prefs[AUTO_READ_ASSISTANT_EVENTS] ?: AssistantTileSettings.DEFAULT_AUTO_READ_ASSISTANT_EVENTS,
            pillTimeoutSeconds = prefs[PILL_TIMEOUT_SECONDS] ?: AssistantTileSettings.DEFAULT_PILL_TIMEOUT_SECONDS,
            expandedTimeoutSeconds = prefs[EXPANDED_TIMEOUT_SECONDS] ?: AssistantTileSettings.DEFAULT_EXPANDED_TIMEOUT_SECONDS,
            manualExpandedToPillTimeoutSeconds = prefs[MANUAL_EXPANDED_TO_PILL_TIMEOUT_SECONDS] ?: AssistantTileSettings.DEFAULT_MANUAL_EXPANDED_TO_PILL_TIMEOUT_SECONDS,
            manualAutoHideEnabled = prefs[MANUAL_AUTO_HIDE_ENABLED] ?: AssistantTileSettings.DEFAULT_MANUAL_AUTO_HIDE_ENABLED,
            manualPillToHiddenTimeoutSeconds = prefs[MANUAL_PILL_TO_HIDDEN_TIMEOUT_SECONDS] ?: AssistantTileSettings.DEFAULT_MANUAL_PILL_TO_HIDDEN_TIMEOUT_SECONDS,
            showAssistantMicButton = prefs[SHOW_ASSISTANT_MIC_BUTTON] ?: AssistantTileSettings.DEFAULT_SHOW_ASSISTANT_MIC_BUTTON,
            showAssistantPromptInput = prefs[SHOW_ASSISTANT_PROMPT_INPUT] ?: AssistantTileSettings.DEFAULT_SHOW_ASSISTANT_PROMPT_INPUT,
            showAssistantSendButton = (prefs[SHOW_ASSISTANT_SEND_BUTTON] ?: AssistantTileSettings.DEFAULT_SHOW_ASSISTANT_SEND_BUTTON) &&
                (prefs[SHOW_ASSISTANT_PROMPT_INPUT] ?: AssistantTileSettings.DEFAULT_SHOW_ASSISTANT_PROMPT_INPUT),
            swipeUpToCollapse = prefs[SWIPE_UP_TO_COLLAPSE] ?: AssistantTileSettings.DEFAULT_SWIPE_UP_TO_COLLAPSE,
            tapToCollapse = prefs[TAP_TO_COLLAPSE] ?: AssistantTileSettings.DEFAULT_TAP_TO_COLLAPSE,
        )
    }

    /** Exports the current [AssistantTileSettings] as a JSON string. */
    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("activationMode", s.activationMode.name)
            put("displayAnswerInCutout", s.displayAnswerInCutout)
            put("maxCutoutHeightPercent", s.maxCutoutHeightPercent)
            put("iconContainerColor", s.iconContainerColor?.serialize() ?: JSONObject.NULL)
            put("useAnimatedIcon", s.useAnimatedIcon)
            put("tapToOpenAssistant", s.tapToOpenAssistant)
            put("autoExpandAssistantResponses", s.autoExpandAssistantResponses)
            put("autoReadAssistantEvents", s.autoReadAssistantEvents)
            put("pillTimeoutSeconds", s.pillTimeoutSeconds)
            put("expandedTimeoutSeconds", s.expandedTimeoutSeconds)
            put("manualExpandedToPillTimeoutSeconds", s.manualExpandedToPillTimeoutSeconds)
            put("manualAutoHideEnabled", s.manualAutoHideEnabled)
            put("manualPillToHiddenTimeoutSeconds", s.manualPillToHiddenTimeoutSeconds)
            put("showAssistantMicButton", s.showAssistantMicButton)
            put("showAssistantPromptInput", s.showAssistantPromptInput)
            put("showAssistantSendButton", s.showAssistantSendButton)
            put("swipeUpToCollapse", s.swipeUpToCollapse)
            put("tapToCollapse", s.tapToCollapse)
        }.toString()
    }

    /** Applies the [AssistantTileSettings] object exported by [toJson]; absent fields are left as-is. */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.assistantTileDataStore.edit {
            if (obj.has("activationMode")) {
                val mode = runCatching { AssistantActivationMode.valueOf(obj.getString("activationMode")) }.getOrDefault(AssistantActivationMode.AUTO)
                it[ACTIVATION_MODE] = mode.name
            }
            if (obj.has("displayAnswerInCutout")) it[DISPLAY_ANSWER_IN_CUTOUT] = obj.getBoolean("displayAnswerInCutout")
            if (obj.has("maxCutoutHeightPercent")) it[MAX_CUTOUT_HEIGHT_PERCENT] = obj.getInt("maxCutoutHeightPercent").coerceIn(10, 80)
            if (obj.has("useAnimatedIcon")) it[USE_ANIMATED_ICON] = obj.getBoolean("useAnimatedIcon")
            if (obj.has("tapToOpenAssistant")) it[TAP_TO_OPEN_ASSISTANT] = obj.getBoolean("tapToOpenAssistant")
            if (obj.has("autoExpandAssistantResponses")) it[AUTO_EXPAND_ASSISTANT_RESPONSES] = obj.getBoolean("autoExpandAssistantResponses")
            if (obj.has("autoReadAssistantEvents")) it[AUTO_READ_ASSISTANT_EVENTS] = obj.getBoolean("autoReadAssistantEvents")
            if (obj.has("pillTimeoutSeconds")) it[PILL_TIMEOUT_SECONDS] = obj.getInt("pillTimeoutSeconds").coerceIn(2, 30)
            if (obj.has("expandedTimeoutSeconds")) it[EXPANDED_TIMEOUT_SECONDS] = obj.getInt("expandedTimeoutSeconds").coerceIn(5, 60)
            if (obj.has("manualExpandedToPillTimeoutSeconds")) it[MANUAL_EXPANDED_TO_PILL_TIMEOUT_SECONDS] = obj.getInt("manualExpandedToPillTimeoutSeconds").coerceIn(5, 60)
            if (obj.has("manualAutoHideEnabled")) it[MANUAL_AUTO_HIDE_ENABLED] = obj.getBoolean("manualAutoHideEnabled")
            if (obj.has("manualPillToHiddenTimeoutSeconds")) it[MANUAL_PILL_TO_HIDDEN_TIMEOUT_SECONDS] = obj.getInt("manualPillToHiddenTimeoutSeconds").coerceIn(2, 60)
            if (obj.has("showAssistantMicButton")) it[SHOW_ASSISTANT_MIC_BUTTON] = obj.getBoolean("showAssistantMicButton")
            if (obj.has("showAssistantPromptInput")) {
                val showPrompt = obj.getBoolean("showAssistantPromptInput")
                it[SHOW_ASSISTANT_PROMPT_INPUT] = showPrompt
                if (!showPrompt) it[SHOW_ASSISTANT_SEND_BUTTON] = false
            }
            if (obj.has("swipeUpToCollapse")) it[SWIPE_UP_TO_COLLAPSE] = obj.getBoolean("swipeUpToCollapse")
            if (obj.has("tapToCollapse")) it[TAP_TO_COLLAPSE] = obj.getBoolean("tapToCollapse")
            if (obj.has("showAssistantSendButton")) {
                it[SHOW_ASSISTANT_SEND_BUTTON] =
                    obj.getBoolean("showAssistantSendButton") && (it[SHOW_ASSISTANT_PROMPT_INPUT] ?: true)
            }
            if (obj.has("iconContainerColor")) {
                val raw = if (obj.isNull("iconContainerColor")) null else obj.optString("iconContainerColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
            }
        }
    }

    suspend fun setActivationMode(mode: AssistantActivationMode) = context.assistantTileDataStore.edit {
        it[ACTIVATION_MODE] = mode.name
    }

    suspend fun setDisplayAnswerInCutout(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[DISPLAY_ANSWER_IN_CUTOUT] = enabled
    }

    suspend fun setMaxCutoutHeightPercent(percent: Int) = context.assistantTileDataStore.edit {
        it[MAX_CUTOUT_HEIGHT_PERCENT] = percent.coerceIn(10, 80)
    }

    suspend fun setIconContainerColor(color: CutoutColor?) = context.assistantTileDataStore.edit {
        if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
    }

    suspend fun setUseAnimatedIcon(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[USE_ANIMATED_ICON] = enabled
    }

    suspend fun setTapToOpenAssistant(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[TAP_TO_OPEN_ASSISTANT] = enabled
    }

    suspend fun setAutoExpandAssistantResponses(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[AUTO_EXPAND_ASSISTANT_RESPONSES] = enabled
    }

    suspend fun setAutoReadAssistantEvents(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[AUTO_READ_ASSISTANT_EVENTS] = enabled
    }

    suspend fun setPillTimeoutSeconds(seconds: Int) = context.assistantTileDataStore.edit {
        it[PILL_TIMEOUT_SECONDS] = seconds.coerceIn(2, 30)
    }

    suspend fun setExpandedTimeoutSeconds(seconds: Int) = context.assistantTileDataStore.edit {
        it[EXPANDED_TIMEOUT_SECONDS] = seconds.coerceIn(5, 60)
    }

    suspend fun setManualExpandedToPillTimeoutSeconds(seconds: Int) = context.assistantTileDataStore.edit {
        it[MANUAL_EXPANDED_TO_PILL_TIMEOUT_SECONDS] = seconds.coerceIn(5, 60)
    }

    suspend fun setManualAutoHideEnabled(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[MANUAL_AUTO_HIDE_ENABLED] = enabled
    }

    suspend fun setManualPillToHiddenTimeoutSeconds(seconds: Int) = context.assistantTileDataStore.edit {
        it[MANUAL_PILL_TO_HIDDEN_TIMEOUT_SECONDS] = seconds.coerceIn(2, 60)
    }

    suspend fun setShowAssistantMicButton(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[SHOW_ASSISTANT_MIC_BUTTON] = enabled
    }

    suspend fun setShowAssistantPromptInput(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[SHOW_ASSISTANT_PROMPT_INPUT] = enabled
        if (!enabled) it[SHOW_ASSISTANT_SEND_BUTTON] = false
    }

    suspend fun setShowAssistantSendButton(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[SHOW_ASSISTANT_SEND_BUTTON] = enabled && (it[SHOW_ASSISTANT_PROMPT_INPUT] ?: true)
    }

    suspend fun setSwipeUpToCollapse(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[SWIPE_UP_TO_COLLAPSE] = enabled
    }

    suspend fun setTapToCollapse(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[TAP_TO_COLLAPSE] = enabled
    }

    private companion object {
        val MANUAL_EXPANDED_TO_PILL_TIMEOUT_SECONDS = intPreferencesKey("manual_expanded_to_pill_timeout_seconds")
        val MANUAL_AUTO_HIDE_ENABLED = booleanPreferencesKey("manual_auto_hide_enabled")
        val MANUAL_PILL_TO_HIDDEN_TIMEOUT_SECONDS = intPreferencesKey("manual_pill_to_hidden_timeout_seconds")
        val SHOW_ASSISTANT_MIC_BUTTON = booleanPreferencesKey("show_assistant_mic_button")
        val SHOW_ASSISTANT_PROMPT_INPUT = booleanPreferencesKey("show_assistant_prompt_input")
        val SHOW_ASSISTANT_SEND_BUTTON = booleanPreferencesKey("show_assistant_send_button")
        val SWIPE_UP_TO_COLLAPSE = booleanPreferencesKey("swipe_up_to_collapse")
        val TAP_TO_COLLAPSE = booleanPreferencesKey("tap_to_collapse")
        val ACTIVATION_MODE = stringPreferencesKey("activation_mode")
        val DISPLAY_ANSWER_IN_CUTOUT = booleanPreferencesKey("display_answer_in_cutout")
        val MAX_CUTOUT_HEIGHT_PERCENT = intPreferencesKey("max_cutout_height_percent")
        val ICON_CONTAINER_COLOR = stringPreferencesKey("icon_container_color")
        val USE_ANIMATED_ICON = booleanPreferencesKey("use_animated_icon")
        val TAP_TO_OPEN_ASSISTANT = booleanPreferencesKey("tap_to_open_assistant")
        val AUTO_EXPAND_ASSISTANT_RESPONSES = booleanPreferencesKey("auto_expand_assistant_responses")
        val AUTO_READ_ASSISTANT_EVENTS = booleanPreferencesKey("auto_read_assistant_events")
        val PILL_TIMEOUT_SECONDS = intPreferencesKey("pill_timeout_seconds")
        val EXPANDED_TIMEOUT_SECONDS = intPreferencesKey("expanded_timeout_seconds")
    }
}
