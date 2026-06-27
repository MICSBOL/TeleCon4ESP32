package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetApplicationProtocolModeUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    operator fun invoke(applicationId: ApplicationId): Flow<BluetoothProtocolMode> =
        repository.protocolModeFlow(applicationId)
}

class SaveApplicationProtocolModeUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(applicationId: ApplicationId, mode: BluetoothProtocolMode) {
        repository.saveProtocolMode(applicationId, mode)
    }
}
