package com.micsbol.emitterapp.ui.codes

data class CodesUiState(
    val isSavingZip: Boolean = false,
    val savedZipLocation: String? = null,
    val saveError: String? = null
)
