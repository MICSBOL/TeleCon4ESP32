package com.micsbol.telecon4esp32.ui.control_panel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlPanelPlotDisplayViewModel @Inject constructor(
    private val settingsRepository: ISettingsRepository,
) : ViewModel() {

    private val _settings = MutableStateFlow(ControlPanelPlotDisplaySettings.DEFAULT)
    val settings: StateFlow<ControlPanelPlotDisplaySettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = ControlPanelPlotDisplaySettings.decode(
                settingsRepository.controlPanelPlotDisplayFlow().first(),
            )
        }
    }

    fun save(next: ControlPanelPlotDisplaySettings) {
        _settings.value = next
        viewModelScope.launch {
            settingsRepository.saveControlPanelPlotDisplay(next.encode())
        }
    }
}
