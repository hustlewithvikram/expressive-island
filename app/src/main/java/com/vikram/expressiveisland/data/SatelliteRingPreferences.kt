package com.vikram.expressiveisland.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.satelliteRingStore by preferencesDataStore(name = "satellite_ring")

enum class SatelliteRingStyle(val label: String) {
    SOLID("Classic"),
    DOTTED("Dotted"),
    WAVY("Wavy"),
    EXPRESSIVE("MD3 Expressive"),
}

data class SatelliteRingSettings(
    val enabled: Boolean = true,
    val progressColor: CutoutColor? = null,
    val trackColor: CutoutColor? = null,
    val thicknessDp: Float = 2.5f,
    val style: SatelliteRingStyle = SatelliteRingStyle.EXPRESSIVE,
    val clockwise: Boolean = true,
)

class SatelliteRingPreferences(private val context: Context) {
    val settings: Flow<SatelliteRingSettings> = context.satelliteRingStore.data.map { p ->
        SatelliteRingSettings(
            enabled = p[ENABLED] ?: true,
            progressColor = CutoutColor.deserialize(p[PROGRESS_COLOR]),
            trackColor = CutoutColor.deserialize(p[TRACK_COLOR]),
            thicknessDp = (p[THICKNESS] ?: 2.5f).coerceIn(1f, 6f),
            style = runCatching { SatelliteRingStyle.valueOf(p[STYLE] ?: SatelliteRingStyle.EXPRESSIVE.name) }
                .getOrDefault(SatelliteRingStyle.EXPRESSIVE),
            clockwise = p[CLOCKWISE] ?: true,
        )
    }

    suspend fun setEnabled(value: Boolean) { context.satelliteRingStore.edit { it[ENABLED] = value } }
    suspend fun setProgressColor(value: CutoutColor?) { context.satelliteRingStore.edit { p -> if (value == null) p.remove(PROGRESS_COLOR) else p[PROGRESS_COLOR] = value.serialize() } }
    suspend fun setTrackColor(value: CutoutColor?) { context.satelliteRingStore.edit { p -> if (value == null) p.remove(TRACK_COLOR) else p[TRACK_COLOR] = value.serialize() } }
    suspend fun setThickness(value: Float) { context.satelliteRingStore.edit { it[THICKNESS] = value.coerceIn(1f, 6f) } }
    suspend fun setStyle(value: SatelliteRingStyle) { context.satelliteRingStore.edit { it[STYLE] = value.name } }
    suspend fun setClockwise(value: Boolean) { context.satelliteRingStore.edit { it[CLOCKWISE] = value } }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val PROGRESS_COLOR = stringPreferencesKey("progress_color")
        val TRACK_COLOR = stringPreferencesKey("track_color")
        val THICKNESS = floatPreferencesKey("thickness_dp")
        val STYLE = stringPreferencesKey("style")
        val CLOCKWISE = booleanPreferencesKey("clockwise")
    }
}
