package com.micsbol.telecon4esp32.ui.smarthome

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SmartHomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState = _uiState.asStateFlow()
}
