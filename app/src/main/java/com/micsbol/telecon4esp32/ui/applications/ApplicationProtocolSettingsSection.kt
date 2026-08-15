package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.domain.camera.isCameraStreamAtRisk
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.availableConnectionModes
import com.micsbol.telecon4esp32.domain.model.connectionModesForFamily
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.model.settingsUserType
import com.micsbol.telecon4esp32.domain.model.usesCamera
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo

data class SettingsMenuOption<T>(
    val value: T,
    val label: String,
    val description: String,
    val enabled: Boolean = true,
)

@Composable
fun ApplicationDeviceSettingsSection(
    selectedBoard: Esp32Board,
    onBoardSelected: (Esp32Board) -> Unit,
    userType: SettingsUserType = SettingsUserType.NORMAL,
    canUseAdvanced: Boolean = true,
) {
    val showKitB = userType == SettingsUserType.ADVANCED && canUseAdvanced
    val options = buildList {
        add(
            SettingsMenuOption(
                value = Esp32Board.DEV_KIT,
                label = stringResource(R.string.app_settings_device_dev_kit),
                description = stringResource(R.string.app_settings_device_dev_kit_description),
            ),
        )
        add(
            SettingsMenuOption(
                value = Esp32Board.CAM,
                label = stringResource(R.string.app_settings_device_cam),
                description = stringResource(R.string.app_settings_device_cam_description),
            ),
        )
        if (showKitB) {
            add(
                SettingsMenuOption(
                    value = Esp32Board.CAM_AND_DEV_KIT,
                    label = stringResource(R.string.app_settings_device_cam_and_dev_kit),
                    description = stringResource(R.string.app_settings_device_cam_and_dev_kit_description),
                ),
            )
        }
    }
    val boardSelection = rememberCamAwareBoardSelection(
        selectedBoard = selectedBoard,
        onBoardSelected = onBoardSelected,
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        NeoSectionTitle(text = stringResource(R.string.app_settings_device_section_title))
        Spacer(modifier = Modifier.height(12.dp))
        SettingsOptionDropdown(
            options = options,
            selected = selectedBoard,
            onSelected = boardSelection.onSelect,
            sectionInfo = stringResource(R.string.app_settings_device_section_description),
        )
        if (boardSelection.showAtRiskBanner) {
            Spacer(modifier = Modifier.height(12.dp))
            CamBoardPerformanceWarningBanner()
        }
        boardSelection.WarningDialog()
    }
}

/**
 * Wraps board selection so choosing ESP32-CAM on a weak phone shows a confirm dialog
 * and keeps a persistent banner while CAM remains selected.
 */
@Composable
fun rememberCamAwareBoardSelection(
    selectedBoard: Esp32Board,
    onBoardSelected: (Esp32Board) -> Unit,
): CamAwareBoardSelection {
    val context = LocalContext.current
    val atRisk = remember(context) { context.isCameraStreamAtRisk() }
    var pendingCamConfirm by remember { mutableStateOf(false) }
    var pendingCamBoard by remember { mutableStateOf(Esp32Board.CAM) }

    val onSelect: (Esp32Board) -> Unit = { board ->
        if (board.usesSoftApCamera && atRisk && !selectedBoard.usesSoftApCamera) {
            pendingCamConfirm = true
            pendingCamBoard = board
        } else {
            onBoardSelected(board)
        }
    }

    return CamAwareBoardSelection(
        onSelect = onSelect,
        showAtRiskBanner = selectedBoard.usesSoftApCamera && atRisk,
        warningDialog = {
            if (pendingCamConfirm) {
                CamBoardPerformanceWarningDialog(
                    onConfirm = {
                        pendingCamConfirm = false
                        onBoardSelected(pendingCamBoard)
                    },
                    onDismiss = { pendingCamConfirm = false },
                )
            }
        },
    )
}

class CamAwareBoardSelection(
    val onSelect: (Esp32Board) -> Unit,
    val showAtRiskBanner: Boolean,
    private val warningDialog: @Composable () -> Unit,
) {
    @Composable
    fun WarningDialog() = warningDialog()
}

@Composable
fun CamBoardPerformanceWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    NeoDialog(
        onDismissRequest = onDismiss,
        title = {
            NeoDialogTitle(text = stringResource(R.string.app_settings_device_cam_performance_warning_title))
        },
        subtitle = {
            NeoDialogBody(text = stringResource(R.string.app_settings_device_cam_performance_warning_body))
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeoPillButton(
                    text = stringResource(R.string.app_settings_device_cam_performance_warning_continue),
                    onClick = onConfirm,
                    fillMaxWidth = true,
                    compact = true,
                )
                NeoSecondaryButton(
                    text = stringResource(R.string.app_settings_device_cam_performance_warning_cancel),
                    onClick = onDismiss,
                    fillMaxWidth = true,
                    compact = true,
                )
            }
        },
    )
}

@Composable
fun CamBoardPerformanceWarningBanner(
    modifier: Modifier = Modifier,
    secondaryColor: Color = Neo.TextSecondary,
    accent: Color = Color(0xFFF4A261),
    container: Color = accent.copy(alpha = 0.14f),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.app_settings_device_cam_performance_warning_banner),
            style = MaterialTheme.typography.bodySmall,
            color = secondaryColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ApplicationProtocolSettingsSection(
    applicationId: ApplicationId,
    selectedMode: BluetoothConnectionMode,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    userType: SettingsUserType,
    canUseAdvanced: Boolean = true,
    selectedBoard: Esp32Board = Esp32Board.DEV_KIT,
    belowConnectionTypeContent: (@Composable () -> Unit)? = null,
) {
    val availableModes = applicationId.availableConnectionModes(selectedBoard, userType)
    val camModesOnly = applicationId.usesCamera() && selectedBoard == Esp32Board.CAM
    val kitBModesOnly = applicationId.usesCamera() && selectedBoard.isKitBDual
    val useDevKitLinkPicker = !camModesOnly && !kitBModesOnly
    val includesWifi = applicationId.availableConnectionModes(selectedBoard).any { it.isWifiLink }
    val connectionSectionInfo = stringResource(
        when {
            kitBModesOnly -> R.string.app_settings_connection_section_description_cam
            camModesOnly -> R.string.app_settings_connection_section_description_cam
            useDevKitLinkPicker -> R.string.app_settings_connection_section_description_devkit
            else -> R.string.app_settings_connection_section_description
        },
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        NeoSectionTitle(
            text = stringResource(
                if (includesWifi) {
                    R.string.app_settings_connection_section_title_general
                } else {
                    R.string.app_settings_connection_section_title
                },
            ),
        )
        Text(
            text = stringResource(
                R.string.app_settings_protocol_prefix_label,
                applicationId.protocolPrefix(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Neo.TextSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (useDevKitLinkPicker) {
            DevKitConnectionSettings(
                applicationId = applicationId,
                selectedBoard = selectedBoard,
                selectedMode = selectedMode,
                userType = userType,
                onModeSelected = onModeSelected,
                canUseAdvanced = canUseAdvanced,
                connectionSectionInfo = connectionSectionInfo,
                belowConnectionTypeContent = belowConnectionTypeContent,
            )
        } else {
            val options = availableModes.map { mode ->
                connectionModeMenuOption(
                    mode = mode,
                    canUseAdvanced = canUseAdvanced,
                    camKitLabels = camModesOnly || kitBModesOnly,
                )
            }
            SettingsOptionDropdown(
                options = options,
                selected = selectedMode,
                onSelected = onModeSelected,
                sectionInfo = connectionSectionInfo,
            )
            if (belowConnectionTypeContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                belowConnectionTypeContent()
            }
        }
    }
}

@Composable
fun SettingsUserTypeSection(
    selected: SettingsUserType,
    canUseAdvanced: Boolean,
    onSelected: (SettingsUserType) -> Unit,
    modifier: Modifier = Modifier,
    fieldColors: TextFieldColors = settingsDropdownFieldColors(),
    menuBackground: Color = AppGlass.DialogSurface,
    descriptionColor: Color = Neo.TextSecondary,
    accent: Color = Neo.Accent,
    dividerColor: Color = Neo.TextSecondary.copy(alpha = 0.28f),
    labelColor: Color = Neo.TextPrimary,
    mutedLabelColor: Color = Neo.TextMuted,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NeoSectionTitle(text = stringResource(R.string.app_settings_user_type_section_title))
        Spacer(modifier = Modifier.height(12.dp))
        SettingsUserTypeSelector(
            selected = selected,
            canUseAdvanced = canUseAdvanced,
            onSelected = onSelected,
            sectionInfo = stringResource(R.string.app_settings_user_type_section_description),
            fieldColors = fieldColors,
            menuBackground = menuBackground,
            descriptionColor = descriptionColor,
            accent = accent,
            dividerColor = dividerColor,
            labelColor = labelColor,
            mutedLabelColor = mutedLabelColor,
        )
    }
}

@Composable
fun SettingsUserTypeSelector(
    selected: SettingsUserType,
    canUseAdvanced: Boolean,
    onSelected: (SettingsUserType) -> Unit,
    modifier: Modifier = Modifier,
    sectionInfo: String? = null,
    fieldColors: TextFieldColors = settingsDropdownFieldColors(),
    menuBackground: Color = AppGlass.DialogSurface,
    descriptionColor: Color = Neo.TextSecondary,
    accent: Color = Neo.Accent,
    dividerColor: Color = Neo.TextSecondary.copy(alpha = 0.28f),
    labelColor: Color = Neo.TextPrimary,
    mutedLabelColor: Color = Neo.TextMuted,
) {
    val options = listOf(
        SettingsMenuOption(
            value = SettingsUserType.NORMAL,
            label = stringResource(R.string.app_settings_user_type_normal),
            description = stringResource(R.string.app_settings_user_type_normal_description),
        ),
        SettingsMenuOption(
            value = SettingsUserType.ADVANCED,
            label = stringResource(R.string.app_settings_user_type_advanced),
            description = if (canUseAdvanced) {
                stringResource(R.string.app_settings_user_type_advanced_description)
            } else {
                stringResource(R.string.app_settings_protocol_advanced_premium_required)
            },
            enabled = canUseAdvanced,
        ),
    )
    SettingsOptionDropdown(
        options = options,
        selected = selected,
        onSelected = onSelected,
        modifier = modifier,
        sectionInfo = sectionInfo,
        fieldColors = fieldColors,
        menuBackground = menuBackground,
        descriptionColor = descriptionColor,
        accent = accent,
        dividerColor = dividerColor,
        labelColor = labelColor,
        mutedLabelColor = mutedLabelColor,
    )
}

@Composable
private fun DevKitConnectionSettings(
    applicationId: ApplicationId,
    selectedBoard: Esp32Board,
    selectedMode: BluetoothConnectionMode,
    userType: SettingsUserType,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    canUseAdvanced: Boolean,
    connectionSectionInfo: String,
    belowConnectionTypeContent: (@Composable () -> Unit)? = null,
) {
    val selectedFamily = selectedMode.linkFamily
    val familyOptions = listOf(
        SettingsMenuOption(
            value = ConnectionLinkFamily.BLUETOOTH,
            label = stringResource(R.string.app_settings_connection_link_bluetooth),
            description = stringResource(R.string.app_settings_connection_link_bluetooth_description),
        ),
        SettingsMenuOption(
            value = ConnectionLinkFamily.WIFI,
            label = stringResource(R.string.app_settings_connection_link_wifi),
            description = stringResource(R.string.app_settings_connection_link_wifi_description),
        ),
    )
    val detailModes = applicationId.connectionModesForFamily(selectedBoard, selectedFamily, userType)
    val detailOptions = detailModes.map { mode ->
        connectionModeMenuOption(
            mode = mode,
            canUseAdvanced = canUseAdvanced,
            camKitLabels = false,
        )
    }
    val protocolSectionInfo = stringResource(
        if (selectedFamily == ConnectionLinkFamily.WIFI) {
            R.string.app_settings_connection_wifi_protocol_description
        } else {
            R.string.app_settings_connection_bluetooth_protocol_description
        },
    )

    SettingsOptionDropdown(
        options = familyOptions,
        selected = selectedFamily,
        onSelected = { family ->
            if (family == selectedFamily) return@SettingsOptionDropdown
            val preferred = applicationId.preferredConnectionMode(
                board = selectedBoard,
                family = family,
                userType = userType,
                canUseAdvanced = canUseAdvanced,
            )
            if (preferred != null) onModeSelected(preferred)
        },
        sectionInfo = connectionSectionInfo,
    )
    // Normal (and any single-protocol link) has nothing to choose; skip the section.
    if (detailOptions.size > 1) {
        Spacer(modifier = Modifier.height(16.dp))
        NeoSectionTitle(
            text = stringResource(
                if (selectedFamily == ConnectionLinkFamily.WIFI) {
                    R.string.app_settings_connection_wifi_protocol_title
                } else {
                    R.string.app_settings_connection_bluetooth_protocol_title
                },
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingsOptionDropdown(
            options = detailOptions,
            selected = selectedMode,
            onSelected = onModeSelected,
            sectionInfo = protocolSectionInfo,
        )
    }
    if (belowConnectionTypeContent != null) {
        Spacer(modifier = Modifier.height(16.dp))
        belowConnectionTypeContent()
    }
}

@Composable
fun SoftApPerformanceSettingsSection(
    selectedPreset: SoftApPerformancePreset,
    onPresetSelected: (SoftApPerformancePreset) -> Unit,
    selectedHudRate: SoftApHudProcessingRate,
    onHudRateSelected: (SoftApHudProcessingRate) -> Unit,
) {
    val presetOptions = SoftApPerformancePreset.entries.map { preset ->
        SettingsMenuOption(
            value = preset,
            label = stringResource(preset.titleRes()),
            description = stringResource(preset.descriptionRes()),
        )
    }
    val hudOptions = SoftApHudProcessingRate.entries.map { rate ->
        SettingsMenuOption(
            value = rate,
            label = stringResource(rate.titleRes()),
            description = stringResource(rate.descriptionRes()),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        NeoSectionTitle(text = stringResource(R.string.rc_vehicle_softap_perf_section_title))
        Spacer(modifier = Modifier.height(12.dp))
        SettingsOptionDropdown(
            options = presetOptions,
            selected = selectedPreset,
            onSelected = onPresetSelected,
            sectionInfo = stringResource(R.string.rc_vehicle_softap_perf_section_body),
        )
        Spacer(modifier = Modifier.height(20.dp))
        NeoSectionTitle(text = stringResource(R.string.rc_vehicle_softap_hud_rate_section_title))
        Spacer(modifier = Modifier.height(12.dp))
        SettingsOptionDropdown(
            options = hudOptions,
            selected = selectedHudRate,
            onSelected = onHudRateSelected,
            sectionInfo = buildString {
                append(stringResource(R.string.rc_vehicle_softap_hud_rate_section_body))
                append("\n\n")
                append(stringResource(R.string.rc_vehicle_softap_miui_tip))
            },
        )
    }
}

@Composable
fun connectionModeMenuOption(
    mode: BluetoothConnectionMode,
    canUseAdvanced: Boolean,
    camKitLabels: Boolean,
): SettingsMenuOption<BluetoothConnectionMode> = when (mode) {
    BluetoothConnectionMode.WIFI_CAM_STARTER -> SettingsMenuOption(
        value = mode,
        label = stringResource(R.string.app_settings_connection_wifi_cam_starter),
        description = stringResource(R.string.app_settings_connection_wifi_cam_starter_description),
    )
    BluetoothConnectionMode.WIFI_SOFTAP -> SettingsMenuOption(
        value = mode,
        label = stringResource(
            if (camKitLabels) {
                R.string.app_settings_connection_wifi_softap_kit_a
            } else {
                R.string.app_settings_connection_wifi_softap
            },
        ),
        description = if (canUseAdvanced) {
            stringResource(
                if (camKitLabels) {
                    R.string.app_settings_connection_wifi_softap_kit_a_description
                } else {
                    R.string.app_settings_connection_wifi_softap_description
                },
            )
        } else {
            stringResource(R.string.app_settings_protocol_advanced_premium_required)
        },
        enabled = canUseAdvanced,
    )
    BluetoothConnectionMode.WIFI_SIMPLE -> SettingsMenuOption(
        value = mode,
        label = stringResource(R.string.app_settings_connection_wifi_simple),
        description = stringResource(R.string.app_settings_connection_wifi_simple_description),
    )
    BluetoothConnectionMode.WIFI_BINARY -> SettingsMenuOption(
        value = mode,
        label = stringResource(
            if (camKitLabels) {
                R.string.app_settings_connection_wifi_binary_cam
            } else {
                R.string.app_settings_connection_wifi_binary
            },
        ),
        description = if (canUseAdvanced) {
            stringResource(
                if (camKitLabels) {
                    R.string.app_settings_connection_wifi_binary_cam_description
                } else {
                    R.string.app_settings_connection_wifi_binary_description
                },
            )
        } else {
            stringResource(R.string.app_settings_protocol_advanced_premium_required)
        },
        enabled = canUseAdvanced,
    )
    BluetoothConnectionMode.CLASSIC_SIMPLE -> SettingsMenuOption(
        value = mode,
        label = stringResource(R.string.app_settings_connection_classic_simple),
        description = stringResource(R.string.app_settings_connection_classic_simple_description),
    )
    BluetoothConnectionMode.CLASSIC_BINARY -> SettingsMenuOption(
        value = mode,
        label = stringResource(R.string.app_settings_connection_classic_binary),
        description = if (canUseAdvanced) {
            stringResource(R.string.app_settings_connection_classic_binary_description)
        } else {
            stringResource(R.string.app_settings_protocol_advanced_premium_required)
        },
        enabled = canUseAdvanced,
    )
    BluetoothConnectionMode.BLE_BINARY -> SettingsMenuOption(
        value = mode,
        label = stringResource(
            if (camKitLabels) {
                R.string.app_settings_connection_ble_binary_kit_b
            } else {
                R.string.app_settings_connection_ble_binary
            },
        ),
        description = when {
            !canUseAdvanced ->
                stringResource(R.string.app_settings_protocol_advanced_premium_required)
            camKitLabels ->
                stringResource(R.string.app_settings_connection_ble_binary_kit_b_description)
            else ->
                stringResource(R.string.app_settings_connection_ble_binary_description)
        },
        enabled = canUseAdvanced,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsOptionDropdown(
    options: List<SettingsMenuOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    sectionInfo: String? = null,
    fieldColors: TextFieldColors = settingsDropdownFieldColors(),
    menuBackground: Color = AppGlass.DialogSurface,
    descriptionColor: Color = Neo.TextSecondary,
    accent: Color = Neo.Accent,
    dividerColor: Color = Neo.TextSecondary.copy(alpha = 0.28f),
    labelColor: Color = Neo.TextPrimary,
    mutedLabelColor: Color = Neo.TextMuted,
) {
    var expanded by remember { mutableStateOf(false) }
    var infoExpanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.value == selected } ?: options.firstOrNull()
    val hasInfo = !sectionInfo.isNullOrBlank() || !selectedOption?.description.isNullOrBlank()
    val isSingleOption = options.size <= 1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSingleOption) {
                OutlinedTextField(
                    value = selectedOption?.label.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = labelColor),
                    colors = fieldColors,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = selectedOption?.label.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = labelColor),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = fieldColors,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .exposedDropdownSize(matchTextFieldWidth = true)
                            .background(menuBackground),
                    ) {
                        options.forEachIndexed { index, option ->
                            val isSelected = option.value == selected
                            DropdownMenuItem(
                                text = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = option.label,
                                                color = when {
                                                    !option.enabled -> mutedLabelColor
                                                    isSelected -> accent
                                                    else -> labelColor
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = option.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = descriptionColor,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                },
                                enabled = option.enabled,
                                onClick = {
                                    onSelected(option.value)
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = labelColor,
                                    disabledTextColor = mutedLabelColor,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = if (isSelected) {
                                    Modifier.background(accent.copy(alpha = 0.10f))
                                } else {
                                    Modifier
                                },
                            )
                            if (index < options.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp,
                                    color = dividerColor,
                                )
                            }
                        }
                    }
                }
            }
            if (hasInfo) {
                IconButton(
                    onClick = { infoExpanded = !infoExpanded },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(
                            R.string.app_settings_option_info_content_description,
                        ),
                        tint = if (infoExpanded) accent else descriptionColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        if (infoExpanded && hasInfo) {
            Spacer(modifier = Modifier.height(8.dp))
            if (!sectionInfo.isNullOrBlank()) {
                Text(
                    text = sectionInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = descriptionColor,
                )
            }
            val selectedDescription = selectedOption?.description
            if (!selectedDescription.isNullOrBlank()) {
                if (!sectionInfo.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = selectedDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = descriptionColor,
                )
            }
        }
    }
}

@Composable
fun settingsDropdownFieldColors(
    accent: Color = Neo.Accent,
    text: Color = Neo.TextPrimary,
    secondary: Color = Neo.TextSecondary,
    container: Color = AppGlass.CardSurface,
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = text,
    unfocusedTextColor = text,
    disabledTextColor = secondary,
    focusedBorderColor = accent,
    unfocusedBorderColor = secondary.copy(alpha = 0.55f),
    focusedTrailingIconColor = accent,
    unfocusedTrailingIconColor = secondary,
    focusedContainerColor = container.copy(alpha = 0.06f),
    unfocusedContainerColor = container.copy(alpha = 0.04f),
    cursorColor = accent,
)

private fun SoftApPerformancePreset.titleRes(): Int = when (this) {
    SoftApPerformancePreset.SMOOTH -> R.string.rc_vehicle_softap_perf_smooth
    SoftApPerformancePreset.BALANCED -> R.string.rc_vehicle_softap_perf_balanced
    SoftApPerformancePreset.HIGH_QUALITY -> R.string.rc_vehicle_softap_perf_high_quality
}

private fun SoftApPerformancePreset.descriptionRes(): Int = when (this) {
    SoftApPerformancePreset.SMOOTH -> R.string.rc_vehicle_softap_perf_smooth_description
    SoftApPerformancePreset.BALANCED -> R.string.rc_vehicle_softap_perf_balanced_description
    SoftApPerformancePreset.HIGH_QUALITY -> R.string.rc_vehicle_softap_perf_high_quality_description
}

private fun SoftApHudProcessingRate.titleRes(): Int = when (this) {
    SoftApHudProcessingRate.AUTO -> R.string.rc_vehicle_softap_hud_rate_auto
    SoftApHudProcessingRate.FPS_5 -> R.string.rc_vehicle_softap_hud_rate_5
    SoftApHudProcessingRate.FPS_8 -> R.string.rc_vehicle_softap_hud_rate_8
    SoftApHudProcessingRate.FPS_10 -> R.string.rc_vehicle_softap_hud_rate_10
    SoftApHudProcessingRate.FPS_12 -> R.string.rc_vehicle_softap_hud_rate_12
    SoftApHudProcessingRate.FPS_15 -> R.string.rc_vehicle_softap_hud_rate_15
    SoftApHudProcessingRate.UNCAPPED -> R.string.rc_vehicle_softap_hud_rate_unlimited
}

private fun SoftApHudProcessingRate.descriptionRes(): Int = when (this) {
    SoftApHudProcessingRate.AUTO -> R.string.rc_vehicle_softap_hud_rate_auto_description
    SoftApHudProcessingRate.FPS_5 -> R.string.rc_vehicle_softap_hud_rate_5_description
    SoftApHudProcessingRate.FPS_8 -> R.string.rc_vehicle_softap_hud_rate_8_description
    SoftApHudProcessingRate.FPS_10 -> R.string.rc_vehicle_softap_hud_rate_10_description
    SoftApHudProcessingRate.FPS_12 -> R.string.rc_vehicle_softap_hud_rate_12_description
    SoftApHudProcessingRate.FPS_15 -> R.string.rc_vehicle_softap_hud_rate_15_description
    SoftApHudProcessingRate.UNCAPPED -> R.string.rc_vehicle_softap_hud_rate_unlimited_description
}
