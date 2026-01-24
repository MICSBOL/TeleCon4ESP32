package com.example.emitterapp.ui.rc_settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.theme.EmitterAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
){
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
                title = { Text("Joystick Settings") },
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            JoystickModeSelector(
                label = "Left Stick Mode",
                allModes = allModes,
                selectedMode = uiState.leftStickMode,
                onModeSelected = { viewModel.onLeftStickModeChanged(it) }
            )
            JoystickModeSelector(
                label = "Right Stick Mode",
                allModes = allModes,
                selectedMode = uiState.rightStickMode,
                onModeSelected = { viewModel.onRightStickModeChanged(it) }
            )
        }
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

@Preview(showSystemUi = true)
@Composable
private fun JoystickSettingsScreenPreview() {
    EmitterAppTheme {
        RcSettingsScreen(navController = rememberNavController())
    }
}