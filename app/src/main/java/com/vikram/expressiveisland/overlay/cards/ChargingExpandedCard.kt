package com.vikram.expressiveisland.overlay.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.ChargingState
import com.vikram.expressiveisland.data.AppearanceSettings

@Composable
fun ChargingExpandedCard(
    charging: ChargingState,
    appearance: AppearanceSettings,
) {
    val contentColor = LocalContentColor.current
    val secondaryColor = contentColor.copy(alpha = 0.68f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.charging)
                )

                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(60.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {

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

                    charging.powerWatts
                        ?.takeIf { it > 0f }
                        ?.let { power ->
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    charging.timeToFullMinutes
                        ?.takeIf { it > 0L }
                        ?.let { minutes ->
                            Text(
                                text = formatChargingTime(minutes),
                                fontSize = 11.sp,
                                color = secondaryColor,
                            )
                        }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        charging.voltageVolts?.let {
                            Text(
                                text = "${"%.2f".format(it)} V",
                                fontSize = 11.sp,
                                color = secondaryColor,
                            )
                        }

                        charging.temperatureCelsius?.let {
                            Text(
                                text = "${"%.1f".format(it)} °C",
                                fontSize = 11.sp,
                                color = secondaryColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatChargingTime(minutes: Long): String {
    if (minutes <= 0L) return ""

    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return if (hours > 0L) {
        "${hours}h ${remainingMinutes}m to full"
    } else {
        "${remainingMinutes}m to full"
    }
}