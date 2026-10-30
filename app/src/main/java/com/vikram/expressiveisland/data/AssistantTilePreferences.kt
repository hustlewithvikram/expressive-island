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

private val Context.assistantTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_tile_prefs")

/** The assistant tile's settings, edited on its dedicated settings screen. */
data class AssistantTileSettings(
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
) {
    companion object {
        const val DEFAULT_DISPLAY_ANSWER_IN_CUTOUT = true
        const val DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT = 35
        const val DEFAULT_USE_ANIMATED_ICON = false
        const val DEFAULT_TAP_TO_OPEN_ASSISTANT = false
        const val DEFAULT_AUTO_EXPAND_ASSISTANT_RESPONSES = false
    }
}

/** Persists the assistant tile's options (answer display, max cutout height, container colour). */
class AssistantTilePreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<AssistantTileSettings> = context.assistantTileDataStore.data.map { prefs ->
        AssistantTileSettings(
            displayAnswerInCutout = prefs[DISPLAY_ANSWER_IN_CUTOUT] ?: AssistantTileSettings.DEFAULT_DISPLAY_ANSWER_IN_CUTOUT,
            maxCutoutHeightPercent = prefs[MAX_CUTOUT_HEIGHT_PERCENT] ?: AssistantTileSettings.DEFAULT_MAX_CUTOUT_HEIGHT_PERCENT,
            iconContainerColor = CutoutColor.deserialize(prefs[ICON_CONTAINER_COLOR]),
            useAnimatedIcon = prefs[USE_ANIMATED_ICON] ?: AssistantTileSettings.DEFAULT_USE_ANIMATED_ICON,
            tapToOpenAssistant = prefs[TAP_TO_OPEN_ASSISTANT] ?: AssistantTileSettings.DEFAULT_TAP_TO_OPEN_ASSISTANT,
            autoExpandAssistantResponses = prefs[AUTO_EXPAND_ASSISTANT_RESPONSES] ?: AssistantTileSettings.DEFAULT_AUTO_EXPAND_ASSISTANT_RESPONSES,
        )
    }

    /** Exports the current [AssistantTileSettings] as a JSON string. */
    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("displayAnswerInCutout", s.displayAnswerInCutout)
            put("maxCutoutHeightPercent", s.maxCutoutHeightPercent)
            put("iconContainerColor", s.iconContainerColor?.serialize() ?: JSONObject.NULL)
            put("useAnimatedIcon", s.useAnimatedIcon)
            put("tapToOpenAssistant", s.tapToOpenAssistant)
            put("autoExpandAssistantResponses", s.autoExpandAssistantResponses)
        }.toString()
    }

    /** Applies the [AssistantTileSettings] object exported by [toJson]; absent fields are left as-is. */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.assistantTileDataStore.edit {
            if (obj.has("displayAnswerInCutout")) it[DISPLAY_ANSWER_IN_CUTOUT] = obj.getBoolean("displayAnswerInCutout")
            if (obj.has("maxCutoutHeightPercent")) it[MAX_CUTOUT_HEIGHT_PERCENT] = obj.getInt("maxCutoutHeightPercent").coerceIn(10, 80)
            if (obj.has("useAnimatedIcon")) it[USE_ANIMATED_ICON] = obj.getBoolean("useAnimatedIcon")
            if (obj.has("tapToOpenAssistant")) it[TAP_TO_OPEN_ASSISTANT] = obj.getBoolean("tapToOpenAssistant")
            if (obj.has("autoExpandAssistantResponses")) it[AUTO_EXPAND_ASSISTANT_RESPONSES] = obj.getBoolean("autoExpandAssistantResponses")
            if (obj.has("iconContainerColor")) {
                val raw = if (obj.isNull("iconContainerColor")) null else obj.optString("iconContainerColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
            }
        }
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
        // Enabling the interactive panel starts in manual mode; users can opt back into
        // automatic response expansion with its dedicated setting.
        if (enabled) it[AUTO_EXPAND_ASSISTANT_RESPONSES] = false
    }

    suspend fun setAutoExpandAssistantResponses(enabled: Boolean) = context.assistantTileDataStore.edit {
        it[AUTO_EXPAND_ASSISTANT_RESPONSES] = enabled
    }

    private companion object {
        val DISPLAY_ANSWER_IN_CUTOUT = booleanPreferencesKey("display_answer_in_cutout")
        val MAX_CUTOUT_HEIGHT_PERCENT = intPreferencesKey("max_cutout_height_percent")
        val ICON_CONTAINER_COLOR = stringPreferencesKey("icon_container_color")
        val USE_ANIMATED_ICON = booleanPreferencesKey("use_animated_icon")
        val TAP_TO_OPEN_ASSISTANT = booleanPreferencesKey("tap_to_open_assistant")
        val AUTO_EXPAND_ASSISTANT_RESPONSES = booleanPreferencesKey("auto_expand_assistant_responses")
    }
}
