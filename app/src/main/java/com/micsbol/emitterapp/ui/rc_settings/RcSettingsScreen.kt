package com.micsbol.emitterapp.ui.rc_settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.micsbol.emitterapp.R
import com.micsbol.emitterapp.domain.model.UserSettings
import com.micsbol.emitterapp.domain.model.JoystickMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.micsbol.emitterapp.ui.components.EmitterAppScaffold
import com.micsbol.emitterapp.ui.components.EmitterSectionTitle
import com.micsbol.emitterapp.ui.components.EmitterStyledCard
import com.micsbol.emitterapp.ui.components.brandPrimary
import com.micsbol.emitterapp.ui.theme.EmitterAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    uiState: SettingsUiState? = null
) {
    val state = uiState ?: viewModel.uiState.collectAsStateWithLifecycle().value
    EmitterAppScaffold(
        title = stringResource(R.string.home_title),
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
                SettingsContent(
                    modifier = Modifier.padding(paddingValues),
                    settings = state.settings,
                    onLeftStickModeChanged = { viewModel.onLeftStickModeChanged(it) },
                    onRightStickModeChanged = { viewModel.onRightStickModeChanged(it) },
                    onSwitchInitialStateChange = { index, isOn ->
                        viewModel.onSwitchInitialStateChange(index, isOn)
                    },
                    onLeftKnobInitialValueChange = { viewModel.onLeftKnobInitialValueChange(it) },
                    onRightKnobInitialValueChange = { viewModel.onRightKnobInitialValueChange(it) }
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    onLeftStickModeChanged: (JoystickMode) -> Unit = {},
    onRightStickModeChanged: (JoystickMode) -> Unit = {},
    onSwitchInitialStateChange: (Int, Boolean) -> Unit = { _, _ -> },
    onLeftKnobInitialValueChange: (Float) -> Unit = {},
    onRightKnobInitialValueChange: (Float) -> Unit = {}
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
    }
}

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
    EmitterAppTheme {
        RcSettingsScreen(navController = navController, uiState = uiState)
    }
}

@Preview(showBackground = true)
@Composable
fun KnobSettingsSlidersPreview() {
    EmitterAppTheme {
        KnobSettingsSliders(
            leftValue = 0.5f,
            rightValue = 0.7f,
            onLeftChange = {},
            onRightChange = {}
        )
    }
}
