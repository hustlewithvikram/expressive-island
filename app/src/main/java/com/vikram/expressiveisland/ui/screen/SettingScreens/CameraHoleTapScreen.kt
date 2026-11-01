package com.vikram.expressiveisland.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.CameraHoleTapAction
import com.vikram.expressiveisland.ui.AppViewModel

/** Settings for the transparent hit target over the physical camera hole, independent of Empty Island. */
@Composable
internal fun CameraHoleTapScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val assistant by viewModel.assistantTile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.camera_hole_enabled_title),
            description = stringResource(R.string.camera_hole_enabled_desc),
            checked = behaviour.cameraHoleTapEnabled,
            onCheckedChange = viewModel::setCameraHoleTapEnabled,
        )

        AnimatedVisibility(visible = behaviour.cameraHoleTapEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = stringResource(R.string.camera_hole_quick_actions_title),
                    description = stringResource(R.string.camera_hole_quick_actions_desc),
                    checked = assistant.tapToOpenAssistant,
                    onCheckedChange = viewModel::setAssistantTapToOpen,
                )

                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = stringResource(R.string.camera_hole_assistant_title),
                    description = stringResource(R.string.camera_hole_assistant_desc),
                    checked = behaviour.cameraHoleAssistantEnabled,
                    onCheckedChange = viewModel::setCameraHoleAssistantEnabled,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.camera_hole_action_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.camera_hole_action_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CameraHoleTapAction.entries.forEach { action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = behaviour.cameraHoleTapAction == action,
                                        onClick = { viewModel.setCameraHoleTapAction(action) },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = behaviour.cameraHoleTapAction == action,
                                    onClick = null,
                                )
                                Text(
                                    text = stringResource(action.labelRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            }
                        }
                    }
                }

                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = stringResource(R.string.camera_hole_haptics_title),
                    description = stringResource(R.string.camera_hole_haptics_desc),
                    checked = behaviour.cameraHoleTapHaptics,
                    onCheckedChange = viewModel::setCameraHoleTapHaptics,
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
