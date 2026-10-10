package com.vikram.expressiveisland.overlay.contents

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.MediaArtBus
import com.vikram.expressiveisland.core.NowPlaying
import com.vikram.expressiveisland.core.OnCall
import com.vikram.expressiveisland.core.RunningTimerBus
import com.vikram.expressiveisland.data.ActionButtonAnimation
import com.vikram.expressiveisland.data.ActionButtonStyle
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.data.CenterShortcut
import com.vikram.expressiveisland.data.CutoutColor
import com.vikram.expressiveisland.data.MusicButtonStyle
import com.vikram.expressiveisland.data.MusicVisualizerStyle
import com.vikram.expressiveisland.overlay.CenterShortcutCatalog
import com.vikram.expressiveisland.overlay.DynamicIsland
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.island.IslandIcon
import com.vikram.expressiveisland.overlay.NotificationHeaderResolver
import com.vikram.expressiveisland.overlay.albumArtShape
import com.vikram.expressiveisland.overlay.forRole
import com.vikram.expressiveisland.overlay.formatCallDuration
import com.vikram.expressiveisland.overlay.onDynamicRole
import com.vikram.expressiveisland.overlay.onForRole
import com.vikram.expressiveisland.overlay.representativeColor
import com.vikram.expressiveisland.overlay.resolve
import com.vikram.expressiveisland.overlay.resolveBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

// Text colours for a dark fill; on a light fill we swap in a dark text colour (see contentColorFor).
val PillTextColor = Color(0xFFF5F5F5)
val PillTextColorDark = Color(0xFF0A0A0A)

// Time for the rotating album art to complete one full turn.
const val ALBUM_SPIN_MS = 8000

// The optional ring around the album cover, as fractions of the cover's own footprint: the stroke
// itself, then the breathing space between it and the artwork. Kept proportional so the ring reads
// the same on the collapsed pill and in the (larger) expanded layout.
const val ALBUM_STROKE_FRACTION = 0.055f
const val ALBUM_STROKE_GAP_FRACTION = 0.06f

/**
 * The button press reaction, chosen in settings and provided by [DynamicIsland] so every
 * [pressScale] call site (action chips, reply buttons, call buttons) picks it up without threading
 * the setting through each one. Defaults to [ActionButtonAnimation.SCALE].
 */
val LocalActionButtonAnimation = staticCompositionLocalOf { ActionButtonAnimation.SCALE }

// How far the EXPAND press animation widens a button, on each side.
val PressExpandDp = 7.dp

// Layout metrics for the call cutout, shared by CallNormalContent (which draws it) and
// callCutoutWidthPercent (which measures the name to size the pill) so the two stay in agreement.
const val CALL_ROW_PADDING_DP = 8
const val CALL_ROW_SPACING_DP = 12

// The photo/icon container and the hang-up button are deliberately the same size so the cutout
// reads as symmetrical, with the caller between two equal circles.
const val CALL_HANGUP_BUTTON_DP = 44
const val CALL_AVATAR_DP = CALL_HANGUP_BUTTON_DP
const val CALL_NAME_SIZE_SP = 15

// A little breathing room so the name never sits flush against the button before the pill grows.
const val CALL_NAME_SLACK_DP = 8

// Metrics for the two-row incoming-call layout (caller row over Take / Hang up buttons). The layout
// grows past the expanded cutout by [callIncomingExtraDp] so the caller row can sit below the camera
// hole with a flexible gap before the buttons pinned to the bottom edge.
const val CALL_INCOMING_SIDE_PAD_DP = 14
const val CALL_INCOMING_BOTTOM_PAD_DP = 14

// Top clearance for the camera hole, matching the empty top the expanded card leaves for it.
const val CALL_INCOMING_TOP_PAD_DP = 34
const val CALL_INCOMING_BUTTON_GAP_DP = 10
const val CALL_INCOMING_BUTTON_DP = 44
const val CALL_INCOMING_AVATAR_DP = 40

// Vertical spacing added around the action row on top of the chip height itself. Must equal the
// expanded column's own child spacing, or a notification with actions and one without end up with
// their header rows at different heights.
const val ACTIONS_ROW_SPACING_DP = 6

/** Fallback fill for a button asked to be [MusicButtonStyle.filled] before the user picks a colour. */
val MusicButtonFilledDefault = Color(0xFFE0E0E0)

// How often the music progress bar re-reads its own clock. The media session pushes nothing between
// real changes, so this is the bar's only motion; a whole bar width is a track long, which makes
// even a half-second step sub-pixel.
const val PROGRESS_TICK_MS = 500L

// In a full-width (flex) row, how much extra weight a pressed chip borrows from its siblings under
// the EXPAND animation: it grows by this share while the others give up the same total between them,
// so the row always fills exactly its own width.
const val FULL_EXPAND_DELTA = 0.15f

// The height of each shortcut button in the expanded center (its diameter too, in disc mode).
val CenterDiscDp = 64.dp

/**
 * The press reaction shared by the action chips and the reply buttons, so every tap on the island
 * feels the same. Two flavours, selected via [LocalActionButtonAnimation]:
 * [ActionButtonAnimation.SCALE] is the expressive "squish" — a springy scale-down that settles back
 * with a little bounce on release; [ActionButtonAnimation.EXPAND] instead briefly widens the button
 * by [PressExpandDp] on each side. Both animate on the same spring and via [graphicsLayer], so the
 * surrounding layout never reflows.
 */
@Composable
fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    pressedScale: Float = 0.88f,
): Modifier {
    val animation = LocalActionButtonAnimation.current
    val pressed by interaction.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return this.graphicsLayer {
        when (animation) {
            ActionButtonAnimation.SCALE -> {
                val scale = 1f + (pressedScale - 1f) * progress
                scaleX = scale
                scaleY = scale
            }

            ActionButtonAnimation.EXPAND -> {
                // Grow the width by PressExpandDp on each side, expressed as a scale relative to the
                // button's own measured width so layout stays put.
                val extraPx = PressExpandDp.toPx() * 2f * progress
                if (size.width > 0f) scaleX = (size.width + extraPx) / size.width
            }
        }
    }
}

/**
 * A single action chip. [style] selects between the Material 3 Expressive and Material You look;
 * [fill] is the base colour those looks derive their container/outline from.
 */
@Composable
fun ActionChip(
    action: IslandAction,
    style: ActionButtonStyle,
    fill: Color,
    heightDp: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
    animatePress: Boolean = true,
) {
    val shape = when (style) {
        ActionButtonStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
        else -> CircleShape
    }
    val container = when (style) {
        ActionButtonStyle.EXPRESSIVE_TONAL -> fill.copy(alpha = 0.22f)
        ActionButtonStyle.EXPRESSIVE_FILLED -> fill
        ActionButtonStyle.MATERIAL_YOU -> fill.copy(alpha = 0.16f)
        ActionButtonStyle.OUTLINED -> Color.Transparent
    }
    val content = when (style) {
        // A solid fill needs ink that contrasts with it; the rest sit on a translucent tint.
        ActionButtonStyle.EXPRESSIVE_FILLED -> if (fill.luminance() > 0.5f) PillTextColorDark else PillTextColor
        ActionButtonStyle.OUTLINED -> fill
        else -> LocalContentColor.current
    }
    val border = if (style == ActionButtonStyle.OUTLINED) {
        BorderStroke(1.5.dp, fill.copy(alpha = 0.7f))
    } else {
        null
    }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = shape,
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier
            .height(heightDp.dp)
            .then(if (animatePress) Modifier.pressScale(interaction) else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = action.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
        }
    }
}

@Composable
fun IconBadge(
    event: IslandEvent,
    badgeSize: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    // A tile's chosen container colour wins: a filled disc with contrasting ink. A per-event colour
    // override then recolours the default look (a faint tinted disc + full-colour glyph). Otherwise,
    // "Dynamic color for all events" gives a role-coloured badge with its matching "on" ink, and the
    // plain default is a faint accent-tinted disc behind a full-accent glyph.
    val container = event.iconContainerColor
    val override = event.colorOverride
    val badgeColor: Color
    val glyphColor: Color
    when {
        container != null -> {
            badgeColor = container.resolve()
            glyphColor = when (container) {
                is CutoutColor.Dynamic -> onDynamicRole(container.role)
                is CutoutColor.Solid ->
                    if (badgeColor.luminance() > 0.5f) PillTextColorDark else PillTextColor

                is CutoutColor.AppIcon ->
                    if (badgeColor.luminance() > 0.5f) PillTextColorDark else PillTextColor
            }
        }

        override != null -> {
            val tint = override.resolve()
            badgeColor = tint.copy(alpha = 0.20f)
            glyphColor = tint
        }

        event.useThemeColor -> {
            badgeColor = MaterialTheme.colorScheme.forRole(event.themeColorRole)
                .copy(alpha = event.themeColorOpacity)
            glyphColor = MaterialTheme.colorScheme.onForRole(event.themeColorRole)
        }

        else -> {
            badgeColor = event.accent.copy(alpha = 0.20f)
            glyphColor = event.accent
        }
    }
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center,
    ) {
        when (val icon = event.icon) {
            is IslandIcon.Vector -> Icon(
                imageVector = icon.image,
                contentDescription = null,
                tint = glyphColor,
                modifier = Modifier.size(iconSize),
            )

            // Full-colour art fills the badge disc; a monochrome glyph (a notification's small
            // icon) is drawn at glyph size in the badge's ink instead, like a vector icon.
            is IslandIcon.Raster -> Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                contentScale = if (icon.tint) ContentScale.Fit else ContentScale.Crop,
                colorFilter = if (icon.tint) ColorFilter.tint(glyphColor) else null,
                modifier = if (icon.tint) {
                    Modifier.size(iconSize)
                } else {
                    Modifier
                        .size(badgeSize * 0.78f)
                        .clip(CircleShape)
                },
            )

            is IslandIcon.Lottie -> {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(icon.resId),
                )
                // Each animation carries its own playback: the unlock padlock clips to frames 0..45
                // and holds open, while a looping icon (e.g. charging) runs its full range forever.
                val clip = if (icon.clipStartFrame != null && icon.clipEndFrame != null) {
                    LottieClipSpec.Frame(icon.clipStartFrame, icon.clipEndFrame)
                } else {
                    null
                }
                // Recolour every layer to the badge glyph colour (the settings' role/accent) when asked.
                val dynamicProperties = if (icon.tint) {
                    rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.COLOR_FILTER,
                            value = SimpleColorFilter(glyphColor.toArgb()),
                            keyPath = arrayOf("**"),
                        ),
                    )
                } else {
                    null
                }
                key(icon.clipStartFrame, icon.clipEndFrame, icon.iterations, icon.speed) {
                    LottieAnimation(
                        composition = composition,
                        iterations = icon.iterations,
                        clipSpec = clip,
                        speed = icon.speed,
                        dynamicProperties = dynamicProperties,
                        // requiredSize (not size) so a scale > 1 can render past the badge bounds instead of
                        // being clamped to them; the overflow is clipped to the badge circle by the parent.
                        modifier = Modifier.requiredSize(iconSize * icon.scale),
                    )
                }
            }
        }
    }
}

/**
 * Album art, cropped to fill. Normally a rounded square; when [rotate] is on it becomes a disc that
 * spins ([ALBUM_SPIN_MS] per turn) while [playing], freezing at its current angle when paused. A
 * non-null [strokeColor] rings the cover, set apart from it by a small gap.
 */
@Composable
fun AlbumArt(
    bitmap: ImageBitmap,
    size: Dp,
    modifier: Modifier = Modifier,
    rotate: Boolean = false,
    playing: Boolean = false,
    /** Colour of the ring drawn around the cover, or null to leave it bare. */
    strokeColor: Color? = null,
) {
    val angle = remember { Animatable(0f) }
    // Spin only while enabled and playing; on pause the effect cancels and the angle holds. Restart
    // repeats identical 0→360 turns from the held value, so a pause/resume is seamless.
    LaunchedEffect(rotate, playing) {
        if (rotate && playing) {
            angle.animateTo(
                targetValue = angle.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = ALBUM_SPIN_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
        }
    }
    // The ring sits inside the cover's existing footprint rather than around it, so switching it on
    // never grows the badge or shoves the pill's text along. Without a ring the artwork fills the
    // footprint exactly as before.
    val strokeWidth = if (strokeColor != null) size * ALBUM_STROKE_FRACTION else 0.dp
    val gap = if (strokeColor != null) size * ALBUM_STROKE_GAP_FRACTION else 0.dp
    val coverSize = size - (strokeWidth + gap) * 2

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (strokeColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // A spinning square would visibly swing its corners, so a rotatable cover — and
                    // the ring tracking it — is drawn as a circle.
                    .border(strokeWidth, strokeColor, albumArtShape(rotate, size)),
            )
        }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(coverSize)
                .rotate(if (rotate) angle.value else 0f)
                .clip(albumArtShape(rotate, coverSize)),
        )
    }
}

/** The caller's contact photo, cropped to a circle. Mirrors [AlbumArt] without the spin. */
@Composable
fun ContactPhoto(bitmap: ImageBitmap, size: Dp, modifier: Modifier = Modifier) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

/**
 * The remaining time on the timer tile, formatted m:ss (or h:mm:ss past an hour), or null when no
 * timer is present. Reads [RunningTimerBus]: a running timer ticks down against
 * [SystemClock.elapsedRealtime] (re-derived a few times a second so the collapsed pill and expanded
 * card stay in sync), while a paused timer shows its frozen remainder without ticking. Seconds are
 * rounded up so a fresh 5:00 timer reads "5:00", and it lands on "0:00" exactly at zero.
 */
@Composable
/**
 * Shared title treatment for expanded dynamic tiles.
 *
 * Keeps the headline and optional supporting label consistent across Music, Phone,
 * Timer, and Assistant while allowing each tile to provide its own text.
 */
@Composable
fun TileTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = LocalContentColor.current,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let { supportingText ->
            Text(
                text = supportingText,
                color = LocalContentColor.current.copy(alpha = 0.70f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun timerRemainingText(): String? {
    val timer by RunningTimerBus.state.collectAsStateWithLifecycle()
    val t = timer ?: return null
    val end = t.endElapsedRealtimeMs
    val remainingMs = if (end != null) {
        var nowElapsed by remember(end) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(end) {
            while (true) {
                nowElapsed = SystemClock.elapsedRealtime()
                delay(250L.milliseconds)
            }
        }
        (end - nowElapsed).coerceAtLeast(0L)
    } else {
        (t.pausedRemainingMs ?: return null).coerceAtLeast(0L)
    }
    return formatCallDuration((remainingMs + 999L) / 1_000L)
}

/** The phone tile's secondary line: a duration that ticks up once connected, else "incoming call". */
@Composable
fun CallStatus(onCall: OnCall?) {
    val start = onCall?.startTimeMs
    val text = if (start != null) {
        var now by remember(start) { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(start) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1_000L.milliseconds)
            }
        }
        formatCallDuration(((now - start) / 1_000L).coerceAtLeast(0L))
    } else if (onCall != null) {
        stringResource(R.string.phone_ringing)
    } else {
        return
    }
    Text(
        text = text,
        color = LocalContentColor.current.copy(alpha = 0.70f),
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A round, filled call button (hang up / decline / answer) on the trailing edge of the call cutout. */
@Composable
fun CallCircleButton(
    icon: ImageVector,
    description: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    FilledIconButton(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = content,
        ),
        modifier = Modifier
            .size(CALL_HANGUP_BUTTON_DP.dp)
            .pressScale(interaction),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A full-width, filled call button (Take / Hang up) with a leading icon and label, used by the
 *  incoming-call layout's bottom row. */
@Composable
fun CallWideButton(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = modifier
            .height(CALL_INCOMING_BUTTON_DP.dp)
            .pressScale(interaction),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The cover to draw for the music tile, or null to fall back to the note glyph. The media session's
 * own art wins; a player that publishes its cover as a remote URI (Spotify) leaves the session
 * without one, so the tile uses the cover lifted off that same player's media notification. Matched
 * on package so a stale cover is never drawn over a different player's track.
 */
@Composable
fun albumArtFor(event: IslandEvent, nowPlaying: NowPlaying?): ImageBitmap? {
    val notificationArt by MediaArtBus.state.collectAsStateWithLifecycle()
    if (event.media?.showAlbumArt != true) return null
    return nowPlaying?.albumArt
        ?: notificationArt?.takeIf { it.packageName == nowPlaying?.packageName }?.art
}

/**
 * The colour of the ring around the album cover, or null when the user hasn't asked for one. An
 * enabled ring with no colour picked falls back to the tile's own accent, matching what the
 * settings screen offers as its default swatch.
 */
@Composable
fun albumArtStrokeFor(event: IslandEvent): Color? =
    event.media?.takeIf { it.albumArtStroke }
        ?.let { it.albumArtStrokeColor?.resolve() ?: event.accent }

/** Keeps a visible notification's relative timestamp current while the expanded island is open. */
@Composable
fun rememberNotificationHeader(
    event: IslandEvent,
    appearance: AppearanceSettings,
): String? {
    val nowMs by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = event.id,
        key2 = event.notificationPostTimeMs,
        key3 = appearance.showTimestamp,
    ) {
        if (appearance.showTimestamp && event.notificationPostTimeMs != null) {
            while (true) {
                value = System.currentTimeMillis()
                delay(1_000L.milliseconds)
            }
        }
    }

    return NotificationHeaderResolver.resolveHeader(
        appName = event.notificationAppName,
        postTimeMs = event.notificationPostTimeMs,
        showAppName = appearance.showSourceAppName,
        showTimestamp = appearance.showTimestamp,
        nowMs = nowMs,
    )
}

/** An app's loaded icon, and whether it's the themed (monochrome, tint-me) glyph vs the full-colour icon. */
class LoadedAppIcon(val bitmap: ImageBitmap, val themed: Boolean)

/**
 * Loads an app's launcher icon off the main thread, or null if the package is gone. When [themed] is
 * on and the app ships an adaptive icon with a monochrome layer (API 33+), that layer is returned to
 * be tinted like the built-in shortcut glyphs; otherwise the full-colour icon is used.
 */
@Composable
fun rememberAppIcon(packageName: String, themed: Boolean): LoadedAppIcon? {
    val context = LocalContext.current
    val icon by produceState<LoadedAppIcon?>(initialValue = null, packageName, themed) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val monochrome =
                    if (themed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        (drawable as? AdaptiveIconDrawable)?.monochrome
                    } else {
                        null
                    }
                if (monochrome != null) {
                    LoadedAppIcon(monochrome.toBitmap().asImageBitmap(), themed = true)
                } else {
                    LoadedAppIcon(drawable.toBitmap().asImageBitmap(), themed = false)
                }
            }.getOrNull()
        }
    }
    return icon
}

/**
 * A single center shortcut: its container (a fixed disc, or a slot-filling pill when [fillContainer])
 * holding the glyph or the real launcher icon, with a small label beneath. A togglable shortcut that
 * is [active] lights up in the theme's primary / on-primary. Shares the island's [pressScale].
 */
@Composable
fun CenterShortcutButton(
    shortcut: CenterShortcut,
    showLabel: Boolean,
    fillContainer: Boolean,
    themedIcon: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
    animatePress: Boolean = true,
) {
    val containerColor =
        if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.14f)
    val glyphColor = if (active) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
    val shapeModifier = if (fillContainer) {
        Modifier
            .fillMaxWidth()
            .height(CenterDiscDp)
    } else {
        Modifier.size(CenterDiscDp)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            shape = CircleShape,
            color = containerColor,
            contentColor = glyphColor,
            modifier = shapeModifier.then(if (animatePress) Modifier.pressScale(interaction) else Modifier),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val appIcon = (shortcut as? CenterShortcut.LaunchApp)?.let {
                    rememberAppIcon(
                        it.packageName,
                        themedIcon
                    )
                }
                when {
                    // A themed (monochrome) app icon: tint its glyph to match the built-in shortcuts.
                    // Its safe-zone padding means it reads well filling the whole button.
                    appIcon?.themed == true -> Icon(
                        bitmap = appIcon.bitmap,
                        contentDescription = null,
                        tint = glyphColor,
                        modifier = Modifier.size(CenterDiscDp),
                    )

                    appIcon != null -> Image(
                        bitmap = appIcon.bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(CenterDiscDp * 0.6f)
                            .clip(CircleShape),
                    )

                    else -> Icon(
                        imageVector = CenterShortcutCatalog.iconFor(shortcut),
                        contentDescription = null,
                        tint = glyphColor,
                        modifier = Modifier.size(CenterDiscDp * 0.46f),
                    )
                }
            }
        }
        if (showLabel) {
            Text(
                text = centerShortcutLabel(shortcut),
                color = LocalContentColor.current.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The label for a shortcut: its fixed string resource, or the app's display name for a launcher. */
@Composable
fun centerShortcutLabel(shortcut: CenterShortcut): String {
    CenterShortcutCatalog.labelResFor(shortcut)?.let { return stringResource(it) }
    val pkg = (shortcut as? CenterShortcut.LaunchApp)?.packageName ?: return ""
    val context = LocalContext.current
    val label by produceState(initialValue = pkg, pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
        }
    }
    return label
}

/**
 * The island's surface: shadow, optional stroke and the background fill. [progress] (0 = collapsed,
 * 1 = expanded) cross-fades the normal fill into the expanded fill, so if the two states use
 * different colours (or gradients) the background morphs in lockstep with the size animation.
 */
@Composable
fun IslandSurface(
    modifier: Modifier,
    shape: Shape,
    appearance: AppearanceSettings,
    progress: Float,
    appColor: Color? = null,
    adaptiveColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val normalBrush = appearance.backgroundNormal.resolveBrush(appColor, adaptiveColor)
    val expandedBrush = appearance.backgroundExpanded.resolveBrush(appColor, adaptiveColor)
    val repColor = lerp(
        appearance.backgroundNormal.representativeColor(appColor, adaptiveColor),
        appearance.backgroundExpanded.representativeColor(appColor, adaptiveColor),
        progress,
    )

    val contentColor = if (repColor.luminance() > 0.5f) PillTextColorDark else PillTextColor
    // Resolve the configurable shadow colour in composition; graphicsLayer is not composable.
    val shadowColor = appearance.shadowColor.resolve(appColor, adaptiveColor)
    val border = if (appearance.strokeEnabled) {
        BorderStroke(
            appearance.strokeWidthDp.dp,
            appearance.strokeColor.resolve(appColor, adaptiveColor)
        )
    } else {
        null
    }

    Surface(
        modifier = modifier.graphicsLayer {
            // Draw the elevation shadow using the user-selected colour, rather than the default black.
            shadowElevation = if (appearance.shadowEnabled) 6.dp.toPx() else 0f
            this.shape = shape
            clip = false
            spotShadowColor = shadowColor
            ambientShadowColor = shadowColor
        },
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = border,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(normalBrush)
            )
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = progress }
                        .background(expandedBrush),
                )
            }
            content()
        }
    }
}

/* Compact music visualizers used on the trailing edge of the collapsed island. */
@Composable
fun MusicVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    style: MusicVisualizerStyle = MusicVisualizerStyle.BARS,
) {
    val transition = rememberInfiniteTransition(label = "musicVisualizer")
    val bars = listOf(0.35f to 1350, 0.55f to 1140, 0.25f to 1575, 0.45f to 1260, 0.30f to 1470, 0.40f to 1050)
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.width(if (style == MusicVisualizerStyle.PULSE) 22.dp else 28.dp).height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bars.forEachIndexed { index, (minHeight, duration) ->
            val animated by transition.animateFloat(
                initialValue = minHeight,
                targetValue = when (index % 3) { 0 -> 0.75f; 1 -> 0.95f; else -> 0.65f },
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "visualizerBar$index",
            )
            val fraction = if (isPlaying) animated else 0.2f
            when (style) {
                MusicVisualizerStyle.BARS -> Box(
                    Modifier.width(2.5.dp).fillMaxHeight(fraction).clip(RoundedCornerShape(3.dp)).background(color)
                )
                MusicVisualizerStyle.MIRRORED -> Box(
                    Modifier.width(2.5.dp).fillMaxHeight(fraction).clip(RoundedCornerShape(3.dp))
                        .background(color).graphicsLayer { scaleY = -1f }
                )
                MusicVisualizerStyle.DOTS -> Box(
                    Modifier.size(if (isPlaying) (3.dp + (animated * 4).dp) else 3.dp)
                        .clip(CircleShape).background(color)
                )
                MusicVisualizerStyle.WAVE -> Box(
                    Modifier.width(2.dp).fillMaxHeight(if (isPlaying) (0.25f + 0.7f * kotlin.math.abs(kotlin.math.sin(index * 0.9f + animated * 2f))) else 0.2f)
                        .clip(RoundedCornerShape(2.dp)).background(color)
                )
                MusicVisualizerStyle.PULSE -> Box(
                    Modifier.size(if (isPlaying) (4.dp + (animated * 5).dp) else 4.dp)
                        .clip(CircleShape).background(color.copy(alpha = if (isPlaying) 0.55f + animated * 0.45f else 0.5f))
                )
            }
        }
    }
}