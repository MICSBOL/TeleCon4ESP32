package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetControlPanelCenterModeUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    operator fun invoke(): Flow<ControlPanelCenterMode> = repository.controlPanelCenterModeFlow()
}

class SaveControlPanelCenterModeUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(mode: ControlPanelCenterMode) {
        repository.saveControlPanelCenterMode(mode)
    }
}
