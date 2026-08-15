package com.micsbol.telecon4esp32.ui.applications

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId

@StringRes
fun ApplicationId.titleRes(): Int = when (this) {
    ApplicationId.CONTROL_PANEL -> R.string.app_control_panel_title
    ApplicationId.RC_VEHICLE_PRO -> R.string.app_rc_vehicle_title
    ApplicationId.GREENHOUSE -> R.string.app_greenhouse_title
    ApplicationId.SOLAR_POWER -> R.string.app_solar_title
    ApplicationId.SMART_HOME -> R.string.app_smart_home_title
    ApplicationId.WATER_TANK -> R.string.app_water_tank_title
    ApplicationId.SMART_DOOR_LOCK -> R.string.app_smart_door_lock_title
    ApplicationId.SMART_LIGHTING -> R.string.app_smart_lighting_title
}

@DrawableRes
fun ApplicationId.thumbnailRes(): Int = when (this) {
    ApplicationId.CONTROL_PANEL -> R.drawable.app_holo_control_panel
    ApplicationId.RC_VEHICLE_PRO -> R.drawable.app_holo_rc_vehicle
    ApplicationId.GREENHOUSE -> R.drawable.app_holo_greenhouse
    ApplicationId.SOLAR_POWER -> R.drawable.app_holo_solar
    ApplicationId.SMART_HOME -> R.drawable.app_holo_smart_home
    ApplicationId.WATER_TANK -> R.drawable.app_holo_water_tank
    ApplicationId.SMART_DOOR_LOCK -> R.drawable.app_holo_door_lock
    ApplicationId.SMART_LIGHTING -> R.drawable.app_holo_smart_lighting
}

@StringRes
fun ApplicationId.recentTagRes(): Int = when (this) {
    ApplicationId.CONTROL_PANEL,
    ApplicationId.RC_VEHICLE_PRO -> R.string.cyber_tag_robotics
    ApplicationId.GREENHOUSE -> R.string.cyber_tag_greenhouse
    ApplicationId.SOLAR_POWER -> R.string.cyber_tag_solar
    ApplicationId.SMART_HOME,
    ApplicationId.SMART_DOOR_LOCK,
    ApplicationId.SMART_LIGHTING -> R.string.cyber_tag_smart_home
    ApplicationId.WATER_TANK -> R.string.cyber_tag_water
}
