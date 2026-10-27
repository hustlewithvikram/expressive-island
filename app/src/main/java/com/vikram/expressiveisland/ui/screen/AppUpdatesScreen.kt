package com.vikram.expressiveisland.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val UPDATE_API = "https://api.github.com/repos/hustlewithvikram/expressive-island"
private const val DEBUG_RELEASE_TAG = "expressive-island-debug-latest-build"
private data class UpdateInfo(val version:String,val notes:String,val runNumber:String,val runId:String,val commit:String,val branch:String,val apkUrl:String,val channel:String)

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
    var latest by remember { mutableStateOf<UpdateInfo?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var installDialog by remember { mutableStateOf(false) }
    var cancelDialog by remember { mutableStateOf(false) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun checkUpdates() {
        if (checking || downloading || apk != null) return
        checking = true; error = null; latest = null
        scope.launch {
            try {
                latest = withContext(Dispatchers.IO) { fetchUpdate(isDebug) }
                if (latest?.apkUrl.isNullOrBlank()) error = "No APK is attached to the latest $channel build."
                else if (latest?.commit?.isNotBlank() == true && installedCommit.isNotBlank() && latest?.commit == installedCommit) error = "You are already running the latest $channel build."
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
        downloading = true; progress = 0f; error = null
        downloadJob = scope.launch {
            val target = File(context.cacheDir,"updates/expressive-island-update.apk")
            try {
                withContext(Dispatchers.IO) {
                    target.parentFile?.mkdirs(); target.delete()
                    val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout=15000; readTimeout=30000
                        setRequestProperty("User-Agent","Expressive-Island-Android")
                    }
                    try {
                        if (conn.responseCode !in 200..299) error("Download failed (HTTP ${conn.responseCode}).")
                        val total = conn.contentLengthLong
                        conn.inputStream.use { input ->
                            target.outputStream().buffered().use { output ->
                                val buffer=ByteArray(16384)
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val n=input.read(buffer); if(n<0) break
                                    if(n==0) continue
                                    output.write(buffer,0,n)
                                    if(total>0) progress=(target.length().toFloat()/total).coerceIn(0f,1f)
                                }
                            }
                        }
                    } finally { conn.disconnect() }
                }
                if(!target.exists() || target.length()==0L) error("Downloaded APK is empty.")
                apk=target; installDialog=true
            } catch(e: CancellationException) { target.delete(); throw e
            } catch(e: Exception) { target.delete(); error=e.message ?: "Download failed."
            } finally { downloading=false; downloadJob=null }
        }
    }
    fun discardDownloadedApk() { apk?.delete(); apk = null; installDialog = false; error = null }
    fun openSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))
    }
    LaunchedEffect(Unit) { checkUpdates() }

    Column(Modifier.fillMaxWidth().padding(contentPadding),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Column(Modifier.padding(start=8.dp,top=10.dp,bottom=4.dp)) {
            Text("App Updates",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
            Text("Updates for the ${channel.lowercase()} channel.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Rounded.SystemUpdate,null,tint=MaterialTheme.colorScheme.onSecondaryContainer,modifier=Modifier.padding(12.dp))
                    }
                    Column(Modifier.weight(1f).padding(start=12.dp)) {
                        Text("Expressive Island",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                        Text("Installed version: $installedVersion",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Channel: $channel",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                        Text("Package: ${context.packageName}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(when { checking -> "Checking for $channel updates…"; downloading -> "Downloading update…"; error!=null -> error!!; latest!=null -> "Latest build: ${latest!!.version}"; else -> "Check for the latest build." },
                    color=if(error!=null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                if(checking) LoadingIndicator(Modifier.size(32.dp))
                if(downloading) {
                    LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth())
                    Text("${(progress*100).toInt()}% downloaded",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick={cancelDialog=true},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Close,null); Spacer(Modifier.width(8.dp)); Text("Cancel download") }
                } else if(apk!=null) {
                    Button(onClick={installDialog=true},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Rounded.SystemUpdate,null); Spacer(Modifier.width(8.dp)); Text("Install update") }
                    OutlinedButton(onClick={discardDialog=true},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Rounded.DeleteOutline,null); Spacer(Modifier.width(8.dp)); Text("Discard downloaded APK") }
                } else if(latest!=null && error==null) {
                    Button(onClick={startDownload()},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Download,null); Spacer(Modifier.width(8.dp)); Text("Download update") }
                }
                if (apk == null && !downloading && (error != null || latest == null || (latest != null && latest?.commit != installedCommit))) {
                    FilledTonalButton(onClick={checkUpdates()},enabled=!checking,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh,null); Spacer(Modifier.width(8.dp)); Text(if(checking) "Checking…" else "Check for updates") }
                }
                if(error!=null) OutlinedButton(onClick={openSettings()},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Settings,null); Spacer(Modifier.width(8.dp)); Text("Open app settings") }
            }
        }
        latest?.let { info ->
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Build information",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                    Text("Channel: ${info.channel}")
                    Text("Installed branch: ${installedBranch.ifBlank { "Not available" }}")
                    Text("Installed workflow run: ${installedRunNumber.ifBlank { "Not embedded" }}")
                    Text("Installed run ID: ${installedRunId.ifBlank { "Not embedded" }}")
                    Text("Installed commit: ${installedCommit.takeIf { it.isNotBlank() }?.take(12) ?: "Not embedded"}")
                    Text("Latest workflow run: ${info.runNumber.ifBlank { "Not available" }}")
                    Text("Branch: ${info.branch.ifBlank{"Not available"}}")
                    Text("Workflow run ID: ${info.runId.ifBlank{"Not available"}}")
                    Text("Commit: ${info.commit.ifBlank{"Not available"}}")
                    if(info.runId.isNotBlank()) TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/hustlewithvikram/expressive-island/actions/runs/${info.runId}")))}) { Text("Open workflow run") }
                    if(info.notes.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("Build notes",style=MaterialTheme.typography.titleSmall); Text(info.notes,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    if(cancelDialog) AlertDialog(onDismissRequest={cancelDialog=false},title={Text("Cancel download?")},text={Text("The partial APK will be deleted.")},
        confirmButton={TextButton(onClick={cancelDialog=false;downloadJob?.cancel()}) { Text("Cancel download") }},
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
    val run=Regex("(?i)(?:Run ID|Workflow run ID):?\\s*([0-9]{5,})").find(notes)?.groupValues?.get(1).orEmpty()
    val runNumber=Regex("(?i)(?:Build|Run number):?\\s*#?([0-9]+)").find(notes)?.groupValues?.get(1).orEmpty()
    val commit=(Regex("https://github.com/[^/]+/[^/]+/commit/([0-9a-f]{7,40})").find(notes)?.groupValues?.get(1)
        ?: Regex("(?i)Commit:?\\s*[^\\n]*?([0-9a-f]{7,40})").find(notes)?.groupValues?.get(1)).orEmpty()
    val branch=Regex("(?i)Branch:?\\s*[`]?([\\w./-]+)").find(notes)?.groupValues?.get(1).orEmpty()
    return UpdateInfo(json.optString("tag_name").removePrefix("v").ifBlank{"Unknown"},notes,runNumber,run,commit,branch,apkUrl,if(debug)"Development" else "Production")
}
