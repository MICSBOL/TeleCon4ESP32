package com.micsbol.telecon4esp32.ui.rc_settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.UserSettings
import com.micsbol.telecon4esp32.domain.model.canUseAdvancedProtocol
import com.micsbol.telecon4esp32.ui.applications.ApplicationProtocolSettingsSection
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsViewModel
import com.micsbol.telecon4esp32.ui.applications.applicationSettingsTitleRes
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.micsbol.telecon4esp32.ui.components.TeleCon4Esp32Scaffold
import com.micsbol.telecon4esp32.ui.components.EmitterFilledButton
import com.micsbol.telecon4esp32.ui.components.EmitterSectionTitle
import com.micsbol.telecon4esp32.ui.components.EmitterStyledCard
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    uiState: SettingsUiState? = null,
    applicationId: ApplicationId = ApplicationId.CONTROL_PANEL,
    applicationSettingsViewModel: ApplicationSettingsViewModel? = null,
    onDisplayLabelsApplied: (DisplayLabelDraft) -> Unit = {},
) {
    val state = uiState ?: viewModel.uiState.collectAsStateWithLifecycle().value
    val connectionMode = applicationSettingsViewModel
        ?.connectionMode
        ?.collectAsStateWithLifecycle()
        ?.value
    val entitlement = LocalEntitlement.current
    val canUseAdvanced = entitlement.canUseAdvancedProtocol(applicationId)
    val labelDraft by viewModel.labelDraft.collectAsStateWithLifecycle()
    val hasUnsavedLabelChanges by viewModel.hasUnsavedLabelChanges.collectAsStateWithLifecycle()
    TeleCon4Esp32Scaffold(
        title = stringResource(applicationSettingsTitleRes(applicationId)),
        subtitle = stringResource(R.string.rc_controller_settings_title),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        when (val state = state) {
            is SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = brandPrimary())
                }
            }
            is SettingsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is SettingsUiState.Success -> {
                var labelDraftInitialized by remember { mutableStateOf(false) }
                LaunchedEffect(state.settings) {
                    if (!labelDraftInitialized) {
                        viewModel.syncLabelDraftFrom(state.settings)
                        labelDraftInitialized = true
                    }
                }
                SettingsContent(
                    modifier = Modifier.padding(paddingValues),
                    settings = state.settings,
                    labelDraft = labelDraft,
                    hasUnsavedLabelChanges = hasUnsavedLabelChanges,
                    applicationId = applicationId,
                    connectionMode = connectionMode,
                    canUseAdvanced = canUseAdvanced,
                    onConnectionModeChanged = applicationSettingsViewModel?.let { vm ->
                        { mode -> vm.onConnectionModeChanged(mode) }
                    },
                    onLeftStickModeChanged = { viewModel.onLeftStickModeChanged(it) },
                    onRightStickModeChanged = { viewModel.onRightStickModeChanged(it) },
                    onSwitchInitialStateChange = { index, isOn ->
                        viewModel.onSwitchInitialStateChange(index, isOn)
                    },
                    onLeftKnobInitialValueChange = { viewModel.onLeftKnobInitialValueChange(it) },
                    onRightKnobInitialValueChange = { viewModel.onRightKnobInitialValueChange(it) },
                    onLeftPanelUnitChanged = { viewModel.onLeftPanelUnitDraftChanged(it) },
                    onRightPanelUnitChanged = { viewModel.onRightPanelUnitDraftChanged(it) },
                    onAnalogIndicatorUnitChanged = { viewModel.onAnalogIndicatorUnitDraftChanged(it) },
                    onBatteryLabelChanged = { viewModel.onBatteryLabelDraftChanged(it) },
                    onPlotLabelChanged = { index, value ->
                        viewModel.onPlotLabelDraftChanged(index, value)
                    },
                    onApplyDisplayLabels = {
                        viewModel.applyDisplayLabels { draft ->
                            onDisplayLabelsApplied(draft)
                            if (applicationId == ApplicationId.CONTROL_PANEL) {
                                navController.navigateUp()
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    labelDraft: DisplayLabelDraft,
    hasUnsavedLabelChanges: Boolean,
    applicationId: ApplicationId = ApplicationId.CONTROL_PANEL,
    connectionMode: BluetoothConnectionMode? = null,
    canUseAdvanced: Boolean = true,
    onConnectionModeChanged: ((BluetoothConnectionMode) -> Unit)? = null,
    onLeftStickModeChanged: (JoystickMode) -> Unit = {},
    onRightStickModeChanged: (JoystickMode) -> Unit = {},
    onSwitchInitialStateChange: (Int, Boolean) -> Unit = { _, _ -> },
    onLeftKnobInitialValueChange: (Float) -> Unit = {},
    onRightKnobInitialValueChange: (Float) -> Unit = {},
    onLeftPanelUnitChanged: (String) -> Unit = {},
    onRightPanelUnitChanged: (String) -> Unit = {},
    onAnalogIndicatorUnitChanged: (String) -> Unit = {},
    onBatteryLabelChanged: (String) -> Unit = {},
    onPlotLabelChanged: (Int, String) -> Unit = { _, _ -> },
    onApplyDisplayLabels: () -> Unit = {},
) {
    val allModes = remember {
        listOf(
            JoystickMode.Spring(),
            JoystickMode.Hold(),
            JoystickMode.VerticalSpring(),
            JoystickMode.VerticalHold(),
            JoystickMode.HorizontalSpring(),
            JoystickMode.HorizontalHold()
        )
    }

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (connectionMode != null && onConnectionModeChanged != null) {
            item {
                ApplicationProtocolSettingsSection(
                    applicationId = applicationId,
                    selectedMode = connectionMode,
                    onModeSelected = onConnectionModeChanged,
                    canUseAdvanced = canUseAdvanced,
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.rc_controller_settings_joystick_settings)) {
                JoystickModeSelector(
                    label = stringResource(R.string.rc_controller_settings_left_stick),
                    allModes = allModes,
                    selectedMode = settings.leftStickMode,
                    onModeSelected = { onLeftStickModeChanged(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                JoystickModeSelector(
                    label = stringResource(R.string.rc_controller_settings_right_stick),
                    allModes = allModes,
                    selectedMode = settings.rightStickMode,
                    onModeSelected = { onRightStickModeChanged(it) }
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.rc_controller_settings_initial_switch_positions)){
                SwitchSettingsGrid(
                    switchStates = settings.switchInitialStates,
                    onSwitchChange = { index, isOn ->
                        onSwitchInitialStateChange(index, isOn)
                    }
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.rc_controller_settings_initial_knob_values)) {
                KnobSettingsSliders(
                    leftValue = settings.leftKnobInitialValue,
                    rightValue = settings.rightKnobInitialValue,
                    onLeftChange = { onLeftKnobInitialValueChange(it) },
                    onRightChange = { onRightKnobInitialValueChange(it) }
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.rc_controller_settings_display_labels)) {
                TelemetryLabelFields(
                    leftPanelUnit = labelDraft.leftPanelUnit,
                    rightPanelUnit = labelDraft.rightPanelUnit,
                    analogIndicatorUnit = labelDraft.analogIndicatorUnit,
                    batteryLabel = labelDraft.batteryLabel,
                    onLeftPanelUnitChanged = onLeftPanelUnitChanged,
                    onRightPanelUnitChanged = onRightPanelUnitChanged,
                    onAnalogIndicatorUnitChanged = onAnalogIndicatorUnitChanged,
                    onBatteryLabelChanged = onBatteryLabelChanged,
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.rc_controller_settings_plot_labels)) {
                PlotLabelFields(
                    plotLabels = labelDraft.plotLabels,
                    onPlotLabelChanged = onPlotLabelChanged,
                )
            }
        }
        item {
            EmitterFilledButton(
                text = stringResource(R.string.rc_controller_settings_apply_labels),
                onClick = onApplyDisplayLabels,
                enabled = hasUnsavedLabelChanges,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TelemetryLabelFields(
    leftPanelUnit: String,
    rightPanelUnit: String,
    analogIndicatorUnit: String,
    batteryLabel: String,
    onLeftPanelUnitChanged: (String) -> Unit,
    onRightPanelUnitChanged: (String) -> Unit,
    onAnalogIndicatorUnitChanged: (String) -> Unit,
    onBatteryLabelChanged: (String) -> Unit,
) {
    val textFieldColors = settingsOutlinedTextFieldColors()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsTextField(
            value = leftPanelUnit,
            onValueChange = onLeftPanelUnitChanged,
            label = { Text(stringResource(R.string.rc_controller_settings_left_panel_unit)) },
            placeholder = { Text(stringResource(R.string.rc_controller_settings_left_panel_unit_hint)) },
            textStyle = textStyle,
            colors = textFieldColors,
            imeAction = ImeAction.Next,
        )
        SettingsTextField(
            value = rightPanelUnit,
            onValueChange = onRightPanelUnitChanged,
            label = { Text(stringResource(R.string.rc_controller_settings_right_panel_unit)) },
            placeholder = { Text(stringResource(R.string.rc_controller_settings_right_panel_unit_hint)) },
            textStyle = textStyle,
            colors = textFieldColors,
            imeAction = ImeAction.Next,
        )
        SettingsTextField(
            value = analogIndicatorUnit,
            onValueChange = onAnalogIndicatorUnitChanged,
            label = { Text(stringResource(R.string.rc_controller_settings_analog_indicator_unit)) },
            placeholder = { Text(stringResource(R.string.rc_controller_settings_analog_indicator_unit_hint)) },
            textStyle = textStyle,
            colors = textFieldColors,
            imeAction = ImeAction.Next,
        )
        SettingsTextField(
            value = batteryLabel,
            onValueChange = onBatteryLabelChanged,
            label = { Text(stringResource(R.string.rc_controller_settings_battery_label)) },
            placeholder = { Text(stringResource(R.string.rc_controller_settings_battery_label_hint)) },
            textStyle = textStyle,
            colors = textFieldColors,
            imeAction = ImeAction.Next,
        )
    }
}

@Composable
fun PlotLabelFields(
    plotLabels: List<String>,
    onPlotLabelChanged: (Int, String) -> Unit,
) {
    val textFieldColors = settingsOutlinedTextFieldColors()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val labelResIds = listOf(
        R.string.rc_controller_settings_plot_label_1,
        R.string.rc_controller_settings_plot_label_2,
        R.string.rc_controller_settings_plot_label_3,
        R.string.rc_controller_settings_plot_label_4,
    )
    val hintResIds = listOf(
        R.string.rc_controller_settings_plot_label_hint_1,
        R.string.rc_controller_settings_plot_label_hint_2,
        R.string.rc_controller_settings_plot_label_hint_3,
        R.string.rc_controller_settings_plot_label_hint_4,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(UserSettings.PLOT_LABEL_COUNT) { index ->
            SettingsTextField(
                value = plotLabels.getOrElse(index) { "" },
                onValueChange = { onPlotLabelChanged(index, it) },
                label = { Text(stringResource(labelResIds[index])) },
                placeholder = { Text(stringResource(hintResIds[index])) },
                textStyle = textStyle,
                colors = textFieldColors,
                imeAction = if (index < UserSettings.PLOT_LABEL_COUNT - 1) {
                    ImeAction.Next
                } else {
                    ImeAction.Done
                },
            )
        }
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    placeholder: @Composable (() -> Unit)? = null,
    textStyle: TextStyle,
    colors: TextFieldColors,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(value) {
        if (!isFocused && text != value) {
            text = value
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        label = label,
        placeholder = placeholder,
        textStyle = textStyle,
        colors = colors,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() },
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun settingsOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
)

@Composable
fun KnobSettingsSliders(
    leftValue: Float,
    rightValue: Float,
    onLeftChange: (Float) -> Unit,
    onRightChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.rc_controller_settings_left_knob), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = leftValue,
            onValueChange = onLeftChange,
            valueRange = 0f..1f,
            steps = 9
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.rc_controller_settings_right_knob), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = rightValue,
            onValueChange = onRightChange,
            valueRange = 0f..1f,
            steps = 9
        )
    }
}
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        EmitterSectionTitle(text = title)
        EmitterStyledCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SwitchSettingsGrid(
    switchStates: Map<Int, Boolean>,
    onSwitchChange: (index: Int, isOn: Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            (0..2).forEach { index ->
                SwitchSetting(
                    label = "S${index + 1}",
                    isChecked = switchStates[index] ?: false,
                    onCheckedChange = { onSwitchChange(index, it) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            (3..5).forEach { index ->
                SwitchSetting(
                    label = "S${index + 1}",
                    isChecked = switchStates[index] ?: false,
                    onCheckedChange = { onSwitchChange(index, it) }
                )
            }
        }
    }
}

@Composable
fun SwitchSetting(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.padding(2.dp))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoystickModeSelector(
    label: String,
    allModes: List<JoystickMode>,
    selectedMode: JoystickMode,
    onModeSelected: (JoystickMode) -> Unit
) {
    val initialPositions = remember {
        listOf(
            "Center" to JoystickMode.CENTER,
            "Up" to JoystickMode.UP,
            "Down" to JoystickMode.DOWN,
            "Left" to JoystickMode.LEFT,
            "Right" to JoystickMode.RIGHT
        )
    }
    val allowedInitialPositions = remember(selectedMode) {
        initialPositions.filter { (_, position) ->
            isInitialPositionAllowedForMode(selectedMode, position)
        }
    }
    var isModeExpanded by remember { mutableStateOf(false) }
    var isPositionExpanded by remember { mutableStateOf(false) }

    Column {
        Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = isModeExpanded,
                onExpandedChange = { isModeExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedMode::class.java.simpleName,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModeExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTrailingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isModeExpanded,
                    onDismissRequest = { isModeExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    allModes.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(text = mode::class.java.simpleName, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                val carriedPosition = sanitizeInitialPositionForMode(mode, selectedMode.initialPosition)
                                val newMode = mode.withInitialPosition(carriedPosition)
                                onModeSelected(newMode)
                                isModeExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurface,
                                trailingIconColor = MaterialTheme.colorScheme.onSurface,
                            )
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = isPositionExpanded,
                onExpandedChange = { isPositionExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = allowedInitialPositions.firstOrNull { it.second == selectedMode.initialPosition }?.first ?: "Center",
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPositionExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isPositionExpanded,
                    onDismissRequest = { isPositionExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    allowedInitialPositions.forEach { (name, position) ->
                        DropdownMenuItem(
                            text = { Text(text = name, color = MaterialTheme.colorScheme.onSurface)},
                            onClick = {
                                val newMode = selectedMode.withInitialPosition(position)
                                onModeSelected(newMode)
                                isPositionExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                                leadingIconColor = MaterialTheme.colorScheme.onSurface,
                                trailingIconColor = MaterialTheme.colorScheme.onSurface,
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun isInitialPositionAllowedForMode(mode: JoystickMode, position: Pair<Int, Int>): Boolean {
    return when (mode) {
        is JoystickMode.Spring -> position == JoystickMode.CENTER

        is JoystickMode.VerticalSpring,
        is JoystickMode.VerticalHold -> position != JoystickMode.LEFT && position != JoystickMode.RIGHT

        is JoystickMode.HorizontalSpring,
        is JoystickMode.HorizontalHold -> position != JoystickMode.UP && position != JoystickMode.DOWN

        is JoystickMode.Hold -> true
    }
}

private fun sanitizeInitialPositionForMode(mode: JoystickMode, position: Pair<Int, Int>): Pair<Int, Int> {
    return if (isInitialPositionAllowedForMode(mode, position)) position else JoystickMode.CENTER
}

private fun JoystickMode.withInitialPosition(position: Pair<Int, Int>): JoystickMode {
    return when (this) {
        is JoystickMode.Spring -> JoystickMode.Spring(position)
        is JoystickMode.Hold -> JoystickMode.Hold(position)
        is JoystickMode.VerticalSpring -> JoystickMode.VerticalSpring(position)
        is JoystickMode.VerticalHold -> JoystickMode.VerticalHold(position)
        is JoystickMode.HorizontalSpring -> JoystickMode.HorizontalSpring(position)
        is JoystickMode.HorizontalHold -> JoystickMode.HorizontalHold(position)
    }
}



@Preview(showBackground = true)
@Composable
fun RcSettingsScreenPreview() {
    val navController = rememberNavController()
    val mockSettings = UserSettings(
        leftStickMode = JoystickMode.Spring(),
        rightStickMode = JoystickMode.Hold(),
        switchInitialStates = mapOf(0 to true, 1 to false, 2 to true, 3 to false, 4 to true, 5 to false),
        leftKnobInitialValue = 0.5f,
        rightKnobInitialValue = 0.7f
    )
    val uiState = SettingsUiState.Success(mockSettings)
    TeleCon4Esp32Theme {
        RcSettingsScreen(navController = navController, uiState = uiState)
    }
}

@Preview(showBackground = true)
@Composable
fun KnobSettingsSlidersPreview() {
    TeleCon4Esp32Theme {
        KnobSettingsSliders(
            leftValue = 0.5f,
            rightValue = 0.7f,
            onLeftChange = {},
            onRightChange = {}
        )
    }
}
