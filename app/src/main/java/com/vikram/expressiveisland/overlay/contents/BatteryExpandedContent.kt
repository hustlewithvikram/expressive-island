package com.vikram.expressiveisland.overlay.contents

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.ChargingState
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.overlay.formatChargingTime

private val BatteryNotchSafeSpace = 36.dp

@Composable
fun ChargingExpandedContent(
    charging: ChargingState,
    appearance: AppearanceSettings,
    notchBottomInsetDp: Int,
) {
    val percent = charging.batteryPercent.coerceIn(0, 100)
    val progress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "chargingBatteryProgress",
    )
    // Keep the duration only: the expanded island does not need the redundant "to full / left" copy.
    val eta = charging.timeToFullMinutes
        ?.let(::formatChargingTime)
        ?.replace(Regex("\\s*to full", RegexOption.IGNORE_CASE), "")
        ?: "Calculating"
    val inset = notchBottomInsetDp.coerceAtLeast(0).dp

    // The camera/notch band is excluded first. All three content rows then share only the
    // remaining configured expanded height, so the progress bar can never be pushed outside it.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Reserve a real transparent band below the progress track. This keeps the bar
        // visually detached from the island's rounded lower edge at every configured height.
        val bottomBreathingRoom = if (maxHeight >= 100.dp) 10.dp else 4.dp
        val contentHeight = (maxHeight - inset - bottomBreathingRoom).coerceAtLeast(0.dp)
        val ultraCompact = contentHeight < 48.dp
        val compact = contentHeight < 68.dp

        val headerHeight = when {
            ultraCompact -> 14.dp
            compact -> 17.dp
            else -> 30.dp
        }
        val metricHeight = when {
            ultraCompact -> 22.dp
            compact -> 25.dp
            else -> 38.dp
        }
        val barHeight = when {
            ultraCompact -> 6.dp
            compact -> 8.dp
            else -> 30.dp
        }
        val iconSize = when {
            ultraCompact -> 14.dp
            compact -> 17.dp
            else -> 28.dp
        }
        val percentSize = when {
            ultraCompact -> 20.sp
            compact -> 23.sp
            else -> 34.sp
        }
        val etaSize = when {
            ultraCompact -> 16.sp
            compact -> 19.sp
            else -> 29.sp
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(inset))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
                    .padding(horizontal = if (compact) 12.dp else 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(modifier = Modifier.size(iconSize)) {
                        val w = size.width
                        val h = size.height
                        val bolt = Path().apply {
                            moveTo(w * 0.58f, h * 0.02f)
                            lineTo(w * 0.18f, h * 0.56f)
                            lineTo(w * 0.44f, h * 0.56f)
                            lineTo(w * 0.34f, h * 0.98f)
                            lineTo(w * 0.84f, h * 0.38f)
                            lineTo(w * 0.57f, h * 0.38f)
                            close()
                        }
                        drawPath(path = bolt, color = Color.White)
                    }
                    Spacer(Modifier.size(if (compact) 7.dp else 9.dp))
                    Text(
                        text = "Charging...",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = if (compact) 10.sp else 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(metricHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$percent%",
                        color = Color.White,
                        fontSize = percentSize,
                        lineHeight = percentSize,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                    )
                    Text(
                        text = eta,
                        color = Color.White,
                        fontSize = etaSize,
                        lineHeight = etaSize,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                    )
                }

                // The progress track is always laid out as the final row inside the fixed viewport.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF43D9E8)),
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryLowExpandedContent(
    appearance: AppearanceSettings,
) {
    val contentColor = LocalContentColor.current
    val secondaryColor = contentColor.copy(alpha = 0.68f)
    val context = LocalContext.current
    val batteryManager = remember(context) {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    val batteryPercent = remember {
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }.coerceIn(0, 100)
    val warningColor = Color(0xFFF87171)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 6.dp),
    ) {
        val viewportHeight = (maxHeight - BatteryNotchSafeSpace).coerceAtLeast(0.dp)
        val compact = viewportHeight < 76.dp
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(BatteryNotchSafeSpace))
            Column(
                modifier = Modifier.fillMaxWidth().height(viewportHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight)) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (compact) 9.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BatteryAlert, contentDescription = null, modifier = Modifier.size(30.dp), tint = warningColor)
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(stringResource(R.string.event_battery_low), color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (!compact) Text("Consider connecting a charger", color = secondaryColor, fontSize = 10.sp)
                            }
                            Text(batteryPercent.toString() + "%", color = warningColor, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (batteryPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                            color = warningColor,
                            trackColor = contentColor.copy(alpha = 0.12f),
                        )
                    }
                }
                }
            }
        }
    }
}
