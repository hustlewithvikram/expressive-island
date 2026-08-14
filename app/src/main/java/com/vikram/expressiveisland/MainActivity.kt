package com.vikram.expressiveisland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vikram.expressiveisland.permissions.Permissions
import com.vikram.expressiveisland.service.CutoutNotificationListenerService
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.MainScreen
import com.vikram.expressiveisland.ui.theme.ExpressiveIslandTheme
import com.vikram.expressiveisland.ui.theme.isDark

/** Single-activity host. The overlay itself runs independently in the services. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val darkTheme = theme.isDark()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !darkTheme
            }
            ExpressiveIslandTheme(appTheme = theme) {
                MainScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (Permissions.isNotificationAccessGranted(this) &&
            !CutoutNotificationListenerService.Companion.bound.value
        ) {
            CutoutNotificationListenerService.Companion.requestRebind(this)
        }
    }
}
