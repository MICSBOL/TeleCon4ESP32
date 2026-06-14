package com.micsbol.telecon4esp32.domain.use_case
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
class GetLastDeviceUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    operator fun invoke(): Flow<Pair<String, String?>?> = repository.lastDeviceFlow
}
