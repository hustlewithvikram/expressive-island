package com.vikram.expressiveisland.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.CameraHoleTapAction
import com.vikram.expressiveisland.ui.AppViewModel

private fun cameraHoleGroupShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) 28.dp else 4.dp,
    topEnd = if (isFirst) 28.dp else 4.dp,
    bottomStart = if (isLast) 28.dp else 4.dp,
    bottomEnd = if (isLast) 28.dp else 4.dp,
)

/** Settings for the transparent hit target over the physical camera hole, independent of Empty Island. */
@Composable
internal fun CameraHoleTapScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val assistant by viewModel.assistantTile.collectAsStateWithLifecycle()
    var showTouchActionDialog by remember { mutableStateOf(false) }
    val selectedAction = when {
        behaviour.cameraHoleAssistantEnabled -> CameraHoleTapAction.APP_ASSISTANT
        assistant.tapToOpenAssistant -> CameraHoleTapAction.QUICK_ACTIONS
        else -> behaviour.cameraHoleTapAction
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.camera_hole_section_touch),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
        )
        SettingsToggleCard(
            shape = cameraHoleGroupShape(isFirst = true, isLast = !behaviour.cameraHoleTapEnabled),
            title = stringResource(R.string.camera_hole_enabled_title),
            description = stringResource(R.string.camera_hole_enabled_desc),
            checked = behaviour.cameraHoleTapEnabled,
            onCheckedChange = viewModel::setCameraHoleTapEnabled,
        )

        AnimatedVisibility(visible = behaviour.cameraHoleTapEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Card(
                    onClick = { showTouchActionDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = cameraHoleGroupShape(isFirst = false, isLast = true),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.camera_hole_action_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(selectedAction.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Tap to choose what happens when the camera hole is touched",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Choose touch action",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (showTouchActionDialog) {
                    AlertDialog(
                        onDismissRequest = { showTouchActionDialog = false },
                        title = { Text(stringResource(R.string.camera_hole_action_title)) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                CameraHoleTapAction.entries.forEach { action ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setCameraHoleTapAction(action)
                                                showTouchActionDialog = false
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = selectedAction == action,
                                            onClick = {
                                                viewModel.setCameraHoleTapAction(action)
                                                showTouchActionDialog = false
                                            },
                                        )
                                        Text(
                                            text = stringResource(action.labelRes),
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(start = 10.dp),
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showTouchActionDialog = false }) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        },
                    )
                }

                Text(
                    text = stringResource(R.string.camera_hole_section_feedback),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp),
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = stringResource(R.string.camera_hole_haptics_title),
                    description = stringResource(R.string.camera_hole_haptics_desc),
                    checked = behaviour.cameraHoleTapHaptics,
                    onCheckedChange = viewModel::setCameraHoleTapHaptics,
                )

                Text(
                    text = stringResource(R.string.camera_hole_section_requirements),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        text = stringResource(R.string.camera_hole_shizuku_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (!behaviour.cameraHoleTapEnabled) {
            Text(
                text = stringResource(R.string.camera_hole_disabled_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
