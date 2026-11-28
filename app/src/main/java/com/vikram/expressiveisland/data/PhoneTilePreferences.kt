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

private val Context.phoneTileDataStore: DataStore<Preferences> by preferencesDataStore(name = "phone_tile_prefs")

/** The phone tile's own settings, edited on its dedicated settings screen. */
enum class CallState(val key: Preferences.Key<Boolean>) {
    INCOMING(booleanPreferencesKey("incoming_enabled")),
    OUTGOING(booleanPreferencesKey("outgoing_enabled")),
    ONGOING(booleanPreferencesKey("ongoing_enabled")),
}

data class PhoneTileSettings(
    /** Show the caller's contact photo on the collapsed pill and expanded card. */
    val showPhoto: Boolean = DEFAULT_SHOW_PHOTO,
    /** Show the ticking call duration on the expanded card. */
    val showDuration: Boolean = DEFAULT_SHOW_DURATION,
    /** Show the call's action buttons (Hang up, and any others the dialer exposes). */
    val showActions: Boolean = DEFAULT_SHOW_ACTIONS,
    /** Show text labels beside the incoming call's wide Answer / Decline icons. */
    val showButtonLabels: Boolean = DEFAULT_SHOW_BUTTON_LABELS,
    /**
     * Use the taller two-row layout for an incoming (ringing) call — caller over a row of Take /
     * Hang up buttons — instead of the compact single row. Connected calls always use the single row.
     */
    val expandedIncomingLayout: Boolean = DEFAULT_EXPANDED_INCOMING,
    /** Shrink connected calls into a compact mini-call pill; tapping it still opens call controls. */
    val miniCall: Boolean = DEFAULT_MINI_CALL,
    val incomingEnabled: Boolean = DEFAULT_INCOMING_ENABLED,
    val outgoingEnabled: Boolean = DEFAULT_OUTGOING_ENABLED,
    val ongoingEnabled: Boolean = DEFAULT_ONGOING_ENABLED,
    /** Use the caller's photo as a background only while the connected-call island is expanded. */
    val expandedPhotoBackground: Boolean = DEFAULT_EXPANDED_PHOTO_BACKGROUND,
    /** Blur the expanded caller-photo background. */
    val expandedPhotoBlur: Boolean = DEFAULT_EXPANDED_PHOTO_BLUR,
    /** Blur radius in dp, clamped by the settings UI. */
    val expandedPhotoBlurRadius: Float = DEFAULT_EXPANDED_PHOTO_BLUR_RADIUS,
    /** Colour of the icon container (fallback disc shown when there's no contact photo). Null = default. */
    val iconContainerColor: CutoutColor? = null,
    /** Fill of the hang-up / end-call button. */
    val hangUpColor: CutoutColor = DEFAULT_HANG_UP_COLOR,
    /** Fill shared by every other call button (answer, mute, speaker, …). */
    val otherButtonColor: CutoutColor = DEFAULT_OTHER_BUTTON_COLOR,
    /** Incoming Answer button fill. */
    val incomingAnswerColor: CutoutColor = DEFAULT_INCOMING_ANSWER_COLOR,
    /** Incoming Decline button fill. */
    val incomingHangUpColor: CutoutColor = DEFAULT_INCOMING_HANG_UP_COLOR,
    /** Connected-call expanded Hang up button fill. */
    val expandedHangUpColor: CutoutColor = DEFAULT_EXPANDED_HANG_UP_COLOR,
) {
    val anyCallStateEnabled: Boolean
        get() = incomingEnabled || outgoingEnabled || ongoingEnabled

    companion object {
        const val DEFAULT_SHOW_PHOTO = true
        const val DEFAULT_SHOW_DURATION = true
        const val DEFAULT_SHOW_ACTIONS = true
        const val DEFAULT_SHOW_BUTTON_LABELS = true
        const val DEFAULT_EXPANDED_INCOMING = true
        const val DEFAULT_MINI_CALL = false
        const val DEFAULT_INCOMING_ENABLED = true
        const val DEFAULT_OUTGOING_ENABLED = true
        const val DEFAULT_ONGOING_ENABLED = true
        const val DEFAULT_EXPANDED_PHOTO_BACKGROUND = false
        const val DEFAULT_EXPANDED_PHOTO_BLUR = true
        const val DEFAULT_EXPANDED_PHOTO_BLUR_RADIUS = 18f

        /** Hang up is red by default (matches the preset red swatch). */
        val DEFAULT_HANG_UP_COLOR: CutoutColor = CutoutColor.Solid(0xFFEF4444)

        /** Every other button follows the Material You primary accent by default. */
        val DEFAULT_OTHER_BUTTON_COLOR: CutoutColor = CutoutColor.Dynamic(DynamicRole.PRIMARY)
        val DEFAULT_INCOMING_ANSWER_COLOR: CutoutColor = CutoutColor.Dynamic(DynamicRole.PRIMARY)
        val DEFAULT_INCOMING_HANG_UP_COLOR: CutoutColor = CutoutColor.Solid(0xFFEF4444)
        val DEFAULT_EXPANDED_HANG_UP_COLOR: CutoutColor = CutoutColor.Solid(0xFFEF4444)
    }
}

/** Persists the phone tile's display options (contact photo, duration, action buttons). */
class PhoneTilePreferences(private val context: Context) : JsonSerializable {

    val settings: Flow<PhoneTileSettings> = context.phoneTileDataStore.data.map { prefs ->
        PhoneTileSettings(
            showPhoto = prefs[SHOW_PHOTO] ?: PhoneTileSettings.DEFAULT_SHOW_PHOTO,
            showDuration = prefs[SHOW_DURATION] ?: PhoneTileSettings.DEFAULT_SHOW_DURATION,
            showActions = prefs[SHOW_ACTIONS] ?: PhoneTileSettings.DEFAULT_SHOW_ACTIONS,
            showButtonLabels = prefs[SHOW_BUTTON_LABELS] ?: PhoneTileSettings.DEFAULT_SHOW_BUTTON_LABELS,
            expandedIncomingLayout = prefs[EXPANDED_INCOMING] ?: PhoneTileSettings.DEFAULT_EXPANDED_INCOMING,
            miniCall = prefs[MINI_CALL] ?: PhoneTileSettings.DEFAULT_MINI_CALL,
            incomingEnabled = prefs[INCOMING_ENABLED] ?: PhoneTileSettings.DEFAULT_INCOMING_ENABLED,
            outgoingEnabled = prefs[OUTGOING_ENABLED] ?: PhoneTileSettings.DEFAULT_OUTGOING_ENABLED,
            ongoingEnabled = prefs[ONGOING_ENABLED] ?: PhoneTileSettings.DEFAULT_ONGOING_ENABLED,
            expandedPhotoBackground = prefs[EXPANDED_PHOTO_BACKGROUND] ?: PhoneTileSettings.DEFAULT_EXPANDED_PHOTO_BACKGROUND,
            expandedPhotoBlur = prefs[EXPANDED_PHOTO_BLUR] ?: PhoneTileSettings.DEFAULT_EXPANDED_PHOTO_BLUR,
            expandedPhotoBlurRadius = prefs[EXPANDED_PHOTO_BLUR_RADIUS]?.toFloatOrNull()
                ?.coerceIn(0f, 40f) ?: PhoneTileSettings.DEFAULT_EXPANDED_PHOTO_BLUR_RADIUS,
            iconContainerColor = CutoutColor.deserialize(prefs[ICON_CONTAINER_COLOR]),
            hangUpColor = CutoutColor.deserialize(prefs[HANG_UP_COLOR])
                ?: PhoneTileSettings.DEFAULT_HANG_UP_COLOR,
            otherButtonColor = CutoutColor.deserialize(prefs[OTHER_BUTTON_COLOR])
                ?: PhoneTileSettings.DEFAULT_OTHER_BUTTON_COLOR,
            incomingAnswerColor = CutoutColor.deserialize(prefs[INCOMING_ANSWER_COLOR])
                ?: PhoneTileSettings.DEFAULT_INCOMING_ANSWER_COLOR,
            incomingHangUpColor = CutoutColor.deserialize(prefs[INCOMING_HANG_UP_COLOR])
                ?: PhoneTileSettings.DEFAULT_INCOMING_HANG_UP_COLOR,
            expandedHangUpColor = CutoutColor.deserialize(prefs[EXPANDED_HANG_UP_COLOR])
                ?: PhoneTileSettings.DEFAULT_EXPANDED_HANG_UP_COLOR,
        )
    }

    /** Exports the current [PhoneTileSettings] as a JSON string. */
    override suspend fun toJson(): String {
        val s = settings.first()
        return JSONObject().apply {
            put("showPhoto", s.showPhoto)
            put("showDuration", s.showDuration)
            put("showActions", s.showActions)
            put("showButtonLabels", s.showButtonLabels)
            put("expandedIncomingLayout", s.expandedIncomingLayout)
            put("miniCall", s.miniCall)
            put("incomingEnabled", s.incomingEnabled)
            put("outgoingEnabled", s.outgoingEnabled)
            put("ongoingEnabled", s.ongoingEnabled)
            put("expandedPhotoBackground", s.expandedPhotoBackground)
            put("expandedPhotoBlur", s.expandedPhotoBlur)
            put("expandedPhotoBlurRadius", s.expandedPhotoBlurRadius.toDouble())
            put("iconContainerColor", s.iconContainerColor?.serialize() ?: JSONObject.NULL)
            put("hangUpColor", s.hangUpColor.serialize())
            put("otherButtonColor", s.otherButtonColor.serialize())
            put("incomingAnswerColor", s.incomingAnswerColor.serialize())
            put("incomingHangUpColor", s.incomingHangUpColor.serialize())
            put("expandedHangUpColor", s.expandedHangUpColor.serialize())
        }.toString()
    }

    /** Applies the [PhoneTileSettings] object exported by [toJson]; absent fields are left as-is. */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        context.phoneTileDataStore.edit {
            if (obj.has("showPhoto")) it[SHOW_PHOTO] = obj.getBoolean("showPhoto")
            if (obj.has("showDuration")) it[SHOW_DURATION] = obj.getBoolean("showDuration")
            if (obj.has("showActions")) it[SHOW_ACTIONS] = obj.getBoolean("showActions")
            if (obj.has("showButtonLabels")) it[SHOW_BUTTON_LABELS] = obj.getBoolean("showButtonLabels")
            if (obj.has("expandedIncomingLayout")) it[EXPANDED_INCOMING] = obj.getBoolean("expandedIncomingLayout")
            if (obj.has("miniCall")) it[MINI_CALL] = obj.getBoolean("miniCall")
            if (obj.has("incomingEnabled")) it[INCOMING_ENABLED] = obj.getBoolean("incomingEnabled")
            if (obj.has("outgoingEnabled")) it[OUTGOING_ENABLED] = obj.getBoolean("outgoingEnabled")
            if (obj.has("ongoingEnabled")) it[ONGOING_ENABLED] = obj.getBoolean("ongoingEnabled")
            if (obj.has("expandedPhotoBackground")) it[EXPANDED_PHOTO_BACKGROUND] = obj.getBoolean("expandedPhotoBackground")
            if (obj.has("expandedPhotoBlur")) it[EXPANDED_PHOTO_BLUR] = obj.getBoolean("expandedPhotoBlur")
            if (obj.has("expandedPhotoBlurRadius")) it[EXPANDED_PHOTO_BLUR_RADIUS] = obj.getDouble("expandedPhotoBlurRadius").toString()
            if (obj.has("iconContainerColor")) {
                val raw = if (obj.isNull("iconContainerColor")) null else obj.optString("iconContainerColor")
                val color = CutoutColor.deserialize(raw)
                if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
            }
            if (obj.has("hangUpColor") && !obj.isNull("hangUpColor")) {
                CutoutColor.deserialize(obj.optString("hangUpColor"))?.let { c -> it[HANG_UP_COLOR] = c.serialize() }
            }
            if (obj.has("otherButtonColor") && !obj.isNull("otherButtonColor")) {
                CutoutColor.deserialize(obj.optString("otherButtonColor"))?.let { c -> it[OTHER_BUTTON_COLOR] = c.serialize() }
            }
            if (obj.has("incomingAnswerColor") && !obj.isNull("incomingAnswerColor")) {
                CutoutColor.deserialize(obj.optString("incomingAnswerColor"))?.let { c -> it[INCOMING_ANSWER_COLOR] = c.serialize() }
            }
            if (obj.has("incomingHangUpColor") && !obj.isNull("incomingHangUpColor")) {
                CutoutColor.deserialize(obj.optString("incomingHangUpColor"))?.let { c -> it[INCOMING_HANG_UP_COLOR] = c.serialize() }
            }
            if (obj.has("expandedHangUpColor") && !obj.isNull("expandedHangUpColor")) {
                CutoutColor.deserialize(obj.optString("expandedHangUpColor"))?.let { c -> it[EXPANDED_HANG_UP_COLOR] = c.serialize() }
            }
        }
    }

    suspend fun setShowPhoto(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[SHOW_PHOTO] = enabled
    }

    suspend fun setShowDuration(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[SHOW_DURATION] = enabled
    }

    suspend fun setShowActions(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[SHOW_ACTIONS] = enabled
    }

    suspend fun setExpandedIncomingLayout(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[EXPANDED_INCOMING] = enabled
    }

    suspend fun setShowButtonLabels(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[SHOW_BUTTON_LABELS] = enabled
    }

    suspend fun setMiniCall(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[MINI_CALL] = enabled
    }

    suspend fun setCallStateEnabled(state: CallState, enabled: Boolean) = context.phoneTileDataStore.edit {
        it[state.key] = enabled
    }

    suspend fun setAllCallStatesEnabled(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[INCOMING_ENABLED] = enabled
        it[OUTGOING_ENABLED] = enabled
        it[ONGOING_ENABLED] = enabled
    }

    suspend fun setExpandedPhotoBackground(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[EXPANDED_PHOTO_BACKGROUND] = enabled
    }

    suspend fun setExpandedPhotoBlur(enabled: Boolean) = context.phoneTileDataStore.edit {
        it[EXPANDED_PHOTO_BLUR] = enabled
    }

    suspend fun setExpandedPhotoBlurRadius(radius: Float) = context.phoneTileDataStore.edit {
        it[EXPANDED_PHOTO_BLUR_RADIUS] = radius.coerceIn(0f, 40f).toString()
    }

    /** A null [color] clears the override, restoring the default accent-tinted icon container. */
    suspend fun setIconContainerColor(color: CutoutColor?) = context.phoneTileDataStore.edit {
        if (color == null) it.remove(ICON_CONTAINER_COLOR) else it[ICON_CONTAINER_COLOR] = color.serialize()
    }

    suspend fun setHangUpColor(color: CutoutColor) = context.phoneTileDataStore.edit {
        it[HANG_UP_COLOR] = color.serialize()
    }

    suspend fun setOtherButtonColor(color: CutoutColor) = context.phoneTileDataStore.edit {
        it[OTHER_BUTTON_COLOR] = color.serialize()
    }

    suspend fun setIncomingAnswerColor(color: CutoutColor) = context.phoneTileDataStore.edit {
        it[INCOMING_ANSWER_COLOR] = color.serialize()
    }

    suspend fun setIncomingHangUpColor(color: CutoutColor) = context.phoneTileDataStore.edit {
        it[INCOMING_HANG_UP_COLOR] = color.serialize()
    }

    suspend fun setExpandedHangUpColor(color: CutoutColor) = context.phoneTileDataStore.edit {
        it[EXPANDED_HANG_UP_COLOR] = color.serialize()
    }

    private companion object {
        val SHOW_PHOTO = booleanPreferencesKey("show_photo")
        val SHOW_DURATION = booleanPreferencesKey("show_duration")
        val SHOW_ACTIONS = booleanPreferencesKey("show_actions")
        val SHOW_BUTTON_LABELS = booleanPreferencesKey("show_button_labels")
        val EXPANDED_INCOMING = booleanPreferencesKey("expanded_incoming_layout")
        val MINI_CALL = booleanPreferencesKey("mini_call")
        val INCOMING_ENABLED = booleanPreferencesKey("incoming_enabled")
        val OUTGOING_ENABLED = booleanPreferencesKey("outgoing_enabled")
        val ONGOING_ENABLED = booleanPreferencesKey("ongoing_enabled")
        val EXPANDED_PHOTO_BACKGROUND = booleanPreferencesKey("expanded_photo_background")
        val EXPANDED_PHOTO_BLUR = booleanPreferencesKey("expanded_photo_blur")
        val EXPANDED_PHOTO_BLUR_RADIUS = stringPreferencesKey("expanded_photo_blur_radius")
        val ICON_CONTAINER_COLOR = stringPreferencesKey("icon_container_color")
        val HANG_UP_COLOR = stringPreferencesKey("hang_up_color")
        val OTHER_BUTTON_COLOR = stringPreferencesKey("other_button_color")
        val INCOMING_ANSWER_COLOR = stringPreferencesKey("incoming_answer_color")
        val INCOMING_HANG_UP_COLOR = stringPreferencesKey("incoming_hang_up_color")
        val EXPANDED_HANG_UP_COLOR = stringPreferencesKey("expanded_hang_up_color")
    }
}
