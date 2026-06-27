package com.micsbol.telecon4esp32.ui.rc_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.use_case.GetUserSettingsUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveSettingsUseCases
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getUserSettings: GetUserSettingsUseCase,
    private val saveSettings: SaveSettingsUseCases
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        getUserSettings()
            .map<UserSettings, SettingsUiState> { settings ->
                SettingsUiState.Success(settings)
            }
            .catch { throwable ->
                emit(SettingsUiState.Error(throwable.message ?: "An unexpected error occurred"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsUiState.Loading
            )

    private val _labelDraft = MutableStateFlow(DisplayLabelDraft())
    val labelDraft: StateFlow<DisplayLabelDraft> = _labelDraft.asStateFlow()

    val hasUnsavedLabelChanges: StateFlow<Boolean> = combine(uiState, labelDraft) { state, draft ->
        state is SettingsUiState.Success && draft != state.settings.toDisplayLabelDraft()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    fun syncLabelDraftFrom(settings: UserSettings) {
        _labelDraft.value = settings.toDisplayLabelDraft()
    }

    fun onLeftPanelUnitDraftChanged(value: String) {
        _labelDraft.update { it.copy(leftPanelUnit = value) }
    }

    fun onRightPanelUnitDraftChanged(value: String) {
        _labelDraft.update { it.copy(rightPanelUnit = value) }
    }

    fun onAnalogIndicatorUnitDraftChanged(value: String) {
        _labelDraft.update { it.copy(analogIndicatorUnit = value) }
    }

    fun onBatteryLabelDraftChanged(value: String) {
        _labelDraft.update { it.copy(batteryLabel = value) }
    }

    fun onPlotLabelDraftChanged(index: Int, value: String) {
        if (index !in 0 until UserSettings.PLOT_LABEL_COUNT) return
        _labelDraft.update { draft ->
            val updated = draft.plotLabels.toMutableList()
            updated[index] = value
            draft.copy(plotLabels = updated)
        }
    }

    fun applyDisplayLabels(onApplied: (DisplayLabelDraft) -> Unit = {}) {
        viewModelScope.launch {
            val draft = _labelDraft.value
            saveSettings.saveLeftPanelUnit(draft.leftPanelUnit)
            saveSettings.saveRightPanelUnit(draft.rightPanelUnit)
            saveSettings.saveAnalogIndicatorUnit(draft.analogIndicatorUnit)
            saveSettings.saveBatteryLabel(draft.batteryLabel)
            draft.plotLabels.forEachIndexed { index, label ->
                saveSettings.savePlotLabel(index, label)
            }
            onApplied(draft)
        }
    }

    fun onLeftStickModeChanged(newMode: JoystickMode) {
        viewModelScope.launch {
            saveSettings.saveLeftStickMode(newMode)
        }
    }

    fun onRightStickModeChanged(newMode: JoystickMode) {
        viewModelScope.launch {
            saveSettings.saveRightStickMode(newMode)
        }
    }

    fun onSwitchInitialStateChange(index: Int, isOn: Boolean){
        viewModelScope.launch {
            saveSettings.saveSwitchState(index, isOn)
        }
    }

    fun onLeftKnobInitialValueChange(value: Float){
        viewModelScope.launch {
            saveSettings.saveLeftKnobValue(value)
        }
    }

    fun onRightKnobInitialValueChange(value: Float){
        viewModelScope.launch {
            saveSettings.saveRightKnobValue(value)
        }
    }
}
