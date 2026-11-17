package com.vikram.expressiveisland.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Material 3 expressive "emphasized" easing — cubic-bezier(0.2, 0.0, 0.0, 1.0).
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** One destination in the [ExpressiveNavBar]. */
data class NavBarItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * A Material 3 "expressive" style bottom bar: a floating rounded container whose selected
 * item animates into a filled pill that reveals its label. Purely presentational —
 * selection state and clicks are hoisted to the caller.
 */
@Composable
fun ExpressiveNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(32.dp)
    Surface(
        modifier = modifier.border(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f),
            shape = barShape,
        ),
        shape = barShape,
        // Translucent theme surface gives a frosted-glass look while keeping text readable.
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.70f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { index, item ->
                NavBarPill(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun NavBarPill(
    item: NavBarItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 350, easing = EmphasizedEasing),
        label = "navPillContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 350, easing = EmphasizedEasing),
        label = "navPillContent",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(horizontal = if (selected) 18.dp else 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = contentColor,
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(durationMillis = 300, easing = EmphasizedEasing)) +
                expandHorizontally(tween(durationMillis = 350, easing = EmphasizedEasing)),
            exit = fadeOut(tween(durationMillis = 150, easing = EmphasizedEasing)) +
                shrinkHorizontally(tween(durationMillis = 250, easing = EmphasizedEasing)),
        ) {
            Text(
                text = item.label,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
