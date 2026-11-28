package com.vikram.expressiveisland.ui.screen.tiles

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.DynamicTile
import com.vikram.expressiveisland.core.IslandPreviewCategory
import com.vikram.expressiveisland.data.CallState
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.components.ExpressiveSegmentedRow
import com.vikram.expressiveisland.ui.screen.ColorPickerCard
import com.vikram.expressiveisland.ui.screen.IslandLivePreviewEffect
import com.vikram.expressiveisland.ui.screen.IslandPreviewSpacer
import com.vikram.expressiveisland.ui.screen.SettingsToggleCard

private enum class PhoneSettingsTab(val label: String) {
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    ONGOING("Ongoing"),
}

/**
 * Phone tile editor. Each tab selects a real call state in the live island preview, while its
 * controls stay grouped by the part of the call lifecycle they affect.
 */
@Composable
internal fun PhoneTileScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.phoneTile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(PhoneSettingsTab.INCOMING) }
    var phoneStateGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> phoneStateGranted = granted }
    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> contactsGranted = granted }

    val previewDetail = when (selectedTab) {
        PhoneSettingsTab.INCOMING -> "Incoming call"
        PhoneSettingsTab.OUTGOING -> "Outgoing call"
        PhoneSettingsTab.ONGOING -> "Ongoing call"
    }
    val previewExpanded = selectedTab == PhoneSettingsTab.ONGOING && !settings.miniCall

    IslandLivePreviewEffect(
        category = IslandPreviewCategory.PHONE_TILE,
        label = "Alex Morgan",
        detail = previewDetail,
        expanded = previewExpanded,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The call preview can be taller than the standard expanded island. Keep the first setting
        // below its full visual footprint so the preview never covers controls while scrolling.
        IslandPreviewSpacer(
            extraHeightDp = when {
                selectedTab == PhoneSettingsTab.INCOMING && settings.expandedIncomingLayout -> 230
                selectedTab == PhoneSettingsTab.ONGOING && !settings.miniCall -> 270
                else -> 76
            },
        )

        ExpressiveSegmentedRow(
            options = PhoneSettingsTab.entries.map { it.label },
            selectedIndex = PhoneSettingsTab.entries.indexOf(selectedTab),
            onSelect = { selectedTab = PhoneSettingsTab.entries[it] },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )

        if (settings.showPhoto && !contactsGranted) {
            Text(
                text = "The dialer's own caller image is used when available. Allow contact access to load saved contact photos when the dialer does not provide one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Button(
                onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text("Allow contact access")
            }
        }

        when (selectedTab) {
            PhoneSettingsTab.INCOMING -> {
                SectionLabel("Incoming call")
                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = "Enable incoming calls",
                    description = "Show the island for incoming calls. Turn this off to ignore incoming-call events.",
                    checked = settings.incomingEnabled,
                    onCheckedChange = { viewModel.setPhoneCallStateEnabled(CallState.INCOMING, it) },
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    title = stringResource(R.string.phone_show_photo_title),
                    description = "Show the caller's contact photo or the fallback avatar.",
                    checked = settings.showPhoto,
                    onCheckedChange = { enabled ->
                        viewModel.setPhoneShowPhoto(enabled)
                        if (enabled && !contactsGranted) {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(4.dp),
                    title = stringResource(R.string.phone_show_actions_title),
                    description = "Show Answer and Decline actions on the incoming call.",
                    checked = settings.showActions,
                    onCheckedChange = viewModel::setPhoneShowActions,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(4.dp),
                    title = stringResource(R.string.phone_expanded_incoming_title),
                    description = "Use the taller two-row layout with full-width Answer and Hang up buttons.",
                    checked = settings.expandedIncomingLayout,
                    onCheckedChange = viewModel::setPhoneExpandedIncomingLayout,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp, topStart = 4.dp, topEnd = 4.dp),
                    title = stringResource(R.string.phone_button_labels_title),
                    description = "Show Answer and Hang up text beside their icons.",
                    checked = settings.showButtonLabels,
                    onCheckedChange = viewModel::setPhoneShowButtonLabels,
                )

                SectionLabel("Incoming button colours")
                ColorPickerCard(
                    label = stringResource(R.string.phone_incoming_answer_color_label),
                    selected = settings.incomingAnswerColor,
                    onSelect = { it?.let(viewModel::setPhoneIncomingAnswerColor) },
                )
                ColorPickerCard(
                    label = stringResource(R.string.phone_incoming_hangup_color_label),
                    selected = settings.incomingHangUpColor,
                    onSelect = { it?.let(viewModel::setPhoneIncomingHangUpColor) },
                )
                SectionLabel("Fallback avatar")
                ColorPickerCard(
                    label = stringResource(R.string.tile_icon_container_label),
                    selected = settings.iconContainerColor,
                    onSelect = viewModel::setPhoneIconContainerColor,
                    defaultLabel = stringResource(R.string.music_default_accent),
                    defaultColor = Color(DynamicTile.PHONE.accent),
                )
            }

            PhoneSettingsTab.OUTGOING -> {
                SectionLabel("Outgoing call")
                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = "Enable outgoing calls",
                    description = "Show the island while a call is dialing.",
                    checked = settings.outgoingEnabled,
                    onCheckedChange = { viewModel.setPhoneCallStateEnabled(CallState.OUTGOING, it) },
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    title = stringResource(R.string.phone_show_photo_title),
                    description = "Keep the contact portrait visible while the call is dialing.",
                    checked = settings.showPhoto,
                    onCheckedChange = viewModel::setPhoneShowPhoto,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(4.dp),
                    title = stringResource(R.string.phone_show_actions_title),
                    description = "Show the dialer's cancel or end-call action when it is provided.",
                    checked = settings.showActions,
                    onCheckedChange = viewModel::setPhoneShowActions,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp, topStart = 4.dp, topEnd = 4.dp),
                    title = stringResource(R.string.phone_button_labels_title),
                    description = "Show text labels beside outgoing call actions.",
                    checked = settings.showButtonLabels,
                    onCheckedChange = viewModel::setPhoneShowButtonLabels,
                )
                SectionLabel("Outgoing action colour")
                ColorPickerCard(
                    label = stringResource(R.string.phone_other_color_label),
                    selected = settings.otherButtonColor,
                    onSelect = { it?.let(viewModel::setPhoneOtherButtonColor) },
                )
                ColorPickerCard(
                    label = stringResource(R.string.phone_hangup_color_label),
                    selected = settings.hangUpColor,
                    onSelect = { it?.let(viewModel::setPhoneHangUpColor) },
                )
                Text(
                    text = "The outgoing preview is simulated. Actual outgoing-call detection depends on whether your dialer publishes a call notification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }

            PhoneSettingsTab.ONGOING -> {
                SectionLabel("Ongoing call")
                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = "Enable ongoing calls",
                    description = "Show the island for connected calls and their duration.",
                    checked = settings.ongoingEnabled,
                    onCheckedChange = { viewModel.setPhoneCallStateEnabled(CallState.ONGOING, it) },
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    title = stringResource(R.string.phone_show_photo_title),
                    description = "Show the contact portrait during the call.",
                    checked = settings.showPhoto,
                    onCheckedChange = viewModel::setPhoneShowPhoto,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(4.dp),
                    title = stringResource(R.string.phone_show_duration_title),
                    description = stringResource(R.string.phone_show_duration_desc),
                    checked = settings.showDuration,
                    onCheckedChange = viewModel::setPhoneShowDuration,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(4.dp),
                    title = stringResource(R.string.phone_show_actions_title),
                    description = "Show the in-call controls and Hang up action.",
                    checked = settings.showActions,
                    onCheckedChange = viewModel::setPhoneShowActions,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp, topStart = 4.dp, topEnd = 4.dp),
                    title = "Mini call",
                    description = "Shrink a connected call into a compact pill. Tap it to reopen the call controls.",
                    checked = settings.miniCall,
                    onCheckedChange = viewModel::setPhoneMiniCall,
                )

                SettingsToggleCard(
                    shape = RoundedCornerShape(4.dp),
                    title = "Contact photo background",
                    description = "Use the caller's photo behind the expanded call controls. This never affects the compact or mini pill.",
                    checked = settings.expandedPhotoBackground,
                    onCheckedChange = viewModel::setPhoneExpandedPhotoBackground,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(4.dp),
                    title = "Blur background",
                    description = "Soften the contact photo to keep call controls and text readable.",
                    checked = settings.expandedPhotoBlur,
                    onCheckedChange = viewModel::setPhoneExpandedPhotoBlur,
                )
                if (settings.expandedPhotoBackground && settings.expandedPhotoBlur) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "Blur intensity · ${settings.expandedPhotoBlurRadius.toInt()} dp",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Slider(
                            value = settings.expandedPhotoBlurRadius,
                            onValueChange = viewModel::setPhoneExpandedPhotoBlurRadius,
                            valueRange = 0f..40f,
                        )
                    }
                }
                SectionLabel("In-call button colours")
                ColorPickerCard(
                    label = stringResource(R.string.phone_other_color_label),
                    selected = settings.otherButtonColor,
                    onSelect = { it?.let(viewModel::setPhoneOtherButtonColor) },
                )
                ColorPickerCard(
                    label = stringResource(R.string.phone_expanded_hangup_color_label),
                    selected = settings.expandedHangUpColor,
                    onSelect = { it?.let(viewModel::setPhoneExpandedHangUpColor) },
                )
            }
        }

        SectionLabel("Call detection")
        Text(
            text = stringResource(R.string.phone_call_detection_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (!phoneStateGranted) {
            Button(
                onClick = { phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.phone_enable_call_detection))
            }
        } else {
            Text(
                text = stringResource(R.string.phone_call_detection_enabled),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Text(
            text = stringResource(R.string.phone_tile_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp),
    )
}
