package com.micsbol.telecon4esp32.ui.solarsystem

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SolarSystemViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SolarSystemUiState())
    val uiState = _uiState.asStateFlow()
}
