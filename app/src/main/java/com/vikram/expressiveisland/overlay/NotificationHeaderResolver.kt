package com.vikram.expressiveisland.overlay

import android.content.Context

object NotificationHeaderResolver {

    fun resolveAppName(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null

        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(
                pm.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrNull()
    }

    fun resolvePostTimeMs(
        postTime: Long,
        fallbackMs: Long = System.currentTimeMillis(),
    ): Long {
        return if (postTime > 0L) postTime else fallbackMs
    }

    fun formatRelativeTime(
        postTimeMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): String {
        val elapsedSeconds =
            ((nowMs - postTimeMs) / 1000L).coerceAtLeast(0L)

        return when {
            elapsedSeconds < 5L -> "Now"
            elapsedSeconds < 60L -> "${elapsedSeconds}s ago"
            elapsedSeconds < 3600L -> "${elapsedSeconds / 60L}m ago"
            elapsedSeconds < 86400L -> "${elapsedSeconds / 3600L}h ago"
            else -> "${elapsedSeconds / 86400L}d ago"
        }
    }

    fun formatHeader(
        appName: String?,
        relativeTime: String?,
        showAppName: Boolean,
        showTimestamp: Boolean,
    ): String? {
        val showApp = showAppName && !appName.isNullOrBlank()
        val showTime = showTimestamp && !relativeTime.isNullOrBlank()

        return when {
            showApp && showTime -> "$appName • $relativeTime"
            showApp -> appName
            showTime -> relativeTime
            else -> null
        }
    }

    fun resolveHeader(
        appName: String?,
        postTimeMs: Long?,
        showAppName: Boolean,
        showTimestamp: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ): String? {
        val relativeTime = postTimeMs?.let {
            formatRelativeTime(it, nowMs)
        }

        return formatHeader(
            appName = appName,
            relativeTime = relativeTime,
            showAppName = showAppName,
            showTimestamp = showTimestamp,
        )
    }
}