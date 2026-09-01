package com.micsbol.telecon4esp32.ui.codes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.availableConnectionModes
import com.micsbol.telecon4esp32.domain.model.coerceConnectionModeForBoard
import com.micsbol.telecon4esp32.domain.model.effectiveConnectionMode
import com.micsbol.telecon4esp32.domain.model.isConnectionModeAvailable
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationTransportTypeUseCase
import com.micsbol.telecon4esp32.domain.use_case.GetUseSoftApCameraUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveEntitlementUseCase
import com.micsbol.telecon4esp32.domain.use_case.ObserveWalletUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveCodeAssetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CodesFilterState(
    val board: Esp32Board = Esp32Board.DEV_KIT,
    val mode: BluetoothConnectionMode = BluetoothConnectionMode.CLASSIC_SIMPLE,
    val useSoftApCamera: Boolean = false,
    val hydratedFromSettings: Boolean = false,
)

@HiltViewModel
class CodesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saveCodeAsset: SaveCodeAssetUseCase,
    private val getApplicationBoard: GetApplicationBoardUseCase,
    private val getApplicationTransportType: GetApplicationTransportTypeUseCase,
    private val getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    private val getUseSoftApCamera: GetUseSoftApCameraUseCase,
    observeEntitlement: ObserveEntitlementUseCase,
    observeWallet: ObserveWalletUseCase,
) : ViewModel() {

    val applicationId: ApplicationId = savedStateHandle.get<String>("applicationId")
        ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
        ?: ApplicationId.CONTROL_PANEL

    private val _uiState = MutableStateFlow(CodesUiState())
    val uiState = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(CodesFilterState())
    val filter: StateFlow<CodesFilterState> = _filter.asStateFlow()

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

    init {
        viewModelScope.launch {
            val board = getApplicationBoard(applicationId).first()
            val transport = getApplicationTransportType(applicationId).first()
            val protocol = getApplicationProtocolMode(applicationId).first()
            val overlay = getUseSoftApCamera(applicationId).first()
            val access = entitlement.value
            val coinWallet = wallet.value
            val coinEntry = requiresCoinEntry(access)
            val effective = access.effectiveConnectionMode(
                applicationId = applicationId,
                transport = transport,
                storedProtocol = protocol,
                board = board,
                wallet = coinWallet,
                requiresCoinEntry = coinEntry,
            )
            val coerced = access.coerceConnectionModeForBoard(
                applicationId = applicationId,
                board = board,
                mode = effective,
                wallet = coinWallet,
                requiresCoinEntry = coinEntry,
            )
            _filter.value = CodesFilterState(
                board = board,
                mode = coerced,
                useSoftApCamera = overlay,
                hydratedFromSettings = true,
            )
        }
    }

    fun onBoardSelected(board: Esp32Board) {
        val access = entitlement.value
        val coerced = access.coerceConnectionModeForBoard(
            applicationId = applicationId,
            board = board,
            mode = _filter.value.mode,
            wallet = wallet.value,
            requiresCoinEntry = requiresCoinEntry(access),
        )
        _filter.update {
            it.copy(board = board, mode = coerced)
        }
    }

    fun onUseSoftApCameraSelected(enabled: Boolean) {
        _filter.update { it.copy(useSoftApCamera = enabled) }
    }

    fun onModeSelected(mode: BluetoothConnectionMode) {
        if (!applicationId.isConnectionModeAvailable(_filter.value.board, mode)) return
        _filter.update { it.copy(mode = mode) }
    }

    fun availableModesForSelectedBoard(): List<BluetoothConnectionMode> =
        applicationId.availableConnectionModes(_filter.value.board)

    /**
     * Copies the ZIP from assets into public Downloads (or app storage on older Android versions).
     * For sharing via email or messaging, the UI uses a separate send flow.
     */
    fun saveZipAsset(assetFileName: String, outputFileName: String) {
        if (_uiState.value.isSavingZip) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSavingZip = true, saveError = null)
            }

            runCatching {
                saveCodeAsset(assetFileName, outputFileName)
            }.onSuccess { savedAsset ->
                _uiState.update {
                    it.copy(
                        isSavingZip = false,
                        savedZipLocation = savedAsset.location,
                        saveError = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSavingZip = false,
                        saveError = throwable.localizedMessage
                            ?: throwable.message
                            ?: throwable::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun dismissSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }
}
