package com.vikram.expressiveisland.ui.screen

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val EXPRESSIVE_RELEASES_API =
    "https://api.github.com/repos/hustlewithvikram/expressive-island/releases/latest"

@Composable
fun AppUpdatesScreen(contentPadding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installedVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        }.getOrDefault("Unknown")
    }
    var checking by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var releaseUrl by remember { mutableStateOf<String?>(null) }
    var releaseNotes by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var checked by remember { mutableStateOf(false) }

    fun checkForUpdates() {
        if (checking) return
        checking = true
        error = null
        scope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    val connection = (URL(EXPRESSIVE_RELEASES_API).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10000
                        readTimeout = 10000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github+json")
                        setRequestProperty("User-Agent", "Expressive-Island-Android")
                    }
                    try {
                        if (connection.responseCode !in 200..299) {
                            error("GitHub returned HTTP ${connection.responseCode}")
                        }
                        JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    } finally {
                        connection.disconnect()
                    }
                }
                latestVersion = release.optString("tag_name").removePrefix("v").ifBlank { null }
                releaseUrl = release.optString("html_url").ifBlank { null }
                releaseNotes = release.optString("body").takeIf { it.isNotBlank() }
                if (latestVersion == null) error = "The latest release did not include a version tag."
                checked = true
            } catch (e: Exception) {
                error = e.message ?: "Unable to check for updates. Check your internet connection."
                checked = true
            } finally {
                checking = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdates()
    }

    val isUpdateAvailable = latestVersion?.let { compareVersions(it, installedVersion) > 0 } == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp)) {
            Text("App Updates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Check for the latest Expressive Island release.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(
                            Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Expressive Island", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Installed version: $installedVersion", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        checking -> "Checking GitHub for the latest release…"
                        error != null -> error!!
                        isUpdateAvailable -> "Version ${latestVersion} is available."
                        latestVersion != null -> "You're up to date."
                        else -> "Check for updates to see the latest version."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (checking) {
                    CircularProgressIndicator()
                } else if (checked && error == null && !isUpdateAvailable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(" Latest: ${latestVersion ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = { checkForUpdates() },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text(if (checking) "Checking…" else "Check for updates")
                }

                if (isUpdateAvailable && releaseUrl != null) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Text("View and download update")
                    }
                }
            }
        }

        if (!releaseNotes.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Release notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(releaseNotes!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (error != null) {
            OutlinedButton(
                onClick = { checkForUpdates() },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Text("Try again")
            }
        }
    }
}

private fun compareVersions(latest: String, installed: String): Int {
    val latestParts = latest.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
    val installedParts = installed.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(latestParts.size, installedParts.size)) {
        val a = latestParts.getOrElse(i) { 0 }
        val b = installedParts.getOrElse(i) { 0 }
        if (a != b) return a.compareTo(b)
    }
    return 0
}
