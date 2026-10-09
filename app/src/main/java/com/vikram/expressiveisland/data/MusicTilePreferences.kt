package com.vikram.expressiveisland.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.musicTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "music_tile_prefs")

data class MusicButtonStyle(
    val color: CutoutColor?,
    val opacity: Float,
    val cornerPercent: Int,
    val filled: Boolean = DEFAULT_FILLED,
) {
    companion object {
        const val DEFAULT_OPACITY = 1f
        const val DEFAULT_CORNER_PERCENT = 50
        const val MIN_CORNER_PERCENT = 0
        const val MAX_CORNER_PERCENT = 50
        const val DEFAULT_FILLED = false
        const val ROUNDED_CORNER_PERCENT = 30
        val DEFAULT = MusicButtonStyle(null, DEFAULT_OPACITY, DEFAULT_CORNER_PERCENT, DEFAULT_FILLED)
        val ROUNDED = MusicButtonStyle(null, DEFAULT_OPACITY, ROUNDED_CORNER_PERCENT, true)
        val PILL = MusicButtonStyle(null, DEFAULT_OPACITY, MAX_CORNER_PERCENT, true)
        val PRESETS = listOf(ROUNDED, PILL)
    }
}

enum class SeekButtonMode(val label: String) { LEFT("Left button"), RIGHT("Right button"), BOTH("Both buttons") }

enum class MusicProgressStyle(val label: String) {
    WAVY("Wavy"),
    LINEAR("Linear"),
    CIRCULAR("Circular"),
    CIRCULAR_WAVY("Wavy circular"),
}

enum class MusicVisualizerStyle(val label: String) {
    BARS("Classic bars"),
    MIRRORED("Mirrored bars"),
    DOTS("Dancing dots"),
    WAVE("Waveform"),
    PULSE("Pulse"),
}

data class MusicTileSettings(
    val showAlbumArt: Boolean = DEFAULT_SHOW_ALBUM_ART,
    val rotateAlbumArt: Boolean = DEFAULT_ROTATE_ALBUM_ART,
    val albumArtStroke: Boolean = DEFAULT_ALBUM_ART_STROKE,
    val albumArtStrokeColor: CutoutColor? = null,
    val expandOnPlay: Boolean = DEFAULT_EXPAND_ON_PLAY,
    val visibleInPlayerApp: Boolean = DEFAULT_VISIBLE_IN_PLAYER_APP,
    val showControls: Boolean = DEFAULT_SHOW_CONTROLS,
    val skipButton: MusicButtonStyle = MusicButtonStyle.DEFAULT,
    val playPauseButton: MusicButtonStyle = MusicButtonStyle.DEFAULT,
    val skipIconColor: CutoutColor? = null,
    val playPauseIconColor: CutoutColor? = null,
    val seekIconColor: CutoutColor? = null,
    val showProgress: Boolean = DEFAULT_SHOW_PROGRESS,
    val progressStyle: MusicProgressStyle = MusicProgressStyle.WAVY,
    val showVisualizer: Boolean = true,
    val seekButtonMode: SeekButtonMode = SeekButtonMode.BOTH,
    val leftSeekEnabled: Boolean = true,
    val rightSeekEnabled: Boolean = true,
    val leftSeekSeconds: Int = 5,
    val rightSeekSeconds: Int = 5,
    val showPlayPauseIcon: Boolean = true,
    val showPlayPauseText: Boolean = true,
    val visualizerStyle: MusicVisualizerStyle = MusicVisualizerStyle.BARS,
    val expandedBackground: Boolean = DEFAULT_EXPANDED_BACKGROUND,
    val expandedBackgroundBlur: Float = DEFAULT_EXPANDED_BACKGROUND_BLUR,
) {
    companion object {
        const val DEFAULT_SHOW_ALBUM_ART = true
        const val DEFAULT_ROTATE_ALBUM_ART = false
        const val DEFAULT_ALBUM_ART_STROKE = false
        const val DEFAULT_EXPAND_ON_PLAY = true
        const val DEFAULT_VISIBLE_IN_PLAYER_APP = true
        const val DEFAULT_SHOW_CONTROLS = true
        const val DEFAULT_SHOW_PROGRESS = false
        const val DEFAULT_EXPANDED_BACKGROUND = false
        const val DEFAULT_EXPANDED_BACKGROUND_BLUR = 24f
    }
}

class MusicTilePreferences(private val context: Context) : JsonSerializable {
    val settings: Flow<MusicTileSettings> = context.musicTileDataStore.data.map { prefs ->
        MusicTileSettings(
            showAlbumArt = prefs[SHOW_ALBUM_ART] ?: MusicTileSettings.DEFAULT_SHOW_ALBUM_ART,
            rotateAlbumArt = prefs[ROTATE_ALBUM_ART] ?: MusicTileSettings.DEFAULT_ROTATE_ALBUM_ART,
            albumArtStroke = prefs[ALBUM_ART_STROKE] ?: MusicTileSettings.DEFAULT_ALBUM_ART_STROKE,
            albumArtStrokeColor = CutoutColor.deserialize(prefs[ALBUM_ART_STROKE_COLOR]),
            expandOnPlay = prefs[EXPAND_ON_PLAY] ?: MusicTileSettings.DEFAULT_EXPAND_ON_PLAY,
            visibleInPlayerApp = prefs[VISIBLE_IN_PLAYER_APP] ?: MusicTileSettings.DEFAULT_VISIBLE_IN_PLAYER_APP,
            showControls = prefs[SHOW_CONTROLS] ?: MusicTileSettings.DEFAULT_SHOW_CONTROLS,
            skipButton = MusicButtonStyle(
                color = CutoutColor.deserialize(prefs[SKIP_COLOR]),
                opacity = (prefs[SKIP_OPACITY] ?: MusicButtonStyle.DEFAULT_OPACITY).coerceIn(0f, 1f),
                cornerPercent = (prefs[SKIP_CORNER] ?: MusicButtonStyle.DEFAULT_CORNER_PERCENT)
                    .coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT),
                filled = prefs[SKIP_FILLED] ?: MusicButtonStyle.DEFAULT_FILLED,
            ),
            playPauseButton = MusicButtonStyle(
                color = CutoutColor.deserialize(prefs[PLAY_PAUSE_COLOR]),
                opacity = (prefs[PLAY_PAUSE_OPACITY] ?: MusicButtonStyle.DEFAULT_OPACITY).coerceIn(0f, 1f),
                cornerPercent = (prefs[PLAY_PAUSE_CORNER] ?: MusicButtonStyle.DEFAULT_CORNER_PERCENT)
                    .coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT),
                filled = prefs[PLAY_PAUSE_FILLED] ?: MusicButtonStyle.DEFAULT_FILLED,
            ),
            skipIconColor = CutoutColor.deserialize(prefs[SKIP_ICON_COLOR]),
            playPauseIconColor = CutoutColor.deserialize(prefs[PLAY_PAUSE_ICON_COLOR]),
            seekIconColor = CutoutColor.deserialize(prefs[SEEK_ICON_COLOR]),
            showProgress = prefs[SHOW_PROGRESS] ?: MusicTileSettings.DEFAULT_SHOW_PROGRESS,
            progressStyle = runCatching { MusicProgressStyle.valueOf(prefs[PROGRESS_STYLE] ?: MusicProgressStyle.WAVY.name) }.getOrDefault(MusicProgressStyle.WAVY),
            showVisualizer = prefs[SHOW_VISUALIZER] ?: true,
            seekButtonMode = runCatching { SeekButtonMode.valueOf(prefs[SEEK_BUTTON_MODE] ?: SeekButtonMode.BOTH.name) }.getOrDefault(SeekButtonMode.BOTH),
            leftSeekEnabled = prefs[LEFT_SEEK_ENABLED] ?: true,
            rightSeekEnabled = prefs[RIGHT_SEEK_ENABLED] ?: true,
            leftSeekSeconds = (prefs[LEFT_SEEK_SECONDS] ?: 5).coerceIn(5, 60),
            rightSeekSeconds = (prefs[RIGHT_SEEK_SECONDS] ?: 5).coerceIn(5, 60),
            showPlayPauseIcon = prefs[SHOW_PLAY_PAUSE_ICON] ?: true,
            showPlayPauseText = prefs[SHOW_PLAY_PAUSE_TEXT] ?: true,
            visualizerStyle = runCatching { MusicVisualizerStyle.valueOf(prefs[VISUALIZER_STYLE] ?: MusicVisualizerStyle.BARS.name) }.getOrDefault(MusicVisualizerStyle.BARS),
            expandedBackground = prefs[EXPANDED_BACKGROUND] ?: MusicTileSettings.DEFAULT_EXPANDED_BACKGROUND,
            expandedBackgroundBlur = (prefs[EXPANDED_BACKGROUND_BLUR]
                ?: MusicTileSettings.DEFAULT_EXPANDED_BACKGROUND_BLUR).coerceIn(0f, 40f),
        )
    }

    override suspend fun toJson(): String {
        fun MusicButtonStyle.toJsonObject(): JSONObject = JSONObject().apply {
            put("color", color?.serialize() ?: JSONObject.NULL)
            put("opacity", opacity.toDouble())
            put("cornerPercent", cornerPercent)
            put("filled", filled)
        }
        val s = settings.first()
        return JSONObject().apply {
            put("showAlbumArt", s.showAlbumArt)
            put("rotateAlbumArt", s.rotateAlbumArt)
            put("albumArtStroke", s.albumArtStroke)
            put("albumArtStrokeColor", s.albumArtStrokeColor?.serialize() ?: JSONObject.NULL)
            put("expandOnPlay", s.expandOnPlay)
            put("visibleInPlayerApp", s.visibleInPlayerApp)
            put("showControls", s.showControls)
            put("showProgress", s.showProgress)
            put("progressStyle", s.progressStyle.name)
            put("showVisualizer", s.showVisualizer)
            put("seekButtonMode", s.seekButtonMode.name)
            put("leftSeekEnabled", s.leftSeekEnabled)
            put("rightSeekEnabled", s.rightSeekEnabled)
            put("leftSeekSeconds", s.leftSeekSeconds)
            put("rightSeekSeconds", s.rightSeekSeconds)
            put("showPlayPauseIcon", s.showPlayPauseIcon)
            put("showPlayPauseText", s.showPlayPauseText)
            put("visualizerStyle", s.visualizerStyle.name)
            put("expandedBackground", s.expandedBackground)
            put("expandedBackgroundBlur", s.expandedBackgroundBlur.toDouble())
            put("skipButton", s.skipButton.toJsonObject())
            put("playPauseButton", s.playPauseButton.toJsonObject())
            put("skipIconColor", s.skipIconColor?.serialize() ?: JSONObject.NULL)
            put("playPauseIconColor", s.playPauseIconColor?.serialize() ?: JSONObject.NULL)
            put("seekIconColor", s.seekIconColor?.serialize() ?: JSONObject.NULL)
        }.toString()
    }

    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.musicTileDataStore.edit { prefs ->
            if (obj.has("showAlbumArt")) prefs[SHOW_ALBUM_ART] = obj.getBoolean("showAlbumArt")
            if (obj.has("rotateAlbumArt")) prefs[ROTATE_ALBUM_ART] = obj.getBoolean("rotateAlbumArt")
            if (obj.has("albumArtStroke")) prefs[ALBUM_ART_STROKE] = obj.getBoolean("albumArtStroke")
            if (obj.has("albumArtStrokeColor")) {
                val raw = if (obj.isNull("albumArtStrokeColor")) null else obj.optString("albumArtStrokeColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) prefs.remove(ALBUM_ART_STROKE_COLOR) else prefs[ALBUM_ART_STROKE_COLOR] = color.serialize()
            }
            if (obj.has("expandOnPlay")) prefs[EXPAND_ON_PLAY] = obj.getBoolean("expandOnPlay")
            if (obj.has("visibleInPlayerApp")) prefs[VISIBLE_IN_PLAYER_APP] = obj.getBoolean("visibleInPlayerApp")
            if (obj.has("showControls")) prefs[SHOW_CONTROLS] = obj.getBoolean("showControls")
            if (obj.has("showProgress")) prefs[SHOW_PROGRESS] = obj.getBoolean("showProgress")
            if (obj.has("progressStyle")) prefs[PROGRESS_STYLE] = runCatching { MusicProgressStyle.valueOf(obj.getString("progressStyle")).name }.getOrDefault(MusicProgressStyle.WAVY.name)
            if (obj.has("showVisualizer")) prefs[SHOW_VISUALIZER] = obj.getBoolean("showVisualizer")
            if (obj.has("seekButtonMode")) prefs[SEEK_BUTTON_MODE] = runCatching { SeekButtonMode.valueOf(obj.getString("seekButtonMode")).name }.getOrDefault(SeekButtonMode.BOTH.name)
            if (obj.has("leftSeekEnabled")) prefs[LEFT_SEEK_ENABLED] = obj.getBoolean("leftSeekEnabled")
            if (obj.has("rightSeekEnabled")) prefs[RIGHT_SEEK_ENABLED] = obj.getBoolean("rightSeekEnabled")
            if (obj.has("leftSeekSeconds")) prefs[LEFT_SEEK_SECONDS] = obj.getInt("leftSeekSeconds").coerceIn(5, 60)
            if (obj.has("rightSeekSeconds")) prefs[RIGHT_SEEK_SECONDS] = obj.getInt("rightSeekSeconds").coerceIn(5, 60)
            if (obj.has("showPlayPauseIcon")) prefs[SHOW_PLAY_PAUSE_ICON] = obj.getBoolean("showPlayPauseIcon")
            if (obj.has("showPlayPauseText")) prefs[SHOW_PLAY_PAUSE_TEXT] = obj.getBoolean("showPlayPauseText")
            if (obj.has("visualizerStyle")) prefs[VISUALIZER_STYLE] = runCatching { MusicVisualizerStyle.valueOf(obj.getString("visualizerStyle")).name }.getOrDefault(MusicVisualizerStyle.BARS.name)
            if (obj.has("expandedBackground")) prefs[EXPANDED_BACKGROUND] = obj.getBoolean("expandedBackground")
            if (obj.has("expandedBackgroundBlur")) {
                prefs[EXPANDED_BACKGROUND_BLUR] = obj.getDouble("expandedBackgroundBlur").toFloat().coerceIn(0f, 40f)
            }
            obj.optJSONObject("skipButton")?.applyButton(prefs, SKIP_COLOR, SKIP_OPACITY, SKIP_CORNER, SKIP_FILLED)
            obj.optJSONObject("playPauseButton")?.applyButton(prefs, PLAY_PAUSE_COLOR, PLAY_PAUSE_OPACITY, PLAY_PAUSE_CORNER, PLAY_PAUSE_FILLED)
            fun applyOptionalColor(name: String, key: Preferences.Key<String>) {
                if (obj.has(name)) {
                    val raw = if (obj.isNull(name)) null else obj.optString(name)
                    val color = CutoutColor.deserialize(raw)
                    if (color == null) prefs.remove(key) else prefs[key] = color.serialize()
                }
            }
            applyOptionalColor("skipIconColor", SKIP_ICON_COLOR)
            applyOptionalColor("playPauseIconColor", PLAY_PAUSE_ICON_COLOR)
            applyOptionalColor("seekIconColor", SEEK_ICON_COLOR)
        }
    }

    private fun JSONObject.applyButton(
        prefs: MutablePreferences,
        colorKey: Preferences.Key<String>, opacityKey: Preferences.Key<Float>,
        cornerKey: Preferences.Key<Int>, filledKey: Preferences.Key<Boolean>,
    ) {
        if (has("color")) {
            val raw = if (isNull("color")) null else optString("color")
            val color = CutoutColor.deserialize(raw)
            if (color == null) prefs.remove(colorKey) else prefs[colorKey] = color.serialize()
        }
        if (has("opacity")) prefs[opacityKey] = optDouble("opacity").toFloat().coerceIn(0f, 1f)
        if (has("cornerPercent")) prefs[cornerKey] = getInt("cornerPercent").coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT)
        if (has("filled")) prefs[filledKey] = getBoolean("filled")
    }

    suspend fun setShowAlbumArt(enabled: Boolean) = context.musicTileDataStore.edit { it[SHOW_ALBUM_ART] = enabled }
    suspend fun setRotateAlbumArt(enabled: Boolean) = context.musicTileDataStore.edit { it[ROTATE_ALBUM_ART] = enabled }
    suspend fun setAlbumArtStroke(enabled: Boolean) = context.musicTileDataStore.edit { it[ALBUM_ART_STROKE] = enabled }
    suspend fun setAlbumArtStrokeColor(color: CutoutColor?) = context.musicTileDataStore.edit {
        if (color == null) it.remove(ALBUM_ART_STROKE_COLOR) else it[ALBUM_ART_STROKE_COLOR] = color.serialize()
    }
    suspend fun setExpandOnPlay(enabled: Boolean) = context.musicTileDataStore.edit { it[EXPAND_ON_PLAY] = enabled }
    suspend fun setVisibleInPlayerApp(enabled: Boolean) = context.musicTileDataStore.edit { it[VISIBLE_IN_PLAYER_APP] = enabled }
    suspend fun setShowControls(enabled: Boolean) = context.musicTileDataStore.edit { it[SHOW_CONTROLS] = enabled }
    suspend fun setSkipColor(color: CutoutColor?) = context.musicTileDataStore.edit { if (color == null) it.remove(SKIP_COLOR) else it[SKIP_COLOR] = color.serialize() }
    suspend fun setSkipIconColor(color: CutoutColor?) = context.musicTileDataStore.edit { if (color == null) it.remove(SKIP_ICON_COLOR) else it[SKIP_ICON_COLOR] = color.serialize() }
    suspend fun setPlayPauseIconColor(color: CutoutColor?) = context.musicTileDataStore.edit { if (color == null) it.remove(PLAY_PAUSE_ICON_COLOR) else it[PLAY_PAUSE_ICON_COLOR] = color.serialize() }
    suspend fun setSeekIconColor(color: CutoutColor?) = context.musicTileDataStore.edit { if (color == null) it.remove(SEEK_ICON_COLOR) else it[SEEK_ICON_COLOR] = color.serialize() }
    suspend fun setSkipOpacity(opacity: Float) = context.musicTileDataStore.edit { it[SKIP_OPACITY] = opacity.coerceIn(0f, 1f) }
    suspend fun setSkipCornerPercent(percent: Int) = context.musicTileDataStore.edit { it[SKIP_CORNER] = percent.coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT) }
    suspend fun setSkipFilled(filled: Boolean) = context.musicTileDataStore.edit { it[SKIP_FILLED] = filled }
    suspend fun setPlayPauseColor(color: CutoutColor?) = context.musicTileDataStore.edit { if (color == null) it.remove(PLAY_PAUSE_COLOR) else it[PLAY_PAUSE_COLOR] = color.serialize() }
    suspend fun setPlayPauseOpacity(opacity: Float) = context.musicTileDataStore.edit { it[PLAY_PAUSE_OPACITY] = opacity.coerceIn(0f, 1f) }
    suspend fun setShowProgress(enabled: Boolean) = context.musicTileDataStore.edit { it[SHOW_PROGRESS] = enabled }
    suspend fun setProgressStyle(style: MusicProgressStyle) = context.musicTileDataStore.edit { it[PROGRESS_STYLE] = style.name }
    suspend fun setShowVisualizer(enabled: Boolean) = context.musicTileDataStore.edit { it[SHOW_VISUALIZER] = enabled }
    suspend fun setSeekButtonMode(mode: SeekButtonMode) = context.musicTileDataStore.edit { it[SEEK_BUTTON_MODE] = mode.name }
    suspend fun setLeftSeekEnabled(enabled: Boolean) = context.musicTileDataStore.edit { it[LEFT_SEEK_ENABLED] = enabled }
    suspend fun setRightSeekEnabled(enabled: Boolean) = context.musicTileDataStore.edit { it[RIGHT_SEEK_ENABLED] = enabled }
    suspend fun setLeftSeekSeconds(seconds: Int) = context.musicTileDataStore.edit { it[LEFT_SEEK_SECONDS] = seconds.coerceIn(5, 60) }
    suspend fun setRightSeekSeconds(seconds: Int) = context.musicTileDataStore.edit { it[RIGHT_SEEK_SECONDS] = seconds.coerceIn(5, 60) }
    suspend fun setShowPlayPauseIcon(enabled: Boolean) = context.musicTileDataStore.edit { it[SHOW_PLAY_PAUSE_ICON] = enabled }
    suspend fun setShowPlayPauseText(enabled: Boolean) = context.musicTileDataStore.edit { it[SHOW_PLAY_PAUSE_TEXT] = enabled }
    suspend fun setVisualizerStyle(style: MusicVisualizerStyle) = context.musicTileDataStore.edit { it[VISUALIZER_STYLE] = style.name }
    suspend fun setExpandedBackground(enabled: Boolean) = context.musicTileDataStore.edit { it[EXPANDED_BACKGROUND] = enabled }
    suspend fun setExpandedBackgroundBlur(blurDp: Float) = context.musicTileDataStore.edit { it[EXPANDED_BACKGROUND_BLUR] = blurDp.coerceIn(0f, 40f) }
    suspend fun setPlayPauseCornerPercent(percent: Int) = context.musicTileDataStore.edit { it[PLAY_PAUSE_CORNER] = percent.coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT) }
    suspend fun setPlayPauseFilled(filled: Boolean) = context.musicTileDataStore.edit { it[PLAY_PAUSE_FILLED] = filled }
    suspend fun applySkipPreset(preset: MusicButtonStyle) = context.musicTileDataStore.edit {
        it[SKIP_OPACITY] = preset.opacity.coerceIn(0f, 1f)
        it[SKIP_CORNER] = preset.cornerPercent.coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT)
        it[SKIP_FILLED] = preset.filled
    }
    suspend fun applyPlayPausePreset(preset: MusicButtonStyle) = context.musicTileDataStore.edit {
        it[PLAY_PAUSE_OPACITY] = preset.opacity.coerceIn(0f, 1f)
        it[PLAY_PAUSE_CORNER] = preset.cornerPercent.coerceIn(MusicButtonStyle.MIN_CORNER_PERCENT, MusicButtonStyle.MAX_CORNER_PERCENT)
        it[PLAY_PAUSE_FILLED] = preset.filled
    }

    private companion object {
        val SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")
        val ROTATE_ALBUM_ART = booleanPreferencesKey("rotate_album_art")
        val ALBUM_ART_STROKE = booleanPreferencesKey("album_art_stroke")
        val ALBUM_ART_STROKE_COLOR = stringPreferencesKey("album_art_stroke_color")
        val EXPAND_ON_PLAY = booleanPreferencesKey("expand_on_play")
        val VISIBLE_IN_PLAYER_APP = booleanPreferencesKey("visible_in_player_app")
        val SHOW_CONTROLS = booleanPreferencesKey("show_controls")
        val SKIP_COLOR = stringPreferencesKey("skip_button_color")
        val SKIP_OPACITY = floatPreferencesKey("skip_button_opacity")
        val SKIP_CORNER = intPreferencesKey("skip_button_corner_percent")
        val SKIP_FILLED = booleanPreferencesKey("skip_button_filled")
        val SKIP_ICON_COLOR = stringPreferencesKey("skip_button_icon_color")
        val PLAY_PAUSE_ICON_COLOR = stringPreferencesKey("play_pause_button_icon_color")
        val SEEK_ICON_COLOR = stringPreferencesKey("seek_button_icon_color")
        val PLAY_PAUSE_COLOR = stringPreferencesKey("play_pause_button_color")
        val PLAY_PAUSE_OPACITY = floatPreferencesKey("play_pause_button_opacity")
        val PLAY_PAUSE_CORNER = intPreferencesKey("play_pause_button_corner_percent")
        val PLAY_PAUSE_FILLED = booleanPreferencesKey("play_pause_button_filled")
        val SHOW_PROGRESS = booleanPreferencesKey("show_current_progress")
        val PROGRESS_STYLE = stringPreferencesKey("music_progress_style")
        val SHOW_VISUALIZER = booleanPreferencesKey("show_music_visualizer")
        val SHOW_PLAY_PAUSE_ICON = booleanPreferencesKey("music_show_play_pause_icon")
        val SHOW_PLAY_PAUSE_TEXT = booleanPreferencesKey("music_show_play_pause_text")
        val SEEK_BUTTON_MODE = stringPreferencesKey("music_seek_button_mode")
        val LEFT_SEEK_ENABLED = booleanPreferencesKey("music_left_seek_enabled")
        val RIGHT_SEEK_ENABLED = booleanPreferencesKey("music_right_seek_enabled")
        val LEFT_SEEK_SECONDS = intPreferencesKey("music_left_seek_seconds")
        val RIGHT_SEEK_SECONDS = intPreferencesKey("music_right_seek_seconds")
        val VISUALIZER_STYLE = stringPreferencesKey("music_visualizer_style")
        val EXPANDED_BACKGROUND = booleanPreferencesKey("expanded_background")
        val EXPANDED_BACKGROUND_BLUR = floatPreferencesKey("expanded_background_blur")
    }
}
