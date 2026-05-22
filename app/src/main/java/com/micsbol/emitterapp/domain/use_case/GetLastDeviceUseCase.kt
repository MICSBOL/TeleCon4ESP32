package com.micsbol.emitterapp.domain.use_case
import com.micsbol.emitterapp.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
class GetLastDeviceUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    operator fun invoke(): Flow<Pair<String, String?>?> = repository.lastDeviceFlow
}
