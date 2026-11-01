package com.vikram.expressiveisland.data

import androidx.annotation.StringRes
import com.vikram.expressiveisland.R

/**
 * Action performed by tapping the physical camera-hole hit target while no island event or
 * Empty Island pill is visible. Kept separate from [CenterShortcut] so its settings and actions
 * never change the Empty Island shortcut grid.
 */
enum class CameraHoleTapAction(@param:StringRes val labelRes: Int) {
    NONE(R.string.camera_hole_action_none),
    SCREENSHOT(R.string.center_action_screenshot),
    LOCK_SCREEN(R.string.center_action_lock),
    POWER_MENU(R.string.center_action_power),
    NOTIFICATIONS(R.string.center_action_notifications),
    QUICK_SETTINGS(R.string.center_action_quick_settings),
    RECENTS(R.string.center_action_recents),
    WIFI(R.string.center_action_wifi_shizuku),
    BLUETOOTH(R.string.center_action_bluetooth_shizuku),
    MOBILE_DATA(R.string.center_action_mobile_data_shizuku),
    FLASHLIGHT(R.string.center_action_torch),
}

/** Shizuku-only radio toggles, deliberately excluded from Empty Island shortcuts. */
enum class RadioToggle { WIFI, BLUETOOTH, MOBILE_DATA }
