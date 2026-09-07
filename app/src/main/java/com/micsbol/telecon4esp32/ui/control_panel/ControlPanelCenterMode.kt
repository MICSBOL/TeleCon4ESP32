package com.micsbol.telecon4esp32.ui.control_panel

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode

@get:StringRes
val ControlPanelCenterMode.titleRes: Int
    get() = when (this) {
        ControlPanelCenterMode.PLOTS -> R.string.control_panel_center_mode_plots
        ControlPanelCenterMode.STICK -> R.string.control_panel_center_mode_stick
        ControlPanelCenterMode.CAMERA -> R.string.control_panel_center_mode_camera
        ControlPanelCenterMode.RADAR -> R.string.control_panel_center_mode_radar
    }

val ControlPanelCenterMode.icon: ImageVector
    get() = when (this) {
        ControlPanelCenterMode.PLOTS -> Icons.AutoMirrored.Filled.ShowChart
        ControlPanelCenterMode.STICK -> Icons.Filled.FilterCenterFocus
        ControlPanelCenterMode.CAMERA -> Icons.Filled.Videocam
        ControlPanelCenterMode.RADAR -> Icons.Filled.Radar
    }
