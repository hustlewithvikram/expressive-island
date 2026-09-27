package com.vikram.expressiveisland.overlay.contents

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
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

@Composable
fun ChargingExpandedContent(
    charging: ChargingState,
    appearance: AppearanceSettings,
) {
    val contentColor = LocalContentColor.current
    val secondaryColor = contentColor.copy(alpha = 0.68f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        // Charging icon
        Box(
            modifier = Modifier
                .size(74.dp),
            contentAlignment = Alignment.Center,
        ) {
            LottieAnimation(
                composition = rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.charging)
                ).value,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(70.dp),
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Charging",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                    )

                    Text(
                        text = "${charging.batteryPercent}%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                }

                val power = charging.powerWatts

                if (power != null && power > 0f) {
                    Column(
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "${"%.1f".format(power)} W",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )

                        Text(
                            text = "Charging power",
                            fontSize = 10.sp,
                            color = secondaryColor,
                        )
                    }
                }
            }

            // Battery progress
            LinearProgressIndicator(
                progress = {
                    (charging.batteryPercent / 100f)
                        .coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
            )

            // Bottom information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                val timeToFull = charging.timeToFullMinutes

                if (timeToFull != null) {
                    Text(
                        text = formatChargingTime(timeToFull),
                        fontSize = 11.sp,
                        color = secondaryColor,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    val voltage = charging.voltageVolts

                    if (voltage != null) {
                        Text(
                            text = "${"%.2f".format(voltage)} V",
                            fontSize = 11.sp,
                            color = secondaryColor,
                        )
                    }

                    val temperature = charging.temperatureCelsius

                    if (temperature != null) {
                        Text(
                            text = "${"%.1f".format(temperature)} °C",
                            fontSize = 11.sp,
                            color = secondaryColor,
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

    // Get the current battery percentage.
    val context = LocalContext.current
    val batteryManager =
        remember(context) {
            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }
    val batteryPercent =
        remember {
            batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )
        }.coerceIn(0, 100)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryAlert,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFFF87171),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.event_battery_low),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    )

                    Text(
                        text = "Your battery is running low",
                        fontSize = 12.sp,
                        color = secondaryColor,
                    )
                }

                Text(
                    text = "$batteryPercent%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF87171),
                )
            }

            LinearProgressIndicator(
                progress = {
                    batteryPercent / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFFF87171),
                trackColor = contentColor.copy(alpha = 0.12f),
            )
        }
    }
}