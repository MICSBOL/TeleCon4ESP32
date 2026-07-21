package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
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

class GetApplicationTransportTypeUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    operator fun invoke(applicationId: ApplicationId): Flow<BluetoothTransportType> =
        repository.transportTypeFlow(applicationId)
}

class SaveApplicationTransportTypeUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(applicationId: ApplicationId, transport: BluetoothTransportType) {
        repository.saveTransportType(applicationId, transport)
    }
}

class GetApplicationBoardUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    operator fun invoke(applicationId: ApplicationId): Flow<Esp32Board> =
        repository.boardFlow(applicationId)
}

class SaveApplicationBoardUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(applicationId: ApplicationId, board: Esp32Board) {
        repository.saveBoard(applicationId, board)
    }
}
