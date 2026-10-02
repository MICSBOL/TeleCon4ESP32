package com.micsbol.telecon4esp32.ui.codes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.use_case.SaveCodeAssetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ZipExportEvent {
    data object Shared : ZipExportEvent
    data object NoShareApp : ZipExportEvent
    data object Failed : ZipExportEvent
}

/**
 * Saves sketch ZIP packages from module settings (not the docs screen).
 */
@HiltViewModel
class CodePackageExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val saveCodeAsset: SaveCodeAssetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodesUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ZipExportEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun shareZip(asset: CodeAssetInfo) {
        if (_uiState.value.isSavingZip) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingZip = true, saveError = null) }
            val event = try {
                val file = materializeZip(context, asset)
                when (shareZipFile(context, file)) {
                    ZipSharePrepareResult.Success -> ZipExportEvent.Shared
                    ZipSharePrepareResult.NoActivity -> ZipExportEvent.NoShareApp
                    ZipSharePrepareResult.CopyFailed -> ZipExportEvent.Failed
                }
            } catch (_: Exception) {
                ZipExportEvent.Failed
            }
            _uiState.update { it.copy(isSavingZip = false) }
            _events.emit(event)
        }
    }

    fun saveZip(asset: CodeAssetInfo) {
        if (_uiState.value.isSavingZip) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingZip = true, saveError = null) }
            runCatching {
                val file = materializeZip(context, asset)
                saveCodeAsset.saveFile(file, asset.outputFileName)
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
