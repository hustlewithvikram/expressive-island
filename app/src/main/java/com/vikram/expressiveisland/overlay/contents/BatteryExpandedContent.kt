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
) {
    val contentColor = LocalContentColor.current
    val secondaryColor = contentColor.copy(alpha = 0.68f)
    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.charging)).value

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        val viewportHeight = (maxHeight - BatteryNotchSafeSpace).coerceAtLeast(0.dp)
        val compact = viewportHeight < 92.dp
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(BatteryNotchSafeSpace))
            Column(
                modifier = Modifier.fillMaxWidth().height(viewportHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (compact) 20.dp else 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = if (compact) 12.dp else 15.dp,
                            vertical = if (compact) 9.dp else 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(if (compact) 34.dp else 46.dp), contentAlignment = Alignment.Center) {
                                LottieAnimation(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(if (compact) 34.dp else 46.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f).padding(start = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text("Charging", color = secondaryColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    charging.batteryPercent.toString() + "%",
                                    color = contentColor,
                                    fontSize = if (compact) 21.sp else 26.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            val power = charging.powerWatts
                            if (power != null && power > 0f) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("%.1f W".format(power), color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Charging power", color = secondaryColor, fontSize = 9.sp)
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (charging.batteryPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val timeToFull = charging.timeToFullMinutes
                            if (timeToFull != null) {
                                Text(formatChargingTime(timeToFull), color = secondaryColor, fontSize = 10.sp)
                            } else {
                                Text("Battery charging", color = secondaryColor, fontSize = 10.sp)
                            }
                            if (!compact) {
                                val temperature = charging.temperatureCelsius
                                val voltage = charging.voltageVolts
                                val telemetry = listOfNotNull(
                                    voltage?.let { "%.2f V".format(it) },
                                    temperature?.let { "%.1f °C".format(it) },
                                ).joinToString("  ·  ")
                                if (telemetry.isNotEmpty()) {
                                    Text(telemetry, color = secondaryColor, fontSize = 9.sp)
                                }
                            }
                        }
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
