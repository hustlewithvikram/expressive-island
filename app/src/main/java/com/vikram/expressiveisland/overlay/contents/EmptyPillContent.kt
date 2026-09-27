package com.vikram.expressiveisland.overlay.contents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.vikram.expressiveisland.data.CutoutColor
import com.vikram.expressiveisland.data.IconSource
import com.vikram.expressiveisland.overlay.MaterialIconCatalog
import com.vikram.expressiveisland.overlay.loadImageBitmapOrNull
import com.vikram.expressiveisland.overlay.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The resting (event-less) pill's optional glyph, centred on the collapsed cutout. A user-chosen
 * [containerColor] draws a filled disc with contrasting ink behind the glyph; without one, the glyph
 * sits directly on the pill in its content colour. The glyph is a picked image or a Material icon.
 */
@Composable
fun EmptyPillContent(
    icon: IconSource,
    containerColor: CutoutColor?,
    heightDp: Int,
    isStickToCamera: Boolean = false,
) {
    val context = LocalContext.current
    val badgeSize = (heightDp * 0.72f).dp
    val iconSize = (heightDp * 0.46f).dp

    val disc = containerColor?.resolve()
    val glyphColor = when {
        disc != null -> if (disc.luminance() > 0.5f) PillTextColorDark else PillTextColor
        else -> LocalContentColor.current
    }

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = icon) {
        value = when (icon) {
            is IconSource.Image -> withContext(Dispatchers.IO) {
                icon.uri.toUri().loadImageBitmapOrNull(context)
            }

            is IconSource.Material -> null
        }
    }
    val materialIcon =
        (icon as? IconSource.Material)?.let { MaterialIconCatalog.iconFor(it.iconName) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Sit on the leading edge like the normal cutout's icon (clear of the camera), or centred at
        // the bottom when the pill is stuck beside the camera — mirroring CollapsedContent.
        val placement = Modifier
            .align(if (isStickToCamera) Alignment.BottomCenter else Alignment.CenterStart)
            .padding(
                start = if (isStickToCamera) 0.dp else (heightDp * 0.16f).dp,
                bottom = if (isStickToCamera) (heightDp * 0.14f).dp else 0.dp,
            )
        Box(
            modifier = placement
                .size(badgeSize)
                .clip(CircleShape)
                .then(if (disc != null) Modifier.background(disc) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val loaded = bitmap
            when {
                loaded != null -> Image(
                    bitmap = loaded,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(badgeSize * 0.78f)
                        .clip(CircleShape),
                )

                materialIcon != null -> Icon(
                    imageVector = materialIcon,
                    contentDescription = null,
                    tint = glyphColor,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}