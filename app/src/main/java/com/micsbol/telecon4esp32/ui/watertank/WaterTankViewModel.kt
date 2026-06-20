package com.micsbol.telecon4esp32.ui.watertank

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WaterTankViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WaterTankUiState())
    val uiState = _uiState.asStateFlow()

    fun selectChartPeriod(period: TankChartPeriod) {
        _uiState.update {
            it.copy(
                chartPeriod = period,
                chartData = TankLevelChartData.mock(period),
            )
        }
    }

    fun togglePump() {
        _uiState.update { it.copy(pumpOn = !it.pumpOn) }
    }
}
