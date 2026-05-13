package com.example.emitterapp.ui.codes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emitterapp.domain.use_case.SaveCodeAssetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodesViewModel @Inject constructor(
    private val saveCodeAsset: SaveCodeAssetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodesUiState())
    val uiState = _uiState.asStateFlow()

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
                        saveError = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSavingZip = false,
                        saveError = throwable.localizedMessage
                            ?: throwable.message
                            ?: throwable::class.java.simpleName
                    )
                }
            }
        }
    }

    fun dismissSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }
}
