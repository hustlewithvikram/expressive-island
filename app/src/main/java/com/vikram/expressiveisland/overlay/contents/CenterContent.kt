package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.TorchStateBus
import com.vikram.expressiveisland.data.ActionButtonAnimation
import com.vikram.expressiveisland.data.CenterShortcut

/**
 * The expanded "center" the resting pill opens with [com.vikram.expressiveisland.data.EmptyClickAction.OPEN_CENTER]:
 * a titled row of round shortcut buttons, scrolling horizontally when they overflow. Sits in the
 * lower part of the cutout (clear of the camera), mirroring [ExpandedContent]'s placement.
 */
@Composable
fun CenterContent(
    shortcuts: List<CenterShortcut>,
    showLabels: Boolean,
    fillContainers: Boolean,
    themedIcons: Boolean,
    onContentHeight: (Int) -> Unit,
    onShortcut: (CenterShortcut) -> Unit,
) {
    val density = LocalDensity.current.density
    val torchOn by TorchStateBus.on.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                // Report the content's natural height so the cutout can fit itself to it.
                .onGloballyPositioned { onContentHeight((it.size.height / density).toInt()) },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.center_shortcuts_title),
                color = LocalContentColor.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (shortcuts.isEmpty()) {
                Text(
                    text = stringResource(R.string.center_shortcuts_empty),
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            } else {
                // Every shortcut shares the width equally (flex: 1), so the row always fills the
                // cutout with evenly-spread buttons. Under the EXPAND press animation a pressed button
                // borrows width from its siblings (they spring thinner) instead of overflowing in
                // place — the same give-and-take as the action chips (see [ActionChipRow]).
                val redistribute =
                    LocalActionButtonAnimation.current == ActionButtonAnimation.EXPAND &&
                            shortcuts.size > 1
                val interactions = remember(shortcuts.size) {
                    List(shortcuts.size) { MutableInteractionSource() }
                }
                val pressedFlags = interactions.map { it.collectIsPressedAsState().value }
                val pressedIndex = if (redistribute) pressedFlags.indexOfFirst { it } else -1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shortcuts.forEachIndexed { i, shortcut ->
                        val target = when {
                            !redistribute || pressedIndex < 0 -> 1f
                            i == pressedIndex -> 1f + FULL_EXPAND_DELTA
                            else -> 1f - FULL_EXPAND_DELTA / (shortcuts.size - 1)
                        }
                        val weight by animateFloatAsState(
                            targetValue = target,
                            animationSpec = spring(
                                dampingRatio = 0.42f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "centerWeight",
                        )
                        CenterShortcutButton(
                            shortcut = shortcut,
                            showLabel = showLabels,
                            fillContainer = fillContainers,
                            themedIcon = themedIcons,
                            active = shortcut is CenterShortcut.Torch && torchOn,
                            onClick = { onShortcut(shortcut) },
                            interaction = interactions[i],
                            // When redistributing, the width give-and-take IS the press animation, so
                            // the button must not also widen itself in place.
                            animatePress = !redistribute,
                            modifier = Modifier.weight(weight),
                        )
                    }
                }
            }
        }
    }
}