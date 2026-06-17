package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class GreenhouseViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(GreenhouseUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleFan() {
        _uiState.update { it.copy(fanOn = !it.fanOn) }
    }

    fun toggleHeater() {
        _uiState.update { it.copy(heaterOn = !it.heaterOn) }
    }

    fun togglePump() {
        _uiState.update { it.copy(pumpOn = !it.pumpOn) }
    }
}
