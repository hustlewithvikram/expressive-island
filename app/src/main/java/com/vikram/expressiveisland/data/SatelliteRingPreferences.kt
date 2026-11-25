package com.vikram.expressiveisland.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.satelliteRingStore by preferencesDataStore(name = "satellite_ring")

enum class SatelliteRingStyle(val label: String) {
    SOLID("Solid"),
    DOTTED("Dotted"),
    WAVY("Wavy"),
    EXPRESSIVE("Expressive"),
}

data class SatelliteRingSettings(
    val enabled: Boolean = true,
    val progressColor: CutoutColor? = null,
    val trackColor: CutoutColor? = null,
    val thicknessDp: Float = 2.5f,
    val diameterScale: Float = 0.94f,
    val opacity: Float = 1f,
    val style: SatelliteRingStyle = SatelliteRingStyle.EXPRESSIVE,
    val clockwise: Boolean = true,
    val downloadProgressColor: CutoutColor? = null,
    val downloadTrackColor: CutoutColor? = null,
)

class SatelliteRingPreferences(private val context: Context) : JsonSerializable {
    val settings: Flow<SatelliteRingSettings> = context.satelliteRingStore.data.map { p ->
        SatelliteRingSettings(
            enabled = p[ENABLED] ?: true,
            progressColor = CutoutColor.deserialize(p[PROGRESS_COLOR]),
            trackColor = CutoutColor.deserialize(p[TRACK_COLOR]),
            thicknessDp = (p[THICKNESS] ?: 2.5f).coerceIn(1f, 6f),
            diameterScale = (p[DIAMETER_SCALE] ?: 0.94f).coerceIn(0.88f, 1f),
            opacity = (p[OPACITY] ?: 1f).coerceIn(0.2f, 1f),
            style = runCatching { SatelliteRingStyle.valueOf(p[STYLE] ?: SatelliteRingStyle.EXPRESSIVE.name) }
                .getOrDefault(SatelliteRingStyle.EXPRESSIVE),
            clockwise = p[CLOCKWISE] ?: true,
            downloadProgressColor = CutoutColor.deserialize(p[DOWNLOAD_PROGRESS_COLOR]),
            downloadTrackColor = CutoutColor.deserialize(p[DOWNLOAD_TRACK_COLOR]),
        )
    }

    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("enabled", s.enabled)
            put("progressColor", s.progressColor?.serialize() ?: JSONObject.NULL)
            put("trackColor", s.trackColor?.serialize() ?: JSONObject.NULL)
            put("thicknessDp", s.thicknessDp.toDouble())
            put("diameterScale", s.diameterScale.toDouble())
            put("opacity", s.opacity.toDouble())
            put("style", s.style.name)
            put("clockwise", s.clockwise)
            put("downloadProgressColor", s.downloadProgressColor?.serialize() ?: JSONObject.NULL)
            put("downloadTrackColor", s.downloadTrackColor?.serialize() ?: JSONObject.NULL)
        }.toString()
    }

    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.satelliteRingStore.edit { prefs ->
            fun applyColor(name: String, key: androidx.datastore.preferences.core.Preferences.Key<String>) {
                if (!obj.has(name)) return
                val raw = if (obj.isNull(name)) null else obj.optString(name)
                val color = CutoutColor.deserialize(raw)
                if (color == null) prefs.remove(key) else prefs[key] = color.serialize()
            }
            if (obj.has("enabled")) prefs[ENABLED] = obj.optBoolean("enabled")
            applyColor("progressColor", PROGRESS_COLOR)
            applyColor("trackColor", TRACK_COLOR)
            if (obj.has("thicknessDp")) prefs[THICKNESS] = obj.optDouble("thicknessDp", 2.5).toFloat().coerceIn(1f, 6f)
            if (obj.has("diameterScale")) prefs[DIAMETER_SCALE] = obj.optDouble("diameterScale", 0.94).toFloat().coerceIn(0.88f, 1f)
            if (obj.has("opacity")) prefs[OPACITY] = obj.optDouble("opacity", 1.0).toFloat().coerceIn(0.2f, 1f)
            if (obj.has("style")) {
                prefs[STYLE] = runCatching { SatelliteRingStyle.valueOf(obj.getString("style")).name }
                    .getOrDefault(SatelliteRingStyle.EXPRESSIVE.name)
            }
            if (obj.has("clockwise")) prefs[CLOCKWISE] = obj.optBoolean("clockwise")
            applyColor("downloadProgressColor", DOWNLOAD_PROGRESS_COLOR)
            applyColor("downloadTrackColor", DOWNLOAD_TRACK_COLOR)
        }
    }

    suspend fun setEnabled(value: Boolean) { context.satelliteRingStore.edit { it[ENABLED] = value } }
    suspend fun setProgressColor(value: CutoutColor?) { context.satelliteRingStore.edit { p -> if (value == null) p.remove(PROGRESS_COLOR) else p[PROGRESS_COLOR] = value.serialize() } }
    suspend fun setTrackColor(value: CutoutColor?) { context.satelliteRingStore.edit { p -> if (value == null) p.remove(TRACK_COLOR) else p[TRACK_COLOR] = value.serialize() } }
    suspend fun setThickness(value: Float) { context.satelliteRingStore.edit { it[THICKNESS] = value.coerceIn(1f, 6f) } }
    suspend fun setDiameterScale(value: Float) { context.satelliteRingStore.edit { it[DIAMETER_SCALE] = value.coerceIn(0.88f, 1f) } }
    suspend fun setOpacity(value: Float) { context.satelliteRingStore.edit { it[OPACITY] = value.coerceIn(0.2f, 1f) } }
    suspend fun setStyle(value: SatelliteRingStyle) { context.satelliteRingStore.edit { it[STYLE] = value.name } }
    suspend fun setClockwise(value: Boolean) { context.satelliteRingStore.edit { it[CLOCKWISE] = value } }
    suspend fun setDownloadProgressColor(value: CutoutColor?) { context.satelliteRingStore.edit { p -> if (value == null) p.remove(DOWNLOAD_PROGRESS_COLOR) else p[DOWNLOAD_PROGRESS_COLOR] = value.serialize() } }
    suspend fun setDownloadTrackColor(value: CutoutColor?) { context.satelliteRingStore.edit { p -> if (value == null) p.remove(DOWNLOAD_TRACK_COLOR) else p[DOWNLOAD_TRACK_COLOR] = value.serialize() } }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val PROGRESS_COLOR = stringPreferencesKey("progress_color")
        val TRACK_COLOR = stringPreferencesKey("track_color")
        val THICKNESS = floatPreferencesKey("thickness_dp")
        val DIAMETER_SCALE = floatPreferencesKey("diameter_scale")
        val OPACITY = floatPreferencesKey("opacity")
        val STYLE = stringPreferencesKey("style")
        val CLOCKWISE = booleanPreferencesKey("clockwise")
        val DOWNLOAD_PROGRESS_COLOR = stringPreferencesKey("download_progress_color")
        val DOWNLOAD_TRACK_COLOR = stringPreferencesKey("download_track_color")
    }
}
