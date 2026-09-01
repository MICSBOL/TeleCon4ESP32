package com.micsbol.telecon4esp32.data.camera

import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies [SoftApPerformancePreset] to firmware `/camconfig` once per preset change per app session.
 * Android Phase 1 limits still apply when firmware returns 404.
 */
@Singleton
class SoftApCamConfigApplier @Inject constructor(
    private val softApCamConfigClient: SoftApCamConfigClient,
) {
    private val lastAppliedPreset = ConcurrentHashMap<ApplicationId, SoftApPerformancePreset>()

    suspend fun applyIfNeeded(
        applicationId: ApplicationId,
        preset: SoftApPerformancePreset,
        baseUrl: String = Esp32CameraDefaults.DEFAULT_BASE_URL,
        onStreamRestartRequired: suspend () -> Unit = {},
    ): SoftApCamConfigResult {
        if (lastAppliedPreset[applicationId] == preset) {
            return SoftApCamConfigResult.Applied
        }
        val previous = lastAppliedPreset[applicationId]
        val result = softApCamConfigClient.apply(preset, baseUrl)
        when (result) {
            SoftApCamConfigResult.Applied -> {
                lastAppliedPreset[applicationId] = preset
                if (preset.mayChangeFramesize || previous?.mayChangeFramesize == true) {
                    onStreamRestartRequired()
                }
            }
            SoftApCamConfigResult.Unsupported,
            SoftApCamConfigResult.Unreachable,
            -> lastAppliedPreset[applicationId] = preset
        }
        return result
    }

    fun clearSession(applicationId: ApplicationId) {
        lastAppliedPreset.remove(applicationId)
    }
}
