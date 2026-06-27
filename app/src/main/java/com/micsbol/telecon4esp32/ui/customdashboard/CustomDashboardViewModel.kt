package com.micsbol.telecon4esp32.ui.customdashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetPlacement
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetType
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.SavedDashboardLayout
import com.micsbol.telecon4esp32.domain.repository.ICustomDashboardRepository
import com.micsbol.telecon4esp32.ui.customdashboard.components.DashboardGridSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

@HiltViewModel
class CustomDashboardViewModel @Inject constructor(
    private val customDashboardRepository: ICustomDashboardRepository,
    private val remoteController: RemoteController,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private var waveformJob: Job? = null
    private var saveConfirmationJob: Job? = null
    private var phase = 0f
    private var editSessionBaseline: EditorSessionBaseline? = null

    private data class EditorSessionBaseline(
        val layoutName: String,
        val widgets: List<DashboardWidgetPlacement>,
    )

    init {
        viewModelScope.launch {
            customDashboardRepository.savedDashboardsFlow.collect { dashboards ->
                _uiState.update {
                    it.copy(savedDashboards = dashboards.sortedBy { layout -> layout.name.lowercase() })
                }
            }
        }
        viewModelScope.launch {
            remoteController.isConnected.collect { connected ->
                updateEditor { editor ->
                    editor.copy(isEsp32Connected = connected)
                }
            }
        }
    }

    fun onEditorVisible() {
        if (waveformJob?.isActive == true) return
        waveformJob = viewModelScope.launch {
            while (isActive) {
                phase += 0.18f
                val points = generateWaveformPoints(phase)
                updateEditor { editor ->
                    editor.copy(
                        plotSeries = demoPlotSeries(points),
                        plotRevision = editor.plotRevision + 1,
                    )
                }
                delay(120L)
            }
        }
    }

    fun onEditorHidden() {
        waveformJob?.cancel()
        waveformJob = null
    }

    fun onCreateNewDashboard() {
        editSessionBaseline = EditorSessionBaseline(layoutName = "", widgets = emptyList())
        _uiState.update {
            it.copy(
                phase = CustomDashboardScreenPhase.EDITOR,
                editor = CustomDashboardEditorState(
                    layoutId = nextLayoutId(),
                    widgets = emptyList(),
                ),
            )
        }
    }

    fun onOpenDashboard(layoutId: String) {
        val layout = _uiState.value.savedDashboards.firstOrNull { it.id == layoutId } ?: return
        editSessionBaseline = EditorSessionBaseline(layoutName = layout.name, widgets = layout.widgets)
        _uiState.update {
            it.copy(
                phase = CustomDashboardScreenPhase.EDITOR,
                editor = CustomDashboardEditorState(
                    layoutId = layout.id,
                    layoutName = layout.name,
                    isEditMode = false,
                    widgets = layout.widgets,
                    plotSeries = demoPlotSeries(generateWaveformPoints(phase)),
                ),
            )
        }
    }

    fun onBackToHome() {
        editSessionBaseline = null
        _uiState.update { state ->
            state.copy(
                phase = CustomDashboardScreenPhase.HOME,
                editor = null,
            )
        }
        onEditorHidden()
    }

    fun onEditorBackPressed() {
        val editor = _uiState.value.editor ?: return
        if (editor.isEditMode) {
            discardEditSessionAndPreview()
        } else {
            onBackToHome()
        }
    }

    fun onSaveLayoutRequested() {
        val editor = _uiState.value.editor ?: return
        if (editor.widgets.isEmpty()) return

        val isPersisted = isDashboardPersisted(editor.layoutId, _uiState.value.savedDashboards)
        if (isPersisted && editor.layoutName.isNotBlank()) {
            viewModelScope.launch { persistLayout(editor.layoutName) }
        } else {
            openSaveNameDialog(isRename = false)
        }
    }

    fun onRenameLayoutRequested() {
        if (_uiState.value.editor == null) return
        openSaveNameDialog(isRename = true)
    }

    fun onSaveNameChanged(name: String) {
        updateEditor { it.copy(pendingSaveName = name) }
    }

    fun onSaveNameDialogDismiss() {
        updateEditor { it.copy(saveNameDialogVisible = false, saveNameDialogIsRename = false) }
    }

    fun onSaveLayoutConfirmed() {
        val editor = _uiState.value.editor ?: return
        val trimmedName = editor.pendingSaveName.trim()
        if (trimmedName.isBlank() || editor.widgets.isEmpty()) return

        viewModelScope.launch { persistLayout(trimmedName) }
    }

    fun onDeleteDashboardRequested(layoutId: String) {
        _uiState.update { it.copy(pendingDeleteLayoutId = layoutId) }
    }

    fun onDeleteDashboardDismiss() {
        _uiState.update { it.copy(pendingDeleteLayoutId = null) }
    }

    fun onDeleteDashboardConfirmed() {
        val layoutId = _uiState.value.pendingDeleteLayoutId ?: return
        viewModelScope.launch {
            val updatedDashboards = _uiState.value.savedDashboards.filterNot { it.id == layoutId }
            customDashboardRepository.saveDashboards(updatedDashboards)
            _uiState.update { it.copy(pendingDeleteLayoutId = null) }
        }
    }

    private fun openSaveNameDialog(isRename: Boolean) {
        val savedDashboards = _uiState.value.savedDashboards
        updateEditor { editor ->
            editor.copy(
                saveNameDialogVisible = true,
                saveNameDialogIsRename = isRename,
                pendingSaveName = suggestLayoutName(editor.layoutName, savedDashboards),
            )
        }
    }

    private suspend fun persistLayout(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val editor = _uiState.value.editor ?: return
        if (editor.widgets.isEmpty()) return

        val updatedEditor = editor.copy(
            layoutName = trimmedName,
            isEditMode = false,
            saveNameDialogVisible = false,
            saveNameDialogIsRename = false,
            saveConfirmationVisible = true,
            selectedWidgetId = null,
            joystickConfigWidgetId = null,
            joystickPositions = emptyMap(),
        )
        editSessionBaseline = EditorSessionBaseline(
            layoutName = updatedEditor.layoutName,
            widgets = updatedEditor.widgets,
        )
        val updatedDashboards = upsertSavedLayout(_uiState.value.savedDashboards, updatedEditor)
            .sortedBy { it.name.lowercase() }

        _uiState.update { it.copy(editor = updatedEditor) }
        customDashboardRepository.saveDashboards(updatedDashboards)

        saveConfirmationJob?.cancel()
        saveConfirmationJob = viewModelScope.launch {
            delay(2_000L)
            updateEditor { it.copy(saveConfirmationVisible = false) }
        }
    }

    private fun suggestLayoutName(
        currentName: String,
        savedDashboards: List<SavedDashboardLayout>,
    ): String {
        val trimmed = currentName.trim()
        if (trimmed.isNotEmpty()) return trimmed

        val baseName = context.getString(R.string.custom_dashboard_untitled_layout)
        val existingNames = savedDashboards.map { it.name.lowercase() }.toSet()
        if (baseName.lowercase() !in existingNames) return baseName

        var index = 2
        while (true) {
            val candidate = context.getString(R.string.custom_dashboard_untitled_layout_numbered, index)
            if (candidate.lowercase() !in existingNames) return candidate
            index++
        }
    }

    private fun upsertSavedLayout(
        savedDashboards: List<SavedDashboardLayout>,
        editor: CustomDashboardEditorState,
    ): List<SavedDashboardLayout> {
        val saved = SavedDashboardLayout(
            id = editor.layoutId,
            name = editor.layoutName.trim(),
            widgets = editor.widgets,
        )
        return savedDashboards.filterNot { it.id == editor.layoutId } + saved
    }

    fun onToggleEditMode() {
        val editor = _uiState.value.editor ?: return
        if (editor.isEditMode) {
            discardEditSessionAndPreview()
        } else {
            captureEditSessionBaseline()
            updateEditor {
                it.copy(
                    isEditMode = true,
                    selectedWidgetId = null,
                    joystickConfigWidgetId = null,
                )
            }
        }
    }

    private fun captureEditSessionBaseline() {
        val editor = _uiState.value.editor ?: return
        editSessionBaseline = EditorSessionBaseline(
            layoutName = editor.layoutName,
            widgets = editor.widgets,
        )
    }

    private fun discardEditSessionAndPreview() {
        val editor = _uiState.value.editor ?: return
        val baseline = editSessionBaseline
            ?: _uiState.value.savedDashboards.firstOrNull { it.id == editor.layoutId }?.let {
                EditorSessionBaseline(layoutName = it.name, widgets = it.widgets)
            }
            ?: EditorSessionBaseline(layoutName = "", widgets = emptyList())

        updateEditor {
            it.copy(
                layoutName = baseline.layoutName,
                widgets = baseline.widgets,
                isEditMode = false,
                selectedWidgetId = null,
                joystickConfigWidgetId = null,
                joystickPositions = emptyMap(),
                saveNameDialogVisible = false,
                saveNameDialogIsRename = false,
            )
        }
    }

    fun onJoystickMove(widgetId: String, x: Float, y: Float) {
        updateEditor { editor ->
            editor.copy(
                joystickPositions = editor.joystickPositions + (widgetId to Pair(x, y)),
            )
        }
    }

    fun onJoystickModeChanged(widgetId: String, mode: JoystickMode) {
        updateEditor { editor ->
            editor.copy(
                widgets = editor.widgets.map { widget ->
                    if (widget.id == widgetId) widget.copy(joystickMode = mode) else widget
                },
                joystickPositions = editor.joystickPositions - widgetId,
            )
        }
    }

    fun onJoystickConfigRequest(widgetId: String) {
        updateEditor { it.copy(joystickConfigWidgetId = widgetId, selectedWidgetId = widgetId) }
    }

    fun onJoystickConfigDismiss() {
        updateEditor { it.copy(joystickConfigWidgetId = null) }
    }

    fun onRelayToggle(widgetId: String) {
        updateWidget(widgetId) { widget -> widget.copy(isOn = !widget.isOn) }
    }

    fun onOutputLevelChange(widgetId: String, level: Float) {
        updateWidget(widgetId) { widget -> widget.copy(level = level.coerceIn(0f, 1f)) }
    }

    fun onSliderChange(widgetId: String, level: Float) {
        updateWidget(widgetId) { widget -> widget.copy(level = level.coerceIn(0f, 1f)) }
    }

    fun onPushButtonPress(widgetId: String) {
        updateWidget(widgetId) { widget -> widget.copy(isPressed = !widget.isPressed) }
    }

    fun onLedToggle(widgetId: String) {
        updateWidget(widgetId) { widget -> widget.copy(isOn = !widget.isOn) }
    }

    fun onWidgetSelected(widgetId: String?) {
        updateEditor { it.copy(selectedWidgetId = widgetId) }
    }

    fun onWidgetMoved(widgetId: String, column: Int, row: Int) {
        updateEditor { editor ->
            editor.copy(
                widgets = editor.widgets.map { widget ->
                    if (widget.id == widgetId) widget.copy(column = column, row = row) else widget
                },
                selectedWidgetId = widgetId,
            )
        }
    }

    fun onWidgetDeleted(widgetId: String) {
        updateEditor { editor ->
            editor.copy(
                widgets = editor.widgets.filterNot { it.id == widgetId },
                joystickPositions = editor.joystickPositions - widgetId,
                selectedWidgetId = if (editor.selectedWidgetId == widgetId) null else editor.selectedWidgetId,
                joystickConfigWidgetId = if (editor.joystickConfigWidgetId == widgetId) {
                    null
                } else {
                    editor.joystickConfigWidgetId
                },
            )
        }
    }

    fun onWidgetAdded(type: DashboardWidgetType, column: Int, row: Int) {
        updateEditor { editor ->
            val columnSpan = type.defaultColumnSpan()
            val rowSpan = type.defaultRowSpan()
            if (hasWidgetOverlap(
                    widgetId = "",
                    column = column,
                    row = row,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                    widgets = editor.widgets,
                )
            ) {
                return@updateEditor editor
            }

            val widgetDefaults = defaultWidgetStateForType(type)
            val widget = DashboardWidgetPlacement(
                id = "${type.name.lowercase()}_${System.currentTimeMillis()}",
                type = type,
                column = column,
                row = row,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                joystickMode = JoystickMode.Spring(),
                isOn = widgetDefaults.isOn,
                level = widgetDefaults.level,
                isPressed = widgetDefaults.isPressed,
                readoutValue = widgetDefaults.readoutValue,
            )
            editor.copy(
                widgets = editor.widgets + widget,
                selectedWidgetId = widget.id,
                joystickConfigWidgetId = if (type == DashboardWidgetType.JOYSTICK) widget.id else editor.joystickConfigWidgetId,
            )
        }
    }

    fun onWidgetAddedAtNextSlot(type: DashboardWidgetType) {
        val editor = _uiState.value.editor ?: return
        val columnSpan = type.defaultColumnSpan()
        val rowSpan = type.defaultRowSpan()
        val slot = findAvailableGridSlot(
            widgets = editor.widgets,
            columnSpan = columnSpan,
            rowSpan = rowSpan,
            gridColumns = DashboardGridSpec.COLUMNS,
            gridRows = DashboardGridSpec.ROWS,
        ) ?: return
        onWidgetAdded(type, slot.first, slot.second)
    }

    private fun updateWidget(
        widgetId: String,
        transform: (DashboardWidgetPlacement) -> DashboardWidgetPlacement,
    ) {
        updateEditor { editor ->
            editor.copy(
                widgets = editor.widgets.map { widget ->
                    if (widget.id == widgetId) transform(widget) else widget
                },
            )
        }
    }

    private fun updateEditor(transform: (CustomDashboardEditorState) -> CustomDashboardEditorState) {
        _uiState.update { state ->
            val editor = state.editor ?: return@update state
            state.copy(editor = transform(editor))
        }
    }

    private fun nextLayoutId(): String = "layout_${System.currentTimeMillis()}"

    private fun generateWaveformPoints(phase: Float): List<Float> {
        return List(80) { index ->
            val t = index / 80f * 4f * Math.PI.toFloat() + phase
            ((sin(t.toDouble()) * 0.45 + sin(t.toDouble() * 2.3) * 0.15 + 0.5)).toFloat()
                .coerceIn(0f, 1f)
        }
    }
}
