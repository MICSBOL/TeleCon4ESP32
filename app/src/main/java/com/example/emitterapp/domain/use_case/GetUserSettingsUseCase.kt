package com.example.emitterapp.domain.use_case

import com.example.emitterapp.data.repository.SettingsRepository
import com.example.emitterapp.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> {
        return repository.settingsFlow
    }
}
