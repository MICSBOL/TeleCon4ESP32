package com.micsbol.telecon4esp32.ui.applications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.use_case.GetApplicationProtocolModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationProtocolModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getApplicationProtocolMode: GetApplicationProtocolModeUseCase,
    private val saveApplicationProtocolMode: SaveApplicationProtocolModeUseCase,
) : ViewModel() {

    val applicationId: ApplicationId = savedStateHandle.get<String>("applicationId")
        ?.let { runCatching { ApplicationId.valueOf(it) }.getOrNull() }
        ?: ApplicationId.CONTROL_PANEL

    val protocolMode: StateFlow<BluetoothProtocolMode> = getApplicationProtocolMode(applicationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BluetoothProtocolMode.defaultFor(applicationId),
        )

    fun onProtocolModeChanged(mode: BluetoothProtocolMode) {
        viewModelScope.launch {
            saveApplicationProtocolMode(applicationId, mode)
        }
    }
}
