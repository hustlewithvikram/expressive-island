package com.vikram.expressiveisland.ui.screen

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.SystemClock
import android.net.Uri
import android.provider.Settings
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val UPDATE_API = "https://api.github.com/repos/hustlewithvikram/expressive-island"
private const val DEBUG_RELEASE_TAG = "expressive-island-debug-latest-build"
private data class UpdateInfo(val version:String,val notes:String,val runNumber:String,val runId:String,val commit:String,val branch:String,val apkUrl:String,val channel:String)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppUpdatesScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDebug = BuildConfig.DEBUG
    val channel = if (isDebug) "Development" else "Production"
    val installedVersion = remember { runCatching { context.packageManager.getPackageInfo(context.packageName,0).versionName ?: "Unknown" }.getOrDefault("Unknown") }
    val installedCommit = BuildConfig.BUILD_COMMIT
    val installedRunId = BuildConfig.BUILD_RUN_ID
    val installedRunNumber = BuildConfig.BUILD_RUN_NUMBER
    val installedBranch = BuildConfig.BUILD_BRANCH
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var downloadSpeed by remember { mutableStateOf(0L) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var latest by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadedUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var installDialog by remember { mutableStateOf(false) }
    var discardDialog by remember { mutableStateOf(false) }
    var cancelDialog by remember { mutableStateOf(false) }

    fun checkUpdates() {
        if (checking || downloading) return
        checking = true; error = null
        scope.launch {
            try {
                latest = withContext(Dispatchers.IO) { fetchUpdate(isDebug) }
                val latestInfo = latest
                if (latestInfo?.apkUrl.isNullOrBlank()) error = "No APK is attached to the latest $channel build."
                else if (latestInfo != null && isSameBuild(latestInfo, installedRunId, installedCommit)) error = "You are already running the latest $channel build."
                else if (apk != null && latestInfo != null && downloadedUpdate != null && isSameBuild(latestInfo, downloadedUpdate!!.runId, downloadedUpdate!!.commit)) error = "The latest update is downloaded and ready to install."
                else if (apk != null) error = "A newer build is available. Download it to replace the saved APK."
            } catch (e: Exception) {
                error = when {
                    e is SecurityException || e.message?.contains("permission",true)==true ->
                        "Network access failed. Check app settings and allow network access if restricted."
                    else -> e.message ?: "Could not check for updates. Check your connection."
                }
            } finally { checking = false }
        }
    }
    fun startDownload() {
        val info = latest ?: return
        if (info.apkUrl.isBlank()) return
        try {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val prefs = context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
            val oldId = prefs.getLong("download_id", -1L)
            if (oldId >= 0L) runCatching { manager.remove(oldId) }
            val target = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expressive-island-update.apk")
            target.parentFile?.mkdirs()
            target.delete()
            val request = DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle("Expressive Island update")
                .setDescription("Downloading ${info.branch.ifBlank { channel }} build #${info.runNumber}")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .addRequestHeader("User-Agent", "Expressive-Island-Android")
                .addRequestHeader("Accept", "application/octet-stream")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "expressive-island-update.apk")
            val id = manager.enqueue(request)
            prefs.edit()
                .putLong("download_id", id)
                .putString("download_url", info.apkUrl)
                .putString("download_version", info.version)
                .putString("download_notes", info.notes)
                .putString("download_run_number", info.runNumber)
                .putString("download_run_id", info.runId)
                .putString("download_commit", info.commit)
                .putString("download_branch", info.branch)
                .putString("download_channel", info.channel)
                .putString("installed_version_at_download", installedVersion)
                .apply()
            downloadId = id
            downloading = true
            progress = 0f
            downloadSpeed = 0L
            error = null
        } catch (e: Exception) {
            downloading = false
            error = "Could not start download: ${e.message ?: "check network access"}"
        }
    }
    fun discardDownloadedApk() {
        val prefs = context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
        val id = prefs.getLong("download_id", -1L)
        if (id >= 0L) runCatching { (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(id) }
        apk?.delete()
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expressive-island-update.apk").delete()
        prefs.edit().clear().apply()
        downloadId = -1L; downloading = false; downloadSpeed = 0L
        apk = null; downloadedUpdate = null; installDialog = false; error = null
    }
    fun openSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))
    }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val savedId = prefs.getLong("download_id", -1L)
        val savedInstalledVersion = prefs.getString("installed_version_at_download", null)
        if (savedInstalledVersion != null && savedInstalledVersion != installedVersion) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expressive-island-update.apk").delete()
            prefs.edit().clear().apply()
        } else if (savedId >= 0L) {
            downloadId = savedId
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expressive-island-update.apk")
            manager.query(DownloadManager.Query().setFilterById(savedId)).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> downloading = true
                        DownloadManager.STATUS_SUCCESSFUL -> if (file.exists() && file.length() > 0L) {
                            apk = file
                            downloadedUpdate = savedUpdateInfo(prefs, channel)
                            progress = 1f
                        } else error = "Download finished but the APK file is missing. Download it again."
                        DownloadManager.STATUS_FAILED -> {
                            error = "Download failed (code ${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}). Try again on a stable connection."
                            prefs.edit().remove("download_id").apply()
                        }
                    }
                }
            }
        }
        checkUpdates()
        var lastBytes = 0L
        var lastTime = SystemClock.elapsedRealtime()
        while (true) {
            delay(500)
            val id = downloadId
            if (id < 0L) continue
            manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                if (cursor == null || !cursor.moveToFirst()) {
                    downloading = false
                } else {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0L)
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).coerceAtLeast(0L)
                    val now = SystemClock.elapsedRealtime()
                    val elapsed = now - lastTime
                    if (elapsed >= 500L) {
                        downloadSpeed = (((bytes - lastBytes).coerceAtLeast(0L) * 1000L) / elapsed)
                        lastBytes = bytes; lastTime = now
                    }
                    if (total > 0L) progress = (bytes.toFloat() / total).coerceIn(0f, 1f)
                    when (status) {
                        DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> downloading = true
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expressive-island-update.apk")
                            if (file.exists() && file.length() > 0L) {
                                apk = file
                                downloadedUpdate = savedUpdateInfo(prefs, channel)
                                progress = 1f
                                installDialog = true
                            } else error = "Download finished but the APK file is missing. Download it again."
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            error = "Download failed (code ${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}). Check your connection and retry."
                            prefs.edit().remove("download_id").apply()
                            downloadId = -1L
                        }
                    }
                }
            }
        }
    }

    val latestDiffersFromInstalled = latest?.let { !isSameBuild(it, installedRunId, installedCommit) } == true
    val latestDiffersFromDownloaded = latest?.let { info ->
        downloadedUpdate == null || !isSameBuild(info, downloadedUpdate!!.runId, downloadedUpdate!!.commit)
    } == true
    val newerDownloaded = apk != null && latestDiffersFromDownloaded && latestDiffersFromInstalled
    val statusContainer = when {
        error?.startsWith("You are already running") == true || error?.startsWith("The latest update is downloaded") == true -> MaterialTheme.colorScheme.primaryContainer
        error?.startsWith("A newer build") == true -> MaterialTheme.colorScheme.tertiaryContainer
        error != null && !error!!.startsWith("No APK is attached") -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val statusInk = when {
        error?.startsWith("You are already running") == true || error?.startsWith("The latest update is downloaded") == true -> MaterialTheme.colorScheme.onPrimaryContainer
        error?.startsWith("A newer build") == true -> MaterialTheme.colorScheme.onTertiaryContainer
        error != null && !error!!.startsWith("No APK is attached") -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("App Updates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Keep Expressive Island up to date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        AndroidView(
                            factory = { viewContext ->
                                ImageView(viewContext).apply {
                                    contentDescription = "Expressive Island icon"
                                    // Launcher icons can be AdaptiveIconDrawable on newer Android
                                    // versions. Resolve defensively so an icon/resource exception
                                    // cannot crash the entire updater screen during composition.
                                    val icon = runCatching {
                                        viewContext.packageManager.getApplicationIcon(viewContext.packageName)
                                    }.getOrNull()
                                    if (icon != null) {
                                        setImageDrawable(icon)
                                    } else {
                                        setImageResource(android.R.drawable.sym_def_app_icon)
                                    }
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Expressive Island", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(context.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Channel: $channel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    BuildSummaryCard("Installed App", installedBranch, installedRunNumber, installedCommit, Modifier.weight(1f))
                    BuildSummaryCard("Latest App", latest?.branch ?: if (checking) "Checking…" else "—", latest?.runNumber ?: "—", latest?.commit ?: "", Modifier.weight(1f))
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = statusContainer) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(when {
                            error?.startsWith("You are already running") == true || error?.startsWith("The latest update is downloaded") == true -> Icons.Rounded.CheckCircle
                            error?.startsWith("A newer build") == true -> Icons.Rounded.WarningAmber
                            error != null && !error!!.startsWith("No APK is attached") -> Icons.Rounded.ErrorOutline
                            else -> Icons.Rounded.Info
                        }, null, tint = statusInk)
                        Text(when { checking -> "Checking for $channel updates…"; downloading -> "Downloading update…"; error != null -> error!!; latest != null -> "Latest build: " + latest!!.version; else -> "Check for the latest build." },
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = statusInk)
                    }
                }
                if (downloading) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(progress * 100).toInt()}% downloaded  •  ${formatBytes(downloadSpeed)}/s", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { cancelDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Close, null); Spacer(Modifier.width(8.dp)); Text("Cancel download")
                    }
                } else if (apk != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { discardDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.DeleteOutline, null); Spacer(Modifier.width(5.dp)); Text("Discard APK")
                        }
                        Button(onClick = { installDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.SystemUpdate, null); Spacer(Modifier.width(5.dp)); Text("Install App")
                        }
                    }
                    if (newerDownloaded) {
                        Button(onClick = { startDownload() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp))
                            Text("Download Update " + latest!!.branch + "-#" + latest!!.runNumber)
                        }
                    } else {
                        FilledTonalButton(onClick = { checkUpdates() }, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text(if (checking) "Checking…" else "Check again")
                        }
                    }
                } else if (latest != null && error == null && latestDiffersFromInstalled) {
                    Button(onClick = { startDownload() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp))
                        Text("Download Update " + latest!!.branch.ifBlank { channel } + "-#" + latest!!.runNumber.ifBlank { "?" })
                    }
                    FilledTonalButton(onClick = { checkUpdates() }, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Check again")
                    }
                } else {
                    FilledTonalButton(onClick = { checkUpdates() }, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text(if (checking) "Checking…" else "Check again")
                    }
                }
            }
        }
    }
    if(cancelDialog) AlertDialog(onDismissRequest={cancelDialog=false},title={Text("Cancel download?")},text={Text("The partial APK will be deleted.")},
        confirmButton={TextButton(onClick={
            cancelDialog=false
            val prefs=context.getSharedPreferences("app_update_download",Context.MODE_PRIVATE)
            val id=prefs.getLong("download_id",-1L)
            if(id>=0L) runCatching { (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(id) }
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"expressive-island-update.apk").delete()
            prefs.edit().clear().apply()
            downloadId=-1L; downloading=false; progress=0f; downloadSpeed=0L
        }) { Text("Cancel download") }},
        dismissButton={TextButton(onClick={cancelDialog=false}) { Text("Keep downloading") }})
    if(discardDialog) AlertDialog(onDismissRequest={discardDialog=false},title={Text("Discard downloaded APK?")},text={Text("The downloaded APK will be deleted. You can download it again later.")},confirmButton={TextButton(onClick={discardDialog=false;discardDownloadedApk()}) { Text("Discard APK") }},dismissButton={TextButton(onClick={discardDialog=false}) { Text("Keep APK") }})
    if(installDialog) AlertDialog(onDismissRequest={installDialog=false},title={Text("Install update?")},text={Text("The APK is ready. Android will ask you to confirm installation.")},
        confirmButton={TextButton(onClick={
            installDialog=false
            val file=apk ?: return@TextButton
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    error = "Allow installs from this source, then return and tap Install update again."
                    context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${context.packageName}")))
                    return@TextButton
                }
                val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch(e:Exception) { error="Unable to open installer: ${e.message ?: "unknown error"}" }
        }) { Text("Install") }},dismissButton={TextButton(onClick={installDialog=false}) { Text("Later") }})
}


private fun savedUpdateInfo(prefs: android.content.SharedPreferences, channel: String) = UpdateInfo(
    prefs.getString("download_version", "Unknown") ?: "Unknown",
    prefs.getString("download_notes", "") ?: "",
    prefs.getString("download_run_number", "") ?: "",
    prefs.getString("download_run_id", "") ?: "",
    prefs.getString("download_commit", "") ?: "",
    prefs.getString("download_branch", "") ?: "",
    prefs.getString("download_url", "") ?: "",
    prefs.getString("download_channel", channel) ?: channel
)

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = -1
    do { value /= 1024.0; unit++ } while (value >= 1024.0 && unit < units.lastIndex)
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}

@Composable
private fun BuildSummaryCard(title: String, branch: String, run: String, commit: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            BuildDetail("Branch", branch.takeIf { it.isNotBlank() && it != "local" } ?: "—")
            BuildDetail("Run", run.takeIf { it.isNotBlank() && it != "local" } ?: "—")
            BuildDetail("Commit", commit.takeIf { it.isNotBlank() }?.take(8) ?: "—")
        }
    }
}
@Composable
private fun BuildDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun isSameBuild(info: UpdateInfo, installedRunId: String, installedCommit: String): Boolean {
    // A workflow rerun can create a new APK for the same commit; prefer run identity when available.
    if (info.runId.isNotBlank() && installedRunId.isNotBlank()) return info.runId == installedRunId
    return info.commit.isNotBlank() && installedCommit.isNotBlank() &&
        info.commit.equals(installedCommit, ignoreCase = true)
}

private fun fetchUpdate(debug:Boolean): UpdateInfo {
    val endpoint=if(debug) "$UPDATE_API/releases/tags/$DEBUG_RELEASE_TAG" else "$UPDATE_API/releases/latest"
    val conn=(URL(endpoint).openConnection() as HttpURLConnection).apply {
        connectTimeout=12000; readTimeout=12000
        setRequestProperty("Accept","application/vnd.github+json")
        setRequestProperty("User-Agent","Expressive-Island-Android")
    }
    val json=try {
        if(conn.responseCode !in 200..299) error("GitHub returned HTTP ${conn.responseCode}.")
        JSONObject(conn.inputStream.bufferedReader().use{it.readText()})
    } finally { conn.disconnect() }
    var apkUrl=""
    json.optJSONArray("assets")?.let { assets -> for(i in 0 until assets.length()) {
        val asset=assets.optJSONObject(i) ?: continue
        if(asset.optString("name").endsWith(".apk",true)) { apkUrl=asset.optString("browser_download_url"); break }
    } }
    val notes=json.optString("body")
    // Release notes use Markdown bold labels such as **Build:** #123.
    val run=Regex("(?i)(?:Run ID|Workflow run ID):\\*{0,2}\\s*`?([0-9]{5,})").find(notes)?.groupValues?.get(1).orEmpty()
    val runNumber=(Regex("(?i)(?:Build|Run number):\\*{0,2}\\s*#?([0-9]+)").find(notes)?.groupValues?.get(1)
        ?: Regex("(?i)Build\\s*#([0-9]+)").find(json.optString("name"))?.groupValues?.get(1)).orEmpty()
    val commit=(Regex("https://github.com/[^/]+/[^/]+/commit/([0-9a-f]{7,40})").find(notes)?.groupValues?.get(1)
        ?: Regex("(?i)Commit:?\\*{0,2}\\s*[^\\n]*?([0-9a-f]{7,40})").find(notes)?.groupValues?.get(1)).orEmpty()
    val branch=(Regex("(?i)Branch:\\*{0,2}\\s*`?([\\w./-]+)").find(notes)?.groupValues?.get(1)
        ?: Regex("\\((?:[^)]*?\\s)?([\\w./-]+)\\s*[·|]").find(json.optString("name"))?.groupValues?.get(1)).orEmpty()
    return UpdateInfo(json.optString("tag_name").removePrefix("v").ifBlank{"Unknown"},notes,runNumber,run,commit,branch,apkUrl,if(debug)"Development" else "Production")
}
