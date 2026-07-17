package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLastApplicationUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    operator fun invoke(): Flow<ApplicationId?> = repository.lastApplicationFlow
}

class SaveLastApplicationUseCase @Inject constructor(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(applicationId: ApplicationId) {
        repository.saveLastApplication(applicationId)
    }
}
