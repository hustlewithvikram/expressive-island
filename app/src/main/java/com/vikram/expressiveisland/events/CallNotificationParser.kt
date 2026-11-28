package com.vikram.expressiveisland.events

import android.app.Notification
import android.app.Person
import android.content.Context
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.CutoutSignal
import com.vikram.expressiveisland.overlay.toImageBitmap

/**
 * Everything the phone tile needs, pulled out of a dialer's ongoing-call notification. Keeps the
 * Android extras plumbing (CallStyle, [Person], chronometer) out of the listener service.
 */
data class ParsedCall(
    val callerLabel: String,
    /**
     * The caller's dialable number when the dialer exposes one (a CallStyle [Person] `tel:` URI),
     * else null. Shown as the incoming-call tile's secondary line above the name; null (or a value
     * equal to [callerLabel], i.e. an unknown caller shown by number) hides that line.
     */
    val callerNumber: String?,
    val photo: ImageBitmap?,
    val startTimeMs: Long?,
    val ongoing: Boolean,
    val actions: List<CutoutSignal.Notification.Action>,
)

/**
 * Recognises and reads call notifications. A call is any notification the dialer marks as
 * [Notification.CATEGORY_CALL] or renders with the [Notification.CallStyle] template. This is the
 * one place that understands the (partly version-gated) call extras, so callers stay simple.
 */
object CallNotificationParser {

    // Values of Notification.EXTRA_CALL_TYPE (API 31+); duplicated as ints so we can read them
    // without gating the whole call on the SDK level.
    private const val CALL_TYPE_INCOMING = 1
    private const val CALL_TYPE_ONGOING = 2

    // Both legacy call notifications and the modern CallStyle template carry this category
    // (CallStyle sets it automatically), so it alone reliably identifies a call notification.
    fun isCall(sbn: StatusBarNotification): Boolean =
        sbn.notification?.category == Notification.CATEGORY_CALL

    fun parse(sbn: StatusBarNotification, context: Context): ParsedCall {
        val notification = sbn.notification
        val extras = notification.extras

        val person = person(extras)
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val callerLabel = person?.name?.toString()?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.phone_caller_unknown)

        // Prefer the dialer's own CallStyle portrait, then its large icon, and finally the
        // matching Contacts photo when the user has granted contact access.
        val photo = person?.icon?.toImageBitmapOrNull(context)
            ?: notification.getLargeIcon()?.toImageBitmapOrNull(context)
            ?: lookupContactPhoto(context, person)

        // A ticking chronometer anchored to the notification's `when` is the dialer telling us the
        // call is connected and how long it has run; a count-down timer isn't call duration.
        val showsChronometer = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true
        val countsDown = extras?.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN) == true
        val startTimeMs = notification.`when`.takeIf { showsChronometer && !countsDown && it > 0L }

        val ongoing = when (callType(extras)) {
            CALL_TYPE_INCOMING -> false
            CALL_TYPE_ONGOING -> true
            // No explicit type (older dialers): a running timer means it's connected.
            else -> startTimeMs != null
        }

        val actions = notification.actions.orEmpty().mapNotNull { action ->
            val label = action.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val intent = action.actionIntent ?: return@mapNotNull null
            CutoutSignal.Notification.Action(label, intent)
        }

        return ParsedCall(callerLabel, phoneNumber(person), photo, startTimeMs, ongoing, actions)
    }


    /**
     * Some dialers omit Person.icon and the notification large icon even when the number belongs
     * to a saved contact. Resolve that number through ContactsContract as a fallback; this is only
     * attempted after the user grants READ_CONTACTS.
     */
    private fun lookupContactPhoto(context: Context, person: Person?): ImageBitmap? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
        val number = phoneNumber(person)?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number),
            )
            val contactId = context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            } ?: return null
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                contactUri,
                true,
            )?.use { stream -> BitmapFactory.decodeStream(stream) }?.asImageBitmap()
        }.getOrNull()
    }

    /** The caller's dialable number, pulled from the CallStyle [Person]'s `tel:` URI, or null. */
    private fun phoneNumber(person: Person?): String? {
        val uri = person?.uri ?: return null
        val number = uri.removePrefix("tel:")
        return number.takeIf { it != uri && it.isNotBlank() }
    }

    /** The [Person] a CallStyle notification names as the caller (API 31+), or null. */
    private fun person(extras: Bundle?): Person? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || extras == null) return null
        @Suppress("DEPRECATION")
        return extras.getParcelable(Notification.EXTRA_CALL_PERSON) as? Person
    }

    /** The CallStyle call type (API 31+), or 0 when unavailable. */
    private fun callType(extras: Bundle?): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || extras == null) return 0
        return extras.getInt(Notification.EXTRA_CALL_TYPE, 0)
    }

    private fun Icon.toImageBitmapOrNull(context: Context): ImageBitmap? =
        runCatching { loadDrawable(context)?.toImageBitmap() }.getOrNull()
}
