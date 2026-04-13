package com.example.emitterapp.domain.use_case
import com.example.emitterapp.domain.repository.ISettingsRepository
import javax.inject.Inject
class SaveLastDeviceUseCase @Inject constructor(
    private val repository: ISettingsRepository
) {
    suspend operator fun invoke(address: String, name: String?) {
        repository.saveLastDevice(address, name)
    }
}
