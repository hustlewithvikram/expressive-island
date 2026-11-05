package com.vikram.expressiveisland.overlay.contents

import android.content.Context
import android.net.TrafficStats
import android.os.BatteryManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.vikram.expressiveisland.core.WifiBus
import com.vikram.expressiveisland.data.CutoutColor
import com.vikram.expressiveisland.data.IconSource
import com.vikram.expressiveisland.data.PillWidgetType
import com.vikram.expressiveisland.overlay.MaterialIconCatalog
import com.vikram.expressiveisland.overlay.loadImageBitmapOrNull
import com.vikram.expressiveisland.overlay.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun EmptyPillContent(
    icon: IconSource?,
    containerColor: CutoutColor?,
    leftWidget: PillWidgetType,
    rightWidget: PillWidgetType,
    heightDp: Int,
    isStickToCamera: Boolean = false,
) {
    val context = LocalContext.current
    val disc = containerColor?.resolve()
    val glyphColor = if (disc != null && disc.luminance() > 0.5f) PillTextColorDark
        else if (disc != null) PillTextColor else LocalContentColor.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = icon) {
        value = when (icon) {
            is IconSource.Image -> withContext(Dispatchers.IO) { icon.uri.toUri().loadImageBitmapOrNull(context) }
            else -> null
        }
    }
    val materialIcon = (icon as? IconSource.Material)?.let { MaterialIconCatalog.iconFor(it.iconName) }
    val wifi by WifiBus.state.collectAsState()
    val speed by produceState("0 KB/s", context) {
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        while (true) {
            delay(1000)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val bytesPerSecond = (max(0L, rx - previousRx) + max(0L, tx - previousTx)).toDouble()
            previousRx = rx
            previousTx = tx
            value = formatPillSpeed(bytesPerSecond)
        }
    }
    val battery by produceState(100, context) {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (level in 0..100) value = level
            delay(30_000)
        }
    }
    val clock by produceState("", context) {
        while (true) {
            value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(15_000)
        }
    }
    val pulse = rememberInfiniteTransition(label = "persistent-pill-pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "persistent-pill-icon-pulse",
    )

    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (heightDp * 0.16f).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PillWidget(leftWidget, bitmap, materialIcon, disc, glyphColor, heightDp, pulseScale, speed, battery, clock, wifi?.signalLevel)
            PillWidget(rightWidget, bitmap, materialIcon, disc, glyphColor, heightDp, pulseScale, speed, battery, clock, wifi?.signalLevel)
        }
    }
}

@Composable
private fun PillWidget(
    type: PillWidgetType,
    bitmap: ImageBitmap?,
    materialIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    disc: androidx.compose.ui.graphics.Color?,
    glyphColor: androidx.compose.ui.graphics.Color,
    heightDp: Int,
    pulseScale: Float,
    speed: String,
    battery: Int,
    clock: String,
    wifiLevel: Int?,
) {
    when (type) {
        PillWidgetType.NONE -> Unit
        PillWidgetType.ICON, PillWidgetType.ANIMATED_ICON -> {
            val size = (heightDp * 0.48f).dp
            Box(
                Modifier.size((heightDp * 0.72f).dp).clip(CircleShape)
                    .then(if (disc != null) Modifier.background(disc) else Modifier)
                    .graphicsLayer {
                        if (type == PillWidgetType.ANIMATED_ICON) {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    bitmap != null -> Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
                    materialIcon != null -> Icon(
                        imageVector = materialIcon, contentDescription = null, tint = glyphColor,
                        modifier = Modifier.size(size),
                    )
                    else -> Text("★", color = glyphColor, fontSize = 16.sp)
                }
            }
        }
        PillWidgetType.NETWORK_SPEED -> Text(speed, color = LocalContentColor.current, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        PillWidgetType.BATTERY -> Text("$battery%", color = LocalContentColor.current, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        PillWidgetType.CLOCK -> Text(clock, color = LocalContentColor.current, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        PillWidgetType.WIFI_SIGNAL -> Text(
            wifiLevel?.let { "Wi-Fi ${((it + 1).coerceIn(1, 5) * 20)}%" } ?: "Wi-Fi",
            color = LocalContentColor.current, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

private fun formatPillSpeed(bytesPerSecond: Double): String {
    val kb = bytesPerSecond / 1024.0
    return if (kb >= 1024) String.format(Locale.getDefault(), "%.1f MB/s", kb / 1024)
    else String.format(Locale.getDefault(), "%.0f KB/s", kb)
}
