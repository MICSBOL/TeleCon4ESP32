package com.micsbol.telecon4esp32.ui.codes

data class CodesUiState(
    val isSavingZip: Boolean = false,
    val savedZipLocation: String? = null,
    val saveError: String? = null
)
