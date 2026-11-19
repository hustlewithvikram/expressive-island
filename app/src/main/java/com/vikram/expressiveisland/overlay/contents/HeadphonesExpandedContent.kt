package com.vikram.expressiveisland.overlay.contents

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.core.HeadphonesState

private val HeadphonesNotchSafeSpace = 36.dp

@Composable
fun HeadphonesConnectedExpandedContent(
    headphones: HeadphonesState,
) {
    val deviceName = headphones.name?.takeUnless { it.isBlank() } ?: "Headphones"
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        val viewportHeight = (maxHeight - HeadphonesNotchSafeSpace).coerceAtLeast(0.dp)
        val compact = viewportHeight < 76.dp
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(HeadphonesNotchSafeSpace))
            Column(
                modifier = Modifier.fillMaxWidth().height(viewportHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (compact) 20.dp else 24.dp),
                    color = colors.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (compact) 9.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(if (compact) 36.dp else 44.dp),
                            shape = CircleShape,
                            color = colors.primaryContainer,
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Headphones, contentDescription = null, modifier = Modifier.size(22.dp), tint = colors.onPrimaryContainer)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Headphones connected",
                                color = colors.onSurface,
                                fontSize = if (compact) 13.sp else 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(deviceName, color = colors.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!compact) Text("Ready to play", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
