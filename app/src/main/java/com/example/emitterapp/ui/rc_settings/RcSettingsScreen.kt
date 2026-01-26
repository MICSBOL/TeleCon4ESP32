package com.example.emitterapp.ui.rc_settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.theme.EmitterAppTheme
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState(initial = SettingsState())
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RC Controller Settings") }, // More general title
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection(title = "Joystick Settings") {
                    JoystickModeSelector(
                        label = "Left Stick",
                        allModes = allModes,
                        selectedMode = uiState.leftStickMode,
                        onModeSelected = { viewModel.onLeftStickModeChanged(it) }
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    JoystickModeSelector(
                        label = "Right Stick",
                        allModes = allModes,
                        selectedMode = uiState.rightStickMode,
                        onModeSelected = { viewModel.onRightStickModeChanged(it) }
                    )
                }
            }
            item {
                SettingsSection(title = "Initial Switch Positions") {
                    SwitchSettingsGrid(
                        switchStates = uiState.switchInitialStates,
                        onSwitchChange = { index, isOn ->
                            viewModel.onSwitchInitialStateChange(index, isOn)
                        }
                    )
                }
            }
            item {
                SettingsSection(title = "Initial Knob Values") {
                    KnobSettingsSliders(
                        leftValue = uiState.leftKnobInitialValue,
                        rightValue = uiState.rightKnobInitialValue,
                        onLeftChange = { viewModel.onLeftKnobInitialValueChange(it) },
                        onRightChange = { viewModel.onRightKnobInitialValueChange(it) }
                    )
                }
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
        // Slider for the Left Knob
        Text("Left Knob", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = leftValue,
            onValueChange = onLeftChange,
            valueRange = 0f..1f, // Standard range for a normalized value
            steps = 9 // This creates 10 steps (0.0, 0.1, 0.2, ...) for finer control
        )

        // Spacer between the two sliders
        Spacer(modifier = Modifier.height(8.dp))

        // Slider for the Right Knob
        Text("Right Knob", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = rightValue,
            onValueChange = onRightChange,
            valueRange = 0f..1f,
            steps = 9
        )
    }
}
// A reusable composable for a section card in the settings
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

// --- START OF NEW COMPOSABLE ---
@Composable
fun SwitchSettingsGrid(
    switchStates: Map<Int, Boolean>,
    onSwitchChange: (index: Int, isOn: Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Top row of switches (S1, S2, S3)
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
        // Bottom row of switches (S4, S5, S6)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            (3..5).forEach { index ->
                SwitchSetting(
                    label = "S${index + 1}", // This will correctly label S4, S5, S6
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
fun RcSettingsScreenForPreview(
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RC Controller Settings") }, // More general title
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = {  }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection(title = "Joystick Settings") {
                    JoystickModeSelector(
                        label = "Left Stick",
                        allModes = allModes,
                        selectedMode = JoystickMode.Spring(),
                        onModeSelected = {  }
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    JoystickModeSelector(
                        label = "Right Stick",
                        allModes = allModes,
                        selectedMode = JoystickMode.Spring(),
                        onModeSelected = {  }
                    )
                }
            }
            item {
                SettingsSection(title = "Initial Switch Positions") {
                    SwitchSettingsGrid(
                        switchStates = mapOf(0 to false, 1 to false, 2 to false, 3 to false, 4 to false, 5 to false),
                        onSwitchChange = { _, _ -> }
                    )
                }
            }
            item {
                SettingsSection(title = "Initial Knob Values") {
                    KnobSettingsSliders(
                        leftValue = 10f,
                        rightValue = 20f,
                        onLeftChange = {  },
                        onRightChange = {  }
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RcSettingsScreenPreview() {
    EmitterAppTheme {
        RcSettingsScreenForPreview()
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
        mapOf(
            "Center" to JoystickMode.CENTER,
            "Up" to JoystickMode.UP,
            "Down" to JoystickMode.DOWN,
            "Left" to JoystickMode.LEFT,
            "Right" to JoystickMode.RIGHT
        )
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
                                val newMode = when (mode) {
                                    is JoystickMode.Spring -> JoystickMode.Spring(selectedMode.initialPosition)
                                    is JoystickMode.Hold -> JoystickMode.Hold(selectedMode.initialPosition)
                                    is JoystickMode.VerticalSpring -> JoystickMode.VerticalSpring(selectedMode.initialPosition)
                                    is JoystickMode.VerticalHold -> JoystickMode.VerticalHold(selectedMode.initialPosition)
                                    is JoystickMode.HorizontalSpring -> JoystickMode.HorizontalSpring(selectedMode.initialPosition)
                                    is JoystickMode.HorizontalHold -> JoystickMode.HorizontalHold(selectedMode.initialPosition)
                                }
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
                    value = initialPositions.entries.firstOrNull { it.value == selectedMode.initialPosition }?.key ?: "Center",
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
                    initialPositions.forEach { (name, position) ->
                        DropdownMenuItem(
                            text = { Text(text = name, color = MaterialTheme.colorScheme.onSurface)},
                            onClick = {
                                val newMode = when (selectedMode) {
                                    is JoystickMode.Spring -> JoystickMode.Spring(position)
                                    is JoystickMode.Hold -> JoystickMode.Hold(position)
                                    is JoystickMode.VerticalSpring -> JoystickMode.VerticalSpring(position)
                                    is JoystickMode.VerticalHold -> JoystickMode.VerticalHold(position)
                                    is JoystickMode.HorizontalSpring -> JoystickMode.HorizontalSpring(position)
                                    is JoystickMode.HorizontalHold -> JoystickMode.HorizontalHold(position)
                                }
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
