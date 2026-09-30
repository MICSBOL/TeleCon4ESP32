package com.micsbol.telecon4esp32.ui.home

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.originalDefaultConnectionMode
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationBoardUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveApplicationConnectionModeUseCase
import com.micsbol.telecon4esp32.domain.use_case.SaveUseSoftApCameraUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How touches outside the explanation card are handled. */
enum class TutorialTouchPolicy {
    /** Block the screen. Next is the way forward. */
    Block,

    /** Only the clear spotlight areas receive touches. */
    HolesOnly,

    /** The screen stays usable except for the explanation card. */
    All,
}

enum class FirstConnectionTutorialStep(
    @StringRes val stageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val icon: ImageVector,
    val anchors: List<TutorialAnchor>,
    val touchPolicy: TutorialTouchPolicy,
    /** False when this step advances by the user's own tap or gesture. */
    val showsNextButton: Boolean,
    @StringRes val actionRes: Int = R.string.first_connection_tutorial_next,
) {
    OPEN_CONTROL_PANEL(
        stageRes = R.string.first_connection_tutorial_stage_home,
        titleRes = R.string.first_connection_tutorial_open_panel_title,
        bodyRes = R.string.first_connection_tutorial_open_panel_body,
        icon = Icons.Filled.GridView,
        anchors = listOf(TutorialAnchor.HOME_CONTROL_PANEL),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    OPEN_SETTINGS(
        stageRes = R.string.first_connection_tutorial_stage_panel,
        titleRes = R.string.first_connection_tutorial_open_settings_title,
        bodyRes = R.string.first_connection_tutorial_open_settings_body,
        icon = Icons.Filled.Cable,
        anchors = listOf(TutorialAnchor.PANEL_CABLE),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    CONFIRM_CLASSIC_SIMPLE(
        stageRes = R.string.first_connection_tutorial_stage_settings,
        titleRes = R.string.first_connection_tutorial_confirm_title,
        bodyRes = R.string.first_connection_tutorial_confirm_body,
        icon = Icons.Filled.Download,
        anchors = listOf(
            TutorialAnchor.SETTINGS_CONNECTION,
            TutorialAnchor.SETTINGS_MATCHING_CODE,
        ),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    SERIAL_MONITOR(
        stageRes = R.string.first_connection_tutorial_stage_serial,
        titleRes = R.string.first_connection_tutorial_serial_title,
        bodyRes = R.string.first_connection_tutorial_serial_body,
        icon = Icons.Filled.Terminal,
        anchors = emptyList(),
        touchPolicy = TutorialTouchPolicy.Block,
        showsNextButton = true,
        actionRes = R.string.first_connection_tutorial_continue,
    ),
    OPEN_BLUETOOTH(
        stageRes = R.string.first_connection_tutorial_stage_panel,
        titleRes = R.string.first_connection_tutorial_open_bluetooth_title,
        bodyRes = R.string.first_connection_tutorial_open_bluetooth_body,
        icon = Icons.Filled.Bluetooth,
        anchors = listOf(TutorialAnchor.PANEL_BLUETOOTH),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    SCAN_AND_CONNECT(
        stageRes = R.string.first_connection_tutorial_stage_bluetooth,
        titleRes = R.string.first_connection_tutorial_scan_title,
        bodyRes = R.string.first_connection_tutorial_scan_body,
        icon = Icons.Filled.BluetoothSearching,
        anchors = listOf(TutorialAnchor.BLUETOOTH_REFRESH, TutorialAnchor.BLUETOOTH_LIST),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    MOVE_STICK(
        stageRes = R.string.first_connection_tutorial_stage_panel,
        titleRes = R.string.first_connection_tutorial_stick_title,
        bodyRes = R.string.first_connection_tutorial_stick_body,
        icon = Icons.Filled.SportsEsports,
        anchors = listOf(TutorialAnchor.PANEL_LEFT_STICK),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    STICK_SETUP(
        stageRes = R.string.first_connection_tutorial_stage_panel,
        titleRes = R.string.first_connection_tutorial_stick_setup_title,
        bodyRes = R.string.first_connection_tutorial_stick_setup_body,
        icon = Icons.Filled.Tune,
        anchors = listOf(TutorialAnchor.PANEL_LEFT_STICK, TutorialAnchor.PANEL_RIGHT_STICK),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    OTHER_CONTROLS(
        stageRes = R.string.first_connection_tutorial_stage_panel,
        titleRes = R.string.first_connection_tutorial_other_controls_title,
        bodyRes = R.string.first_connection_tutorial_other_controls_body,
        icon = Icons.Filled.Speed,
        anchors = listOf(TutorialAnchor.PANEL_LEFT_CONTROLS),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    CENTER_EXTRAS(
        stageRes = R.string.first_connection_tutorial_stage_panel,
        titleRes = R.string.first_connection_tutorial_center_title,
        bodyRes = R.string.first_connection_tutorial_center_body,
        icon = Icons.Filled.Radar,
        anchors = listOf(TutorialAnchor.PANEL_TOOL_DECK),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    ;

    val isLast: Boolean
        get() = this == CENTER_EXTRAS
}

val LocalFirstConnectionTutorial =
    compositionLocalOf<FirstConnectionTutorialViewModel?> { null }

@HiltViewModel
class FirstConnectionTutorialViewModel @Inject constructor(
    private val saveConnectionMode: SaveApplicationConnectionModeUseCase,
    private val saveBoard: SaveApplicationBoardUseCase,
    private val saveUseSoftApCamera: SaveUseSoftApCameraUseCase,
) : ViewModel() {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _step = MutableStateFlow(FirstConnectionTutorialStep.OPEN_CONTROL_PANEL)
    val step: StateFlow<FirstConnectionTutorialStep> = _step.asStateFlow()

    private var applyingStarter = false

    private val _codeExportOpen = MutableStateFlow(false)
    val codeExportOpen: StateFlow<Boolean> = _codeExportOpen.asStateFlow()

    private val _matchingCodeSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val matchingCodeSaved: SharedFlow<Unit> = _matchingCodeSaved.asSharedFlow()

    fun start() {
        _codeExportOpen.value = false
        _step.value = FirstConnectionTutorialStep.OPEN_CONTROL_PANEL
        _active.value = true
    }

    fun dismiss() {
        applyingStarter = false
        _codeExportOpen.value = false
        _active.value = false
    }

    /** Save/Share dialog for the matching sketch is open on the connection step. */
    fun onCodeExportDialogChanged(open: Boolean) {
        if (!_active.value || _step.value != FirstConnectionTutorialStep.CONFIRM_CLASSIC_SIMPLE) {
            return
        }
        _codeExportOpen.value = open
    }

    /** The matching ZIP was saved or handed to the share sheet. */
    fun onMatchingCodeSaved() {
        if (!_active.value || _step.value != FirstConnectionTutorialStep.CONFIRM_CLASSIC_SIMPLE) {
            return
        }
        _codeExportOpen.value = false
        _matchingCodeSaved.tryEmit(Unit)
    }

    fun show(step: FirstConnectionTutorialStep) {
        if (step != FirstConnectionTutorialStep.CONFIRM_CLASSIC_SIMPLE) {
            _codeExportOpen.value = false
        }
        _step.value = step
    }

    /** Stick options menu opened while the stick-setup step is showing. */
    fun onStickOptionsOpened() {
        if (_active.value && _step.value == FirstConnectionTutorialStep.STICK_SETUP) {
            show(FirstConnectionTutorialStep.OTHER_CONTROLS)
        }
    }

    /** Writes DevKit + Classic Simple, then continues on the main thread. */
    fun applyStarterConfiguration(onApplied: () -> Unit) {
        if (applyingStarter) return
        applyingStarter = true
        viewModelScope.launch {
            val applicationId = ApplicationId.CONTROL_PANEL
            saveBoard(applicationId, Esp32Board.defaultFor(applicationId))
            saveUseSoftApCamera(applicationId, false)
            saveConnectionMode(
                applicationId,
                applicationId.originalDefaultConnectionMode(),
            )
            applyingStarter = false
            if (_active.value) onApplied()
        }
    }
}
