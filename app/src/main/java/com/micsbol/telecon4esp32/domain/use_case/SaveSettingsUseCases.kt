package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import javax.inject.Inject

data class SaveSettingsUseCases @Inject constructor(
    val saveLeftStickMode: SaveLeftStickModeUseCase,
    val saveRightStickMode: SaveRightStickModeUseCase,
    val saveSwitchState: SaveSwitchStateUseCase,
    val saveLeftKnobValue: SaveLeftKnobValueUseCase,
    val saveRightKnobValue: SaveRightKnobValueUseCase,
    val saveLeftPanelUnit: SaveLeftPanelUnitUseCase,
    val saveRightPanelUnit: SaveRightPanelUnitUseCase,
    val saveAnalogIndicatorUnit: SaveAnalogIndicatorUnitUseCase,
    val saveBatteryLabel: SaveBatteryLabelUseCase,
    val savePlotLabel: SavePlotLabelUseCase,
    val savePlotCalibration: SavePlotCalibrationUseCase,
    val saveChannelRouting: SaveChannelRoutingUseCase,
    val saveLeftPanelOn: SaveLeftPanelOnUseCase,
    val saveRightPanelOn: SaveRightPanelOnUseCase,
    val saveLeftPanelColorGreen: SaveLeftPanelColorGreenUseCase,
    val saveRightPanelColorGreen: SaveRightPanelColorGreenUseCase,
)

class SaveLeftStickModeUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    suspend operator fun invoke(mode: JoystickMode) = repository.saveLeftStickMode(mode)
}

class SaveRightStickModeUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    suspend operator fun invoke(mode: JoystickMode) = repository.saveRightStickMode(mode)
}

class SaveSwitchStateUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    suspend operator fun invoke(index: Int, isOn: Boolean) = repository.saveSwitchState(index, isOn)
}

class SaveLeftKnobValueUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    suspend operator fun invoke(value: Float) = repository.saveLeftKnobValue(value)
}

class SaveRightKnobValueUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    suspend operator fun invoke(value: Float) = repository.saveRightKnobValue(value)
}

class SaveLeftPanelUnitUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(value: String) = repository.saveLeftPanelUnit(value)
}

class SaveRightPanelUnitUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(value: String) = repository.saveRightPanelUnit(value)
}

class SaveAnalogIndicatorUnitUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(value: String) = repository.saveAnalogIndicatorUnit(value)
}

class SaveBatteryLabelUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(value: String) = repository.saveBatteryLabel(value)
}

class SavePlotLabelUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(index: Int, value: String) = repository.savePlotLabel(index, value)
}

class SavePlotCalibrationUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(index: Int, calibration: PlotCalibration) =
        repository.savePlotCalibration(index, calibration)
}

class SaveChannelRoutingUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(routing: ChannelRouting) = repository.saveChannelRouting(routing)
}

class SaveLeftPanelOnUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(isOn: Boolean) = repository.saveLeftPanelOn(isOn)
}

class SaveRightPanelOnUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(isOn: Boolean) = repository.saveRightPanelOn(isOn)
}

class SaveLeftPanelColorGreenUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(isGreen: Boolean) = repository.saveLeftPanelColorGreen(isGreen)
}

class SaveRightPanelColorGreenUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(isGreen: Boolean) = repository.saveRightPanelColorGreen(isGreen)
}
