package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import javax.inject.Inject

data class SaveSettingsUseCases @Inject constructor(
    val saveLeftStickMode: SaveLeftStickModeUseCase,
    val saveRightStickMode: SaveRightStickModeUseCase,
    val saveSwitchState: SaveSwitchStateUseCase,
    val saveLeftKnobValue: SaveLeftKnobValueUseCase,
    val saveRightKnobValue: SaveRightKnobValueUseCase
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
