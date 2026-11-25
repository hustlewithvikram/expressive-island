package com.vikram.expressiveisland.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.downloadTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "download_tile_prefs")

enum class DownloadPillProgressStyle(val label: String) {
    CIRCULAR("Circular"), PERCENTAGE("Percentage"), LINEAR("Linear"),
    SEGMENTED("Segmented"), WAVY("Wavy"), DOTTED("Dotted"), EXPRESSIVE("Expressive"),
}

enum class DownloadProgressStyle(val label: String) {
    LINEAR("Linear"),
    CIRCULAR("Circular"),
    SEGMENTED("Segmented"),
    WAVY("Wavy"),
    DOTTED("Dotted"),
    EXPRESSIVE("Expressive"),
}

data class DownloadTileSettings(
    val icon: IconSource? = null,
    val showAppIcon: Boolean = false,
    val showText: Boolean = true,
    val showPercentage: Boolean = true,
    val showProgress: Boolean = true,
    val immersiveProgress: Boolean = false,
    val progressStyle: DownloadProgressStyle = DownloadProgressStyle.LINEAR,
    val iconContainerColor: CutoutColor? = null,
    val progressColor: CutoutColor? = null,
    val trackColor: CutoutColor? = null,
    val showPauseResume: Boolean = true,
    val showCancel: Boolean = true,
    val pillShowProgress: Boolean = true,
    val pillProgressColor: CutoutColor? = null,
    val pillTrackColor: CutoutColor? = null,
    val actionButtonColor: CutoutColor? = null,
    val pillProgressStyle: DownloadPillProgressStyle = DownloadPillProgressStyle.CIRCULAR,
)

class DownloadTilePreferences(private val context: Context) : JsonSerializable {
    val settings: Flow<DownloadTileSettings> = context.downloadTileDataStore.data.map { prefs ->
        DownloadTileSettings(
            icon = prefs[ICON]?.let(IconSource::decode),
            showAppIcon = prefs[SHOW_APP_ICON] ?: false,
            showText = prefs[SHOW_TEXT] ?: true,
            showPercentage = prefs[SHOW_PERCENTAGE] ?: true,
            showProgress = prefs[SHOW_PROGRESS] ?: true,
            immersiveProgress = prefs[IMMERSIVE_PROGRESS] ?: false,
            progressStyle = runCatching {
                DownloadProgressStyle.valueOf(prefs[PROGRESS_STYLE] ?: DownloadProgressStyle.LINEAR.name)
            }.getOrDefault(DownloadProgressStyle.LINEAR),
            iconContainerColor = CutoutColor.deserialize(prefs[ICON_CONTAINER_COLOR]),
            progressColor = CutoutColor.deserialize(prefs[PROGRESS_COLOR]),
            trackColor = CutoutColor.deserialize(prefs[TRACK_COLOR]),
            showPauseResume = prefs[SHOW_PAUSE_RESUME] ?: true,
            showCancel = prefs[SHOW_CANCEL] ?: true,
            pillShowProgress = prefs[PILL_SHOW_PROGRESS] ?: true,
            pillProgressColor = CutoutColor.deserialize(prefs[PILL_PROGRESS_COLOR]),
            pillTrackColor = CutoutColor.deserialize(prefs[PILL_TRACK_COLOR]),
            actionButtonColor = CutoutColor.deserialize(prefs[ACTION_BUTTON_COLOR]),
            pillProgressStyle = runCatching {
                DownloadPillProgressStyle.valueOf(prefs[PILL_PROGRESS_STYLE] ?: DownloadPillProgressStyle.CIRCULAR.name)
            }.getOrDefault(DownloadPillProgressStyle.CIRCULAR),
        )
    }

    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("icon", s.icon?.encode() ?: JSONObject.NULL)
            put("showAppIcon", s.showAppIcon)
            put("showText", s.showText)
            put("showPercentage", s.showPercentage)
            put("showProgress", s.showProgress)
            put("immersiveProgress", s.immersiveProgress)
            put("progressStyle", s.progressStyle.name)
            put("iconContainerColor", s.iconContainerColor?.serialize() ?: JSONObject.NULL)
            put("progressColor", s.progressColor?.serialize() ?: JSONObject.NULL)
            put("trackColor", s.trackColor?.serialize() ?: JSONObject.NULL)
            put("showPauseResume", s.showPauseResume)
            put("showCancel", s.showCancel)
            put("pillShowProgress", s.pillShowProgress)
            put("pillProgressColor", s.pillProgressColor?.serialize() ?: JSONObject.NULL)
            put("pillTrackColor", s.pillTrackColor?.serialize() ?: JSONObject.NULL)
            put("pillProgressStyle", s.pillProgressStyle.name)
            put("actionButtonColor", s.actionButtonColor?.serialize() ?: JSONObject.NULL)
        }.toString()
    }

    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.downloadTileDataStore.edit { prefs ->
            if (obj.has("icon")) {
                val icon = if (obj.isNull("icon")) null else IconSource.decode(obj.optString("icon"))
                if (icon == null) prefs.remove(ICON) else prefs[ICON] = icon.encode()
            }
            if (obj.has("showAppIcon")) prefs[SHOW_APP_ICON] = obj.getBoolean("showAppIcon")
            if (obj.has("showText")) prefs[SHOW_TEXT] = obj.getBoolean("showText")
            if (obj.has("showPercentage")) prefs[SHOW_PERCENTAGE] = obj.getBoolean("showPercentage")
            if (obj.has("showProgress")) prefs[SHOW_PROGRESS] = obj.getBoolean("showProgress")
            if (obj.has("immersiveProgress")) prefs[IMMERSIVE_PROGRESS] = obj.getBoolean("immersiveProgress")
            if (obj.has("progressStyle")) prefs[PROGRESS_STYLE] = runCatching {
                DownloadProgressStyle.valueOf(obj.getString("progressStyle")).name
            }.getOrDefault(DownloadProgressStyle.LINEAR.name)
            fun applyColor(name: String, key: Preferences.Key<String>) {
                if (obj.has(name)) {
                    val raw = if (obj.isNull(name)) null else obj.optString(name)
                    val color = CutoutColor.deserialize(raw)
                    if (color == null) prefs.remove(key) else prefs[key] = color.serialize()
                }
            }
            applyColor("iconContainerColor", ICON_CONTAINER_COLOR)
            applyColor("progressColor", PROGRESS_COLOR)
            applyColor("trackColor", TRACK_COLOR)
            applyColor("pillProgressColor", PILL_PROGRESS_COLOR)
            applyColor("pillTrackColor", PILL_TRACK_COLOR)
            applyColor("actionButtonColor", ACTION_BUTTON_COLOR)
            if (obj.has("pillShowProgress")) prefs[PILL_SHOW_PROGRESS] = obj.getBoolean("pillShowProgress")
            if (obj.has("showPauseResume")) prefs[SHOW_PAUSE_RESUME] = obj.getBoolean("showPauseResume")
            if (obj.has("showCancel")) prefs[SHOW_CANCEL] = obj.getBoolean("showCancel")
            if (obj.has("pillProgressStyle")) prefs[PILL_PROGRESS_STYLE] = runCatching {
                DownloadPillProgressStyle.valueOf(obj.getString("pillProgressStyle")).name
            }.getOrDefault(DownloadPillProgressStyle.CIRCULAR.name)
        }
    }

    suspend fun setIcon(icon: IconSource?) = context.downloadTileDataStore.edit {
        if (icon == null) it.remove(ICON) else it[ICON] = icon.encode()
    }
    suspend fun setShowAppIcon(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_APP_ICON] = enabled }
    suspend fun setShowText(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_TEXT] = enabled }
    suspend fun setShowPercentage(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_PERCENTAGE] = enabled }
    suspend fun setShowProgress(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_PROGRESS] = enabled }
    suspend fun setImmersiveProgress(enabled: Boolean) = context.downloadTileDataStore.edit { it[IMMERSIVE_PROGRESS] = enabled }
    suspend fun setProgressStyle(style: DownloadProgressStyle) = context.downloadTileDataStore.edit { it[PROGRESS_STYLE] = style.name }
    suspend fun setIconContainerColor(color: CutoutColor?) = context.downloadTileDataStore.edit {
        if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
    }
    suspend fun setProgressColor(color: CutoutColor?) = context.downloadTileDataStore.edit {
        if (color == null) it.remove(PROGRESS_COLOR) else it[PROGRESS_COLOR] = color.serialize()
    }
    suspend fun setTrackColor(color: CutoutColor?) = context.downloadTileDataStore.edit {
        if (color == null) it.remove(TRACK_COLOR) else it[TRACK_COLOR] = color.serialize()
    }
    suspend fun setShowPauseResume(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_PAUSE_RESUME] = enabled }
    suspend fun setShowCancel(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_CANCEL] = enabled }
    suspend fun setPillShowProgress(enabled: Boolean) = context.downloadTileDataStore.edit { it[PILL_SHOW_PROGRESS] = enabled }
    suspend fun setPillProgressColor(color: CutoutColor?) = context.downloadTileDataStore.edit { if (color == null) it.remove(PILL_PROGRESS_COLOR) else it[PILL_PROGRESS_COLOR] = color.serialize() }
    suspend fun setPillTrackColor(color: CutoutColor?) = context.downloadTileDataStore.edit { if (color == null) it.remove(PILL_TRACK_COLOR) else it[PILL_TRACK_COLOR] = color.serialize() }
    suspend fun setActionButtonColor(color: CutoutColor?) = context.downloadTileDataStore.edit { if (color == null) it.remove(ACTION_BUTTON_COLOR) else it[ACTION_BUTTON_COLOR] = color.serialize() }
    suspend fun setPillProgressStyle(style: DownloadPillProgressStyle) = context.downloadTileDataStore.edit {
        it[PILL_PROGRESS_STYLE] = style.name
    }

    private companion object {
        val ICON = stringPreferencesKey("icon")
        val SHOW_APP_ICON = booleanPreferencesKey("show_app_icon")
        val SHOW_TEXT = booleanPreferencesKey("show_text")
        val SHOW_PERCENTAGE = booleanPreferencesKey("show_percentage")
        val SHOW_PROGRESS = booleanPreferencesKey("show_progress")
        val IMMERSIVE_PROGRESS = booleanPreferencesKey("immersive_progress")
        val PROGRESS_STYLE = stringPreferencesKey("progress_style")
        val ICON_CONTAINER_COLOR = stringPreferencesKey("icon_container_color")
        val PROGRESS_COLOR = stringPreferencesKey("progress_color")
        val TRACK_COLOR = stringPreferencesKey("track_color")
        val SHOW_PAUSE_RESUME = booleanPreferencesKey("show_pause_resume")
        val SHOW_CANCEL = booleanPreferencesKey("show_cancel")
        val PILL_PROGRESS_STYLE = stringPreferencesKey("pill_progress_style")
        val PILL_SHOW_PROGRESS = booleanPreferencesKey("pill_show_progress")
        val PILL_PROGRESS_COLOR = stringPreferencesKey("pill_progress_color")
        val PILL_TRACK_COLOR = stringPreferencesKey("pill_track_color")
        val ACTION_BUTTON_COLOR = stringPreferencesKey("action_button_color")
    }
}
