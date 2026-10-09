package com.micsbol.telecon4esp32.ui.feature_tour

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId

enum class ModuleFeatureId {
    CP_STICKS,
    CP_KNOBS,
    CP_PLOTS,
    CP_STICK_PATH,
    CP_CAMERA,
    CP_RADAR,
    CP_SWITCHES,
    CP_BUTTONS,
    CP_ANALOG,
    CP_BATTERY,
    CP_NUMERIC,
    RC_TOP_BAR,
    RC_THROTTLE,
    RC_STEERING,
    RC_CENTER,
    RC_PLOT,
    RC_PAN,
}

data class FeatureTourStep(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val advanceOnPractice: Boolean = false,
)

data class ModuleFeature(
    val id: ModuleFeatureId,
    val applicationId: ApplicationId,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val steps: List<FeatureTourStep>,
)

/** Walks one feature lesson. A practice gesture advances only the step that asks for it. */
class FeatureTourCursor(val stepCount: Int) {
    var index: Int by mutableIntStateOf(0)
        private set
    var finished: Boolean by mutableStateOf(false)
        private set

    val isLast: Boolean
        get() = index >= stepCount - 1

    fun next() {
        if (finished || stepCount == 0) return
        if (isLast) finished = true else index += 1
    }

    fun onPracticed(advances: Boolean) {
        if (advances) next()
    }
}

fun moduleFeatures(applicationId: ApplicationId): List<ModuleFeature> = when (applicationId) {
    ApplicationId.CONTROL_PANEL -> controlPanelFeatures
    ApplicationId.RC_VEHICLE_PRO -> rcVehicleFeatures
    else -> emptyList()
}

fun moduleFeature(applicationId: ApplicationId, featureId: ModuleFeatureId): ModuleFeature? =
    moduleFeatures(applicationId).firstOrNull { it.id == featureId }

private fun step(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    advanceOnPractice: Boolean = false,
) = FeatureTourStep(titleRes, bodyRes, advanceOnPractice)

private val controlPanelFeatures = listOf(
    ModuleFeature(
        id = ModuleFeatureId.CP_STICKS,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_sticks_title,
        subtitleRes = R.string.feature_cp_sticks_subtitle,
        icon = Icons.Filled.SportsEsports,
        steps = listOf(
            step(R.string.feature_cp_sticks_use_title, R.string.feature_cp_sticks_use_body),
            step(
                R.string.feature_cp_sticks_try_title,
                R.string.feature_cp_sticks_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_cp_sticks_config_title, R.string.feature_cp_sticks_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_KNOBS,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_knobs_title,
        subtitleRes = R.string.feature_cp_knobs_subtitle,
        icon = Icons.Filled.Tune,
        steps = listOf(
            step(R.string.feature_cp_knobs_use_title, R.string.feature_cp_knobs_use_body),
            step(
                R.string.feature_cp_knobs_try_title,
                R.string.feature_cp_knobs_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_cp_knobs_config_title, R.string.feature_cp_knobs_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_PLOTS,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_plots_title,
        subtitleRes = R.string.feature_cp_plots_subtitle,
        icon = Icons.AutoMirrored.Filled.ShowChart,
        steps = listOf(
            step(R.string.feature_cp_plots_use_title, R.string.feature_cp_plots_use_body),
            step(R.string.feature_cp_plots_try_title, R.string.feature_cp_plots_try_body),
            step(R.string.feature_cp_plots_config_title, R.string.feature_cp_plots_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_STICK_PATH,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_stick_path_title,
        subtitleRes = R.string.feature_cp_stick_path_subtitle,
        icon = Icons.Filled.FilterCenterFocus,
        steps = listOf(
            step(R.string.feature_cp_stick_path_use_title, R.string.feature_cp_stick_path_use_body),
            step(R.string.feature_cp_stick_path_try_title, R.string.feature_cp_stick_path_try_body),
            step(R.string.feature_cp_stick_path_config_title, R.string.feature_cp_stick_path_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_CAMERA,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_camera_title,
        subtitleRes = R.string.feature_cp_camera_subtitle,
        icon = Icons.Filled.Videocam,
        steps = listOf(
            step(R.string.feature_cp_camera_use_title, R.string.feature_cp_camera_use_body),
            step(R.string.feature_cp_camera_try_title, R.string.feature_cp_camera_try_body),
            step(R.string.feature_cp_camera_config_title, R.string.feature_cp_camera_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_RADAR,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_radar_title,
        subtitleRes = R.string.feature_cp_radar_subtitle,
        icon = Icons.Filled.Radar,
        steps = listOf(
            step(R.string.feature_cp_radar_use_title, R.string.feature_cp_radar_use_body),
            step(R.string.feature_cp_radar_try_title, R.string.feature_cp_radar_try_body),
            step(R.string.feature_cp_radar_config_title, R.string.feature_cp_radar_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_SWITCHES,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_switches_title,
        subtitleRes = R.string.feature_cp_switches_subtitle,
        icon = Icons.Filled.ToggleOn,
        steps = listOf(
            step(R.string.feature_cp_switches_use_title, R.string.feature_cp_switches_use_body),
            step(
                R.string.feature_cp_switches_try_title,
                R.string.feature_cp_switches_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_cp_switches_config_title, R.string.feature_cp_switches_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_BUTTONS,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_buttons_title,
        subtitleRes = R.string.feature_cp_buttons_subtitle,
        icon = Icons.Filled.TouchApp,
        steps = listOf(
            step(R.string.feature_cp_buttons_use_title, R.string.feature_cp_buttons_use_body),
            step(
                R.string.feature_cp_buttons_try_title,
                R.string.feature_cp_buttons_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_cp_buttons_config_title, R.string.feature_cp_buttons_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_ANALOG,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_analog_title,
        subtitleRes = R.string.feature_cp_analog_subtitle,
        icon = Icons.Filled.Speed,
        steps = listOf(
            step(R.string.feature_cp_analog_use_title, R.string.feature_cp_analog_use_body),
            step(
                R.string.feature_cp_analog_try_title,
                R.string.feature_cp_analog_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_cp_analog_config_title, R.string.feature_cp_analog_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_BATTERY,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_battery_title,
        subtitleRes = R.string.feature_cp_battery_subtitle,
        icon = Icons.Filled.BatteryChargingFull,
        steps = listOf(
            step(R.string.feature_cp_battery_use_title, R.string.feature_cp_battery_use_body),
            step(
                R.string.feature_cp_battery_try_title,
                R.string.feature_cp_battery_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_cp_battery_config_title, R.string.feature_cp_battery_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.CP_NUMERIC,
        applicationId = ApplicationId.CONTROL_PANEL,
        titleRes = R.string.feature_cp_numeric_title,
        subtitleRes = R.string.feature_cp_numeric_subtitle,
        icon = Icons.Filled.Pin,
        steps = listOf(
            step(R.string.feature_cp_numeric_use_title, R.string.feature_cp_numeric_use_body),
            step(
                R.string.feature_cp_numeric_try_title,
                R.string.feature_cp_numeric_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_cp_numeric_config_title, R.string.feature_cp_numeric_config_body),
        ),
    ),
)

private val rcVehicleFeatures = listOf(
    ModuleFeature(
        id = ModuleFeatureId.RC_TOP_BAR,
        applicationId = ApplicationId.RC_VEHICLE_PRO,
        titleRes = R.string.feature_rc_top_title,
        subtitleRes = R.string.feature_rc_top_subtitle,
        icon = Icons.Filled.Speed,
        steps = listOf(
            step(R.string.feature_rc_top_use_title, R.string.feature_rc_top_use_body),
            step(R.string.feature_rc_top_try_title, R.string.feature_rc_top_try_body),
            step(R.string.feature_rc_top_config_title, R.string.feature_rc_top_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.RC_THROTTLE,
        applicationId = ApplicationId.RC_VEHICLE_PRO,
        titleRes = R.string.feature_rc_throttle_title,
        subtitleRes = R.string.feature_rc_throttle_subtitle,
        icon = Icons.Filled.SwapVert,
        steps = listOf(
            step(R.string.feature_rc_throttle_use_title, R.string.feature_rc_throttle_use_body),
            step(
                R.string.feature_rc_throttle_try_title,
                R.string.feature_rc_throttle_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_rc_throttle_config_title, R.string.feature_rc_throttle_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.RC_STEERING,
        applicationId = ApplicationId.RC_VEHICLE_PRO,
        titleRes = R.string.feature_rc_steering_title,
        subtitleRes = R.string.feature_rc_steering_subtitle,
        icon = Icons.Filled.SwapHoriz,
        steps = listOf(
            step(R.string.feature_rc_steering_use_title, R.string.feature_rc_steering_use_body),
            step(
                R.string.feature_rc_steering_try_title,
                R.string.feature_rc_steering_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_rc_steering_config_title, R.string.feature_rc_steering_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.RC_CENTER,
        applicationId = ApplicationId.RC_VEHICLE_PRO,
        titleRes = R.string.feature_rc_actions_title,
        subtitleRes = R.string.feature_rc_actions_subtitle,
        icon = Icons.Filled.CameraAlt,
        steps = listOf(
            step(R.string.feature_rc_actions_use_title, R.string.feature_rc_actions_use_body),
            step(
                R.string.feature_rc_actions_try_title,
                R.string.feature_rc_actions_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_rc_actions_config_title, R.string.feature_rc_actions_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.RC_PLOT,
        applicationId = ApplicationId.RC_VEHICLE_PRO,
        titleRes = R.string.feature_rc_plot_title,
        subtitleRes = R.string.feature_rc_plot_subtitle,
        icon = Icons.AutoMirrored.Filled.ShowChart,
        steps = listOf(
            step(R.string.feature_rc_plot_use_title, R.string.feature_rc_plot_use_body),
            step(R.string.feature_rc_plot_try_title, R.string.feature_rc_plot_try_body),
            step(R.string.feature_rc_plot_config_title, R.string.feature_rc_plot_config_body),
        ),
    ),
    ModuleFeature(
        id = ModuleFeatureId.RC_PAN,
        applicationId = ApplicationId.RC_VEHICLE_PRO,
        titleRes = R.string.feature_rc_pan_title,
        subtitleRes = R.string.feature_rc_pan_subtitle,
        icon = Icons.Filled.CenterFocusStrong,
        steps = listOf(
            step(R.string.feature_rc_pan_use_title, R.string.feature_rc_pan_use_body),
            step(
                R.string.feature_rc_pan_try_title,
                R.string.feature_rc_pan_try_body,
                advanceOnPractice = true,
            ),
            step(R.string.feature_rc_pan_config_title, R.string.feature_rc_pan_config_body),
        ),
    ),
)
