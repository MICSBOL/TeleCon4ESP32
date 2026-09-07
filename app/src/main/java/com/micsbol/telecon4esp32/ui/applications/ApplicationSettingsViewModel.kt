package com.micsbol.telecon4esp32.ui.applications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.canUseConnectionMode
import com.micsbol.telecon4esp32.domain.model.coerceConnectionModeForBoard
import com.micsbol.telecon4esp32.domain.model.effectiveConnectionMode
import com.micsbol.telecon4esp32.domain.model.effectiveProtocolMode
import com.micsbol.telecon4esp32.domain.model.isConnectionModeAvailable
import com.micsbol.telecon4esp32.domain.model.originalDefaultConnectionMode
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.domain.use_case.ApplyCameraHardwareRoleUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetAdvancedSettingsRevealedUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetControlPanelCenterModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetSoftApPerformancePresetUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUseSoftApCameraUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveWalletUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveAdvancedSettingsRevealedUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApHudProcessingRateUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSoftApPerformancePresetUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveUseSoftApCameraUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    getApplicationTransportType: GetApplicationTransportTypeUseCase,
    getApplicationConnectionMode: GetApplicationConnectionModeUseCase,
    private val saveApplicationConnectionMode: SaveApplicationConnectionModeUseCase,
    getApplicationBoard: GetApplicationBoardUseCase,
    private val saveApplicationBoard: SaveApplicationBoardUseCase,
    getUseSoftApCamera: GetUseSoftApCameraUseCase,
    private val saveUseSoftApCamera: SaveUseSoftApCameraUseCase,
    private val applyCameraHardwareRole: ApplyCameraHardwareRoleUseCase,
    getAdvancedSettingsRevealed: GetAdvancedSettingsRevealedUseCase,
    private val saveAdvancedSettingsRevealed: SaveAdvancedSettingsRevealedUseCase,
    getControlPanelCenterMode: GetControlPanelCenterModeUseCase,
    getSoftApPerformancePreset: GetSoftApPerformancePresetUseCase,
    private val saveSoftApPerformancePreset: SaveSoftApPerformancePresetUseCase,
    getSoftApHudProcessingRate: GetSoftApHudProcessingRateUseCase,
    private val saveSoftApHudProcessingRate: SaveSoftApHudProcessingRateUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
    observeWallet: ObserveWalletUseCase,
) : ViewModel() {

    val applicationId: ApplicationId = savedStateHandle.get<String>("applicationId")
        ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
        ?: ApplicationId.CONTROL_PANEL

    private val storedProtocolMode: StateFlow<BluetoothProtocolMode> =
        getApplicationProtocolMode(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothProtocolMode.defaultFor(applicationId),
            )

    private val storedConnectionMode: StateFlow<BluetoothConnectionMode?> =
        getApplicationConnectionMode(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    private val entitlement: StateFlow<Entitlement> = observeEntitlement()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Entitlement.Free,
        )

    private val wallet: StateFlow<CoinWalletState> = observeWallet()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CoinWalletState.Empty,
        )

    private fun requiresCoinEntry(access: Entitlement): Boolean =
        access.usesCoinEconomy() || BuildConfig.DEBUG

    val protocolMode: StateFlow<BluetoothProtocolMode> = combine(
        storedProtocolMode,
        entitlement,
        wallet,
    ) { stored, access, coinWallet ->
        access.effectiveProtocolMode(
            applicationId = applicationId,
            stored = stored,
            wallet = coinWallet,
            requiresCoinEntry = requiresCoinEntry(access),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothProtocolMode.defaultFor(applicationId),
    )

    val transportType: StateFlow<BluetoothTransportType> =
        getApplicationTransportType(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BluetoothTransportType.CLASSIC,
            )

    val board: StateFlow<Esp32Board> = getApplicationBoard(applicationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Esp32Board.defaultFor(applicationId),
        )

    val useSoftApCamera: StateFlow<Boolean> = getUseSoftApCamera(applicationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /**
     * Settings user-type dropdown. Null means infer from the stored connection mode.
     * Restore always writes [SettingsUserType.NORMAL] so the UI returns to starter Default
     * even when the stored link is still a Simple mode (which also infers Default).
     */
    private val _settingsUserTypeOverride = MutableStateFlow<SettingsUserType?>(null)
    val settingsUserTypeOverride: StateFlow<SettingsUserType?> =
        _settingsUserTypeOverride.asStateFlow()

    fun onSettingsUserTypeSelected(type: SettingsUserType) {
        _settingsUserTypeOverride.value = type
    }

    /**
     * When false, settings look like Advanced is still locked (no user-type menu,
     * lock in the title). Restore persists this so the original Default screen
     * stays after leaving and reopening settings. The Advanced grant is unchanged;
     * [revealAdvancedSettings] shows the menu again.
     */
    private val _advancedSettingsRevealedOverride = MutableStateFlow<Boolean?>(null)
    val advancedSettingsRevealed: StateFlow<Boolean> = combine(
        getAdvancedSettingsRevealed(applicationId),
        _advancedSettingsRevealedOverride,
    ) { stored, override ->
        override ?: stored
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    fun revealAdvancedSettings() {
        _advancedSettingsRevealedOverride.value = true
        viewModelScope.launch {
            saveAdvancedSettingsRevealed(applicationId, true)
        }
    }

    /**
     * Bumped after [resetToDefaultConfiguration] finishes writing storage so the
     * settings form can rebuild against Default / No CAM / Classic Simple.
     */
    private val _configurationResetEpoch = MutableStateFlow(0)
    val configurationResetEpoch: StateFlow<Int> = _configurationResetEpoch.asStateFlow()

    private val centerModeLoaded = MutableStateFlow(false)

    val centerMode: StateFlow<ControlPanelCenterMode> = getControlPanelCenterMode()
        .onEach { centerModeLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ControlPanelCenterMode.PLOTS,
        )

    val isCenterModeLoaded: StateFlow<Boolean> = centerModeLoaded.asStateFlow()

    val softApPerformancePreset: StateFlow<SoftApPerformancePreset> =
        getSoftApPerformancePreset(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SoftApPerformancePreset.DEFAULT,
            )

    val softApHudProcessingRate: StateFlow<SoftApHudProcessingRate> =
        getSoftApHudProcessingRate(applicationId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SoftApHudProcessingRate.DEFAULT,
            )

    val connectionMode: StateFlow<BluetoothConnectionMode> = combine(
        combine(
            transportType,
            storedProtocolMode,
            storedConnectionMode,
            entitlement,
            board,
        ) { transport, storedProtocol, storedMode, access, selectedBoard ->
            ConnectionModeInputs(transport, storedProtocol, storedMode, access, selectedBoard)
        },
        wallet,
    ) { inputs, coinWallet ->
        val coinEntry = requiresCoinEntry(inputs.access)
        val candidate = inputs.storedMode
            ?: inputs.access.effectiveConnectionMode(
                applicationId = applicationId,
                transport = inputs.transport,
                storedProtocol = inputs.storedProtocol,
                board = inputs.selectedBoard,
                wallet = coinWallet,
                requiresCoinEntry = coinEntry,
            )
        inputs.access.coerceConnectionModeForBoard(
            applicationId = applicationId,
            board = inputs.selectedBoard,
            mode = candidate,
            wallet = coinWallet,
            requiresCoinEntry = coinEntry,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BluetoothConnectionMode.from(
            BluetoothTransportType.CLASSIC,
            BluetoothProtocolMode.defaultFor(applicationId),
        ),
    )

    fun onConnectionModeChanged(mode: BluetoothConnectionMode) {
        val access = entitlement.value
        val coinWallet = wallet.value
        val coinEntry = requiresCoinEntry(access)
        if (!access.canUseConnectionMode(applicationId, mode, coinWallet, coinEntry)) return
        if (!applicationId.isConnectionModeAvailable(board.value, mode)) return
        viewModelScope.launch {
            saveApplicationConnectionMode(applicationId, mode)
        }
    }

    fun onBoardChanged(board: Esp32Board) {
        viewModelScope.launch {
            saveApplicationBoard(applicationId, board)
            val access = entitlement.value
            val coinWallet = wallet.value
            val coinEntry = requiresCoinEntry(access)
            val current = storedConnectionMode.value
                ?: access.effectiveConnectionMode(
                    applicationId = applicationId,
                    transport = transportType.value,
                    storedProtocol = storedProtocolMode.value,
                    board = board,
                    wallet = coinWallet,
                    requiresCoinEntry = coinEntry,
                )
            val coerced = access.coerceConnectionModeForBoard(
                applicationId = applicationId,
                board = board,
                mode = current,
                wallet = coinWallet,
                requiresCoinEntry = coinEntry,
            )
            if (coerced != current || storedConnectionMode.value == null) {
                saveApplicationConnectionMode(applicationId, coerced)
            }
        }
    }

    fun onUseSoftApCameraChanged(enabled: Boolean) {
        viewModelScope.launch {
            saveUseSoftApCamera(applicationId, enabled)
        }
    }

    fun onCameraHardwareRoleSelected(role: CameraHardwareRole) {
        viewModelScope.launch {
            applyCameraHardwareRole(applicationId, role)
        }
    }

    fun onSoftApPerformancePresetChanged(preset: SoftApPerformancePreset) {
        viewModelScope.launch {
            saveSoftApPerformancePreset(applicationId, preset)
        }
    }

    fun onSoftApHudProcessingRateChanged(rate: SoftApHudProcessingRate) {
        viewModelScope.launch {
            saveSoftApHudProcessingRate(applicationId, rate)
        }
    }

    fun resetToDefaultConfiguration() {
        _settingsUserTypeOverride.value = SettingsUserType.NORMAL
        _advancedSettingsRevealedOverride.value = false
        viewModelScope.launch {
            saveAdvancedSettingsRevealed(applicationId, false)
            val board = Esp32Board.defaultFor(applicationId)
            saveApplicationBoard(applicationId, board)
            saveUseSoftApCamera(applicationId, false)
            saveApplicationConnectionMode(applicationId, applicationId.originalDefaultConnectionMode())
            saveSoftApPerformancePreset(applicationId, SoftApPerformancePreset.DEFAULT)
            saveSoftApHudProcessingRate(applicationId, SoftApHudProcessingRate.DEFAULT)
            _configurationResetEpoch.update { it + 1 }
        }
    }
}

private data class ConnectionModeInputs(
    val transport: BluetoothTransportType,
    val storedProtocol: BluetoothProtocolMode,
    val storedMode: BluetoothConnectionMode?,
    val access: Entitlement,
    val selectedBoard: Esp32Board,
)
