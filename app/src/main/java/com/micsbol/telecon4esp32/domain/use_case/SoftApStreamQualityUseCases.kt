package com.micsbol.telecon4esp32.domain.use_case

import android.content.Context
import com.micsbol.telecon4esp32.data.camera.SoftApCamConfigApplier
import com.micsbol.telecon4esp32.data.camera.SoftApCamConfigResult
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.camera.isCameraStreamAtRisk
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EnsureSoftApStreamQualityDefaultsUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(applicationId: ApplicationId) {
        if (!appContext.isCameraStreamAtRisk()) return
        repository.ensureSoftApStreamQualityDefaultsForAtRiskDevice(applicationId)
    }
}

class ApplySoftApCamConfigUseCase @Inject constructor(
    private val applier: SoftApCamConfigApplier,
) {
    suspend operator fun invoke(
        applicationId: ApplicationId,
        preset: SoftApPerformancePreset,
        onStreamRestartRequired: suspend () -> Unit = {},
    ): SoftApCamConfigResult = applier.applyIfNeeded(
        applicationId = applicationId,
        preset = preset,
        onStreamRestartRequired = onStreamRestartRequired,
    )

    fun clearSession(applicationId: ApplicationId) {
        applier.clearSession(applicationId)
    }
}
