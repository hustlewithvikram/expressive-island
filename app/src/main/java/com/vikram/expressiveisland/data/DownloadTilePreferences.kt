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

enum class DownloadProgressStyle(val label: String) {
    LINEAR("Linear"),
    CIRCULAR("Circular"),
    SEGMENTED("Segmented"),
}

data class DownloadTileSettings(
    val icon: IconSource? = null,
    val showText: Boolean = true,
    val showPercentage: Boolean = true,
    val showProgress: Boolean = true,
    val progressStyle: DownloadProgressStyle = DownloadProgressStyle.LINEAR,
    val iconContainerColor: CutoutColor? = null,
    val progressColor: CutoutColor? = null,
    val trackColor: CutoutColor? = null,
)

class DownloadTilePreferences(private val context: Context) : JsonSerializable {
    val settings: Flow<DownloadTileSettings> = context.downloadTileDataStore.data.map { prefs ->
        DownloadTileSettings(
            icon = prefs[ICON]?.let(IconSource::decode),
            showText = prefs[SHOW_TEXT] ?: true,
            showPercentage = prefs[SHOW_PERCENTAGE] ?: true,
            showProgress = prefs[SHOW_PROGRESS] ?: true,
            progressStyle = runCatching {
                DownloadProgressStyle.valueOf(prefs[PROGRESS_STYLE] ?: DownloadProgressStyle.LINEAR.name)
            }.getOrDefault(DownloadProgressStyle.LINEAR),
            iconContainerColor = CutoutColor.deserialize(prefs[ICON_CONTAINER_COLOR]),
            progressColor = CutoutColor.deserialize(prefs[PROGRESS_COLOR]),
            trackColor = CutoutColor.deserialize(prefs[TRACK_COLOR]),
        )
    }

    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("icon", s.icon?.encode() ?: JSONObject.NULL)
            put("showText", s.showText)
            put("showPercentage", s.showPercentage)
            put("showProgress", s.showProgress)
            put("progressStyle", s.progressStyle.name)
            put("iconContainerColor", s.iconContainerColor?.serialize() ?: JSONObject.NULL)
            put("progressColor", s.progressColor?.serialize() ?: JSONObject.NULL)
            put("trackColor", s.trackColor?.serialize() ?: JSONObject.NULL)
        }.toString()
    }

    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.downloadTileDataStore.edit { prefs ->
            if (obj.has("icon")) {
                val icon = if (obj.isNull("icon")) null else IconSource.decode(obj.optString("icon"))
                if (icon == null) prefs.remove(ICON) else prefs[ICON] = icon.encode()
            }
            if (obj.has("showText")) prefs[SHOW_TEXT] = obj.getBoolean("showText")
            if (obj.has("showPercentage")) prefs[SHOW_PERCENTAGE] = obj.getBoolean("showPercentage")
            if (obj.has("showProgress")) prefs[SHOW_PROGRESS] = obj.getBoolean("showProgress")
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
        }
    }

    suspend fun setIcon(icon: IconSource?) = context.downloadTileDataStore.edit {
        if (icon == null) it.remove(ICON) else it[ICON] = icon.encode()
    }
    suspend fun setShowText(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_TEXT] = enabled }
    suspend fun setShowPercentage(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_PERCENTAGE] = enabled }
    suspend fun setShowProgress(enabled: Boolean) = context.downloadTileDataStore.edit { it[SHOW_PROGRESS] = enabled }
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

    private companion object {
        val ICON = stringPreferencesKey("icon")
        val SHOW_TEXT = booleanPreferencesKey("show_text")
        val SHOW_PERCENTAGE = booleanPreferencesKey("show_percentage")
        val SHOW_PROGRESS = booleanPreferencesKey("show_progress")
        val PROGRESS_STYLE = stringPreferencesKey("progress_style")
        val ICON_CONTAINER_COLOR = stringPreferencesKey("icon_container_color")
        val PROGRESS_COLOR = stringPreferencesKey("progress_color")
        val TRACK_COLOR = stringPreferencesKey("track_color")
    }
}
