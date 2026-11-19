package com.vikram.expressiveisland.overlay.contents

import android.content.Context
import android.os.BatteryManager
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
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
    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.charging)).value
    val percent = charging.batteryPercent.coerceIn(0, 100)
    val progress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "chargingBatteryProgress",
    )
    val eta = charging.timeToFullMinutes?.let(::formatChargingTime) ?: "Calculating"
    val inset = notchBottomInsetDp.coerceAtLeast(0).dp

    // The configured expanded shell is the viewport. Reserve the camera cutout area,
    // then fit the complete charging composition in the remaining height without a card.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentHeight = (maxHeight - inset).coerceAtLeast(0.dp)
        val compact = contentHeight < 76.dp
        val veryCompact = contentHeight < 52.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(inset))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 9.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(if (compact) 27.dp else 34.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (composition != null) {
                                LottieAnimation(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(if (compact) 27.dp else 34.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Listening",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (compact) 23.dp else 29.dp),
                                )
                            }
                        }
                        Spacer(Modifier.size(9.dp))
                        Text(
                            text = "Charging...",
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = if (compact) 10.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        if (!veryCompact) {
                            Text(
                                text = "$percent%",
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = if (compact) 15.sp else 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (!veryCompact) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = "$percent%",
                                color = Color.White,
                                fontSize = if (compact) 27.sp else 34.sp,
                                lineHeight = if (compact) 29.sp else 37.sp,
                                fontWeight = FontWeight.Light,
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = eta,
                                    color = Color.White,
                                    fontSize = if (compact) 22.sp else 30.sp,
                                    lineHeight = if (compact) 25.sp else 33.sp,
                                    fontWeight = FontWeight.Light,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = "left",
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = if (compact) 10.sp else 12.sp,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                        }
                    }

                    // Wide, edge-to-edge battery progress bar; intentionally no enclosing card.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 15.dp else 32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.13f)),
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
