package com.vikram.expressiveisland.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.ActionButtonAnimation
import com.vikram.expressiveisland.data.AnimationBounce
import com.vikram.expressiveisland.data.AnimationSpeed
import com.vikram.expressiveisland.data.AnimationStyle
import com.vikram.expressiveisland.data.BehaviourSettings
import com.vikram.expressiveisland.overlay.IslandMotion
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.components.ExpressiveSegmentedRow
import com.vikram.expressiveisland.ui.screen.AdjustableSlider
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private fun groupedShape(
    isFirst: Boolean,
    isLast: Boolean,
) = RoundedCornerShape(
    topStart = if (isFirst) 28.dp else 6.dp,
    topEnd = if (isFirst) 28.dp else 6.dp,
    bottomStart = if (isLast) 28.dp else 6.dp,
    bottomEnd = if (isLast) 28.dp else 6.dp,
)

@Composable
internal fun AnimationScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()

    var animationMs by remember(behaviour.animationDurationMs) {
        mutableStateOf(behaviour.animationDurationMs.toFloat())
    }

    val expressive =
        behaviour.animationStyle == AnimationStyle.EXPRESSIVE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {

        // ─────────────────────────────────────────────
        // PREVIEW
        // ─────────────────────────────────────────────

        AnimationExampleCard(
            shape = RoundedCornerShape(28.dp),
            speed = behaviour.animationSpeed,
            bounce = behaviour.animationBounce,
            durationMs = behaviour.animationDurationMs,
        )

        // ─────────────────────────────────────────────
        // MOTION
        // ─────────────────────────────────────────────

        AnimationSectionTitle(
            title = "Motion",
            description = "Control how the island moves and responds.",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AnimationSegmentedRow(
                shape = groupedShape(
                    isFirst = true,
                    isLast = expressive.not(),
                ),
                label = stringResource(R.string.animation_style),
                options = listOf(
                    stringResource(R.string.animation_style_expressive),
                    stringResource(R.string.animation_style_ease),
                ),
                selectedIndex = behaviour.animationStyle.ordinal,
                onSelect = {
                    viewModel.setAnimationStyle(
                        AnimationStyle.entries[it]
                    )
                },
            )

            AnimatedVisibility(visible = expressive) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AnimationSegmentedRow(
                        shape = groupedShape(
                            isFirst = false,
                            isLast = false,
                        ),
                        label = stringResource(R.string.animation_speed),
                        options = listOf(
                            stringResource(R.string.animation_speed_slow),
                            stringResource(R.string.animation_speed_default),
                            stringResource(R.string.animation_speed_fast),
                        ),
                        selectedIndex = behaviour.animationSpeed.ordinal,
                        onSelect = {
                            viewModel.setAnimationSpeed(
                                AnimationSpeed.entries[it]
                            )
                        },
                    )

                    AnimationSegmentedRow(
                        shape = groupedShape(
                            isFirst = false,
                            isLast = true,
                        ),
                        label = stringResource(R.string.animation_bounce),
                        options = listOf(
                            stringResource(R.string.animation_bounce_big),
                            stringResource(R.string.animation_bounce_normal),
                            stringResource(R.string.animation_bounce_small),
                        ),
                        selectedIndex = behaviour.animationBounce.ordinal,
                        onSelect = {
                            viewModel.setAnimationBounce(
                                AnimationBounce.entries[it]
                            )
                        },
                    )
                }
            }

            AnimatedVisibility(visible = !expressive) {
                AnimationDurationCard(
                    shape = groupedShape(
                        isFirst = false,
                        isLast = true,
                    ),
                    animationMs = animationMs,
                    onValueChange = {
                        animationMs = it
                    },
                    onCommit = {
                        viewModel.setAnimationDurationMs(
                            animationMs.roundToInt()
                        )
                    },
                )
            }
        }

        // ─────────────────────────────────────────────
        // BUTTONS
        // ─────────────────────────────────────────────

        AnimationSectionTitle(
            title = "Interaction",
            description = "Choose how action and reply buttons react when pressed.",
        )

        AnimationSegmentedRow(
            shape = RoundedCornerShape(28.dp),
            label = stringResource(R.string.animation_button),
            options = listOf(
                stringResource(R.string.animation_button_scale),
                stringResource(R.string.animation_button_expand),
            ),
            selectedIndex = behaviour.actionButtonAnimation.ordinal,
            onSelect = {
                viewModel.setActionButtonAnimation(
                    ActionButtonAnimation.entries[it]
                )
            },
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun AnimationSectionTitle(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 4.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnimationDurationCard(
    shape: Shape,
    animationMs: Float,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.behaviour_animation_duration
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Text(
                        text = "Adjust the transition speed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "${animationMs.roundToInt()} ms",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            AdjustableSlider(
                label = "",
                valueText = "",
                value = animationMs,
                valueRange =
                    BehaviourSettings.MIN_ANIMATION_DURATION_MS.toFloat()..
                            BehaviourSettings.MAX_ANIMATION_DURATION_MS.toFloat(),
                step = 20f,
                onValueChange = onValueChange,
                onCommit = onCommit,
            )
        }
    }
}

private val ExampleCollapsedSize = DpSize(
    96.dp,
    30.dp,
)

private val ExampleExpandedSize = DpSize(
    230.dp,
    56.dp,
)

@Composable
private fun AnimationExampleCard(
    shape: Shape,
    speed: AnimationSpeed,
    bounce: AnimationBounce,
    durationMs: Int,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_300)
            expanded = !expanded
        }
    }

    val expressiveMotion = remember(
        speed,
        bounce,
    ) {
        IslandMotion(
            AnimationStyle.EXPRESSIVE,
            speed,
            bounce,
            BehaviourSettings.DEFAULT_ANIMATION_DURATION_MS,
        )
    }

    val easeMotion = remember(
        durationMs,
    ) {
        IslandMotion(
            AnimationStyle.EASE_IN_OUT,
            AnimationSpeed.DEFAULT,
            AnimationBounce.NORMAL,
            durationMs,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = "See how the selected animation feels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ExamplePill(
                label = stringResource(
                    R.string.animation_style_expressive
                ),
                motion = expressiveMotion,
                expanded = expanded,
            )

            ExamplePill(
                label = stringResource(
                    R.string.animation_style_ease
                ),
                motion = easeMotion,
                expanded = expanded,
            )
        }
    }
}

@Composable
private fun ExamplePill(
    label: String,
    motion: IslandMotion,
    expanded: Boolean,
) {
    val target =
        if (expanded) {
            ExampleExpandedSize
        } else {
            ExampleCollapsedSize
        }

    val width by animateDpAsState(
        target.width,
        motion.dp(),
        label = "exampleWidth",
    )

    val height by animateDpAsState(
        target.height,
        motion.dp(),
        label = "exampleHeight",
    )

    val corner by animateDpAsState(
        if (expanded) {
            24.dp
        } else {
            ExampleCollapsedSize.height / 2
        },
        motion.dp(),
        label = "exampleCorner",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ExampleExpandedSize.height),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = width,
                        height = height,
                    )
                    .clip(
                        RoundedCornerShape(corner)
                    )
                    .background(
                        MaterialTheme.colorScheme.inverseSurface
                    ),
            )
        }
    }
}

@Composable
private fun AnimationSegmentedRow(
    shape: Shape,
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 15.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
            )

            ExpressiveSegmentedRow(
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
