package com.micsbol.telecon4esp32.domain.use_case

import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.domain.model.coerceConnectionModeForBoard
import com.micsbol.telecon4esp32.domain.model.effectiveConnectionMode
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.settingsUserType
import com.micsbol.telecon4esp32.domain.model.coerceForApplication
import com.micsbol.telecon4esp32.domain.model.toSelection
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ApplyCameraHardwareRoleUseCase @Inject constructor(
    private val saveApplicationBoard: SaveApplicationBoardUseCase,
    private val saveUseSoftApCamera: SaveUseSoftApCameraUseCase,
    private val getApplicationConnectionMode: GetApplicationConnectionModeUseCase,
    private val saveApplicationConnectionMode: SaveApplicationConnectionModeUseCase,
    private val getApplicationTransportType: GetApplicationTransportTypeUseCase,
    private val getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    private val observeEntitlement: ObserveEntitlementUseCase,
    private val observeWallet: ObserveWalletUseCase,
) {
    suspend operator fun invoke(applicationId: ApplicationId, role: CameraHardwareRole) {
        val resolved = role.coerceForApplication(applicationId)
        val selection = resolved.toSelection()
        saveApplicationBoard(applicationId, selection.board)
        saveUseSoftApCamera(applicationId, selection.useSoftApCamera)
        val access = observeEntitlement().value
        val coinWallet = observeWallet().value
        val coinEntry = access.usesCoinEconomy() || BuildConfig.DEBUG
        val storedMode = getApplicationConnectionMode(applicationId).first()
        val current = storedMode
            ?: access.effectiveConnectionMode(
                applicationId = applicationId,
                transport = getApplicationTransportType(applicationId).first(),
                storedProtocol = getApplicationProtocolMode(applicationId).first(),
                board = selection.board,
                wallet = coinWallet,
                requiresCoinEntry = coinEntry,
            )
        val family = when {
            selection.bluetoothControlOnly -> ConnectionLinkFamily.BLUETOOTH
            resolved == CameraHardwareRole.ONE_CAM -> ConnectionLinkFamily.WIFI
            else -> current.linkFamily
        }
        val canUseAdvanced = access.canUseAdvancedProtocol(
            applicationId,
            coinWallet,
            coinEntry,
        )
        val preferred = applicationId.preferredConnectionMode(
            board = selection.board,
            family = family,
            userType = current.settingsUserType,
            canUseAdvanced = canUseAdvanced,
        )
        val coerced = access.coerceConnectionModeForBoard(
            applicationId = applicationId,
            board = selection.board,
            mode = preferred ?: current,
            wallet = coinWallet,
            requiresCoinEntry = coinEntry,
        )
        val next = if (selection.bluetoothControlOnly && coerced.isWifiLink) {
            applicationId.preferredConnectionMode(
                board = selection.board,
                family = ConnectionLinkFamily.BLUETOOTH,
                userType = current.settingsUserType,
                canUseAdvanced = canUseAdvanced,
            ) ?: BluetoothConnectionMode.CLASSIC_SIMPLE
        } else {
            coerced
        }
        if (next != current || storedMode == null) {
            saveApplicationConnectionMode(applicationId, next)
        }
    }
}
