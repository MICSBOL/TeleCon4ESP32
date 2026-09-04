package com.micsbol.telecon4esp32.ui.applications

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId

@StringRes
fun ApplicationId.titleRes(): Int = when (this) {
    ApplicationId.CONTROL_PANEL -> R.string.app_control_panel_title
    ApplicationId.RC_VEHICLE_PRO -> R.string.app_rc_vehicle_title
}

@DrawableRes
fun ApplicationId.thumbnailRes(): Int = when (this) {
    ApplicationId.CONTROL_PANEL -> R.drawable.app_holo_control_panel
    ApplicationId.RC_VEHICLE_PRO -> R.drawable.app_holo_rc_vehicle
}

@StringRes
fun ApplicationId.recentTagRes(): Int = when (this) {
    ApplicationId.CONTROL_PANEL,
    ApplicationId.RC_VEHICLE_PRO -> R.string.cyber_tag_robotics
}

/** RC Vehicle HUD screens hide system bars and use cyan chrome. */
fun ApplicationId.usesImmersiveHudChrome(): Boolean =
    this == ApplicationId.RC_VEHICLE_PRO
