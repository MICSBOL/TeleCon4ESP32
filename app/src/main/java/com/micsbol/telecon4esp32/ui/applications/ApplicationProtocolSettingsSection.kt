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
import androidx.compose.runtime.key
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
import com.micsbol.telecon4esp32.domain.model.CameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.availableConnectionModes
import com.micsbol.telecon4esp32.domain.model.coerceForSettings
import com.micsbol.telecon4esp32.domain.model.connectionModesForFamily
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.model.supportsCamVideoControl
import com.micsbol.telecon4esp32.domain.model.usesCamera
import com.micsbol.telecon4esp32.domain.model.usesSoftApCamera
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.home.TutorialAnchor
import com.micsbol.telecon4esp32.ui.home.reportTutorialAnchor
import com.micsbol.telecon4esp32.ui.components.NeoToggle
import com.micsbol.telecon4esp32.ui.components.settingsChromeAccent
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
) {
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
    }
    val boardSelection = rememberCamAwareBoardSelection(
        selectedBoard = selectedBoard,
        onBoardSelected = onBoardSelected,
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsOptionDropdown(
            options = options,
            selected = selectedBoard,
            onSelected = boardSelection.onSelect,
            label = stringResource(R.string.app_settings_device_section_title),
            sectionInfo = stringResource(R.string.app_settings_device_section_description),
        )
        if (boardSelection.showAtRiskBanner) {
            Spacer(modifier = Modifier.height(12.dp))
            CamBoardPerformanceWarningBanner()
        }
        boardSelection.WarningDialog()
    }
}

@Composable
fun CameraHardwareRoleSettingsSection(
    selected: CameraHardwareRole,
    onRoleSelected: (CameraHardwareRole) -> Unit,
    allowTwoDevices: Boolean = false,
    allowOneCam: Boolean = true,
    allowCameraRoles: Boolean = true,
    userType: SettingsUserType = SettingsUserType.NORMAL,
) {
    val context = LocalContext.current
    val atRisk = remember(context) { context.isCameraStreamAtRisk() }
    var pendingRole by remember { mutableStateOf<CameraHardwareRole?>(null) }
    val visibleSelected = selected.coerceForSettings(
        userType = userType,
        centerCameraEnabled = allowCameraRoles,
    )
    val useAdvancedCamLabels = userType == SettingsUserType.ADVANCED
    val options = buildList {
        if (allowCameraRoles) {
            if (allowOneCam) {
                add(
                    SettingsMenuOption(
                        value = CameraHardwareRole.ONE_CAM,
                        label = stringResource(
                            if (useAdvancedCamLabels) {
                                R.string.app_settings_cam_role_one_device_advanced
                            } else {
                                R.string.app_settings_cam_role_one_device
                            },
                        ),
                        description = stringResource(
                            if (useAdvancedCamLabels) {
                                R.string.app_settings_cam_role_one_device_advanced_description
                            } else {
                                R.string.app_settings_cam_role_one_device_description
                            },
                        ),
                    ),
                )
            }
            if (allowTwoDevices) {
                add(
                    SettingsMenuOption(
                        value = CameraHardwareRole.TWO_DEVICES,
                        label = stringResource(
                            if (useAdvancedCamLabels) {
                                R.string.app_settings_cam_role_two_devices
                            } else {
                                R.string.app_settings_cam_role_two_devices_simple
                            },
                        ),
                        description = stringResource(
                            if (useAdvancedCamLabels) {
                                R.string.app_settings_cam_role_two_devices_description
                            } else {
                                R.string.app_settings_cam_role_two_devices_simple_description
                            },
                        ),
                    ),
                )
            }
        }
        add(
            SettingsMenuOption(
                value = CameraHardwareRole.NO_CAM,
                label = stringResource(R.string.app_settings_cam_role_no_cam),
                description = stringResource(R.string.app_settings_cam_role_no_cam_description),
            ),
        )
    }
    val onSelect: (CameraHardwareRole) -> Unit = { role ->
        if (role.usesSoftApCamera && atRisk && !visibleSelected.usesSoftApCamera) {
            pendingRole = role
        } else {
            onRoleSelected(role)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsOptionDropdown(
            options = options,
            selected = visibleSelected,
            onSelected = onSelect,
            label = stringResource(R.string.app_settings_cam_role_section_title),
            sectionInfo = stringResource(R.string.app_settings_cam_role_section_description),
        )
        if (visibleSelected.usesSoftApCamera && atRisk) {
            Spacer(modifier = Modifier.height(12.dp))
            CamBoardPerformanceWarningBanner()
        }
    }
    if (pendingRole != null) {
        CamBoardPerformanceWarningDialog(
            onConfirm = {
                val role = pendingRole
                pendingRole = null
                if (role != null) onRoleSelected(role)
            },
            onDismiss = { pendingRole = null },
        )
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
fun SoftApCameraOverlaySettingsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val atRisk = remember(context) { context.isCameraStreamAtRisk() }
    var pendingEnableConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        NeoSectionTitle(text = stringResource(R.string.app_settings_softap_camera_overlay_title))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_settings_softap_camera_overlay_description),
                style = MaterialTheme.typography.bodySmall,
                color = Neo.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            NeoToggle(
                checked = enabled,
                onCheckedChange = { checked ->
                    if (checked && atRisk && !enabled) {
                        pendingEnableConfirm = true
                    } else {
                        onEnabledChange(checked)
                    }
                },
            )
        }
        if (enabled && atRisk) {
            Spacer(modifier = Modifier.height(12.dp))
            CamBoardPerformanceWarningBanner()
        }
    }
    if (pendingEnableConfirm) {
        CamBoardPerformanceWarningDialog(
            onConfirm = {
                pendingEnableConfirm = false
                onEnabledChange(true)
            },
            onDismiss = { pendingEnableConfirm = false },
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
    allowWifiControl: Boolean = true,
    belowConnectionTypeContent: (@Composable () -> Unit)? = null,
) {
    val availableModes = applicationId.availableConnectionModes(selectedBoard, userType)
    val camModesOnly =
        applicationId.supportsCamVideoControl() && selectedBoard == Esp32Board.CAM
    val useDevKitLinkPicker = !camModesOnly
    val connectionSectionInfo = stringResource(
        when {
            camModesOnly -> R.string.app_settings_connection_section_description_cam
            useDevKitLinkPicker -> R.string.app_settings_connection_section_description_devkit
            else -> R.string.app_settings_connection_section_description
        },
    )

    val connectionTitle = stringResource(R.string.app_settings_connection_section_title_general)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .reportTutorialAnchor(TutorialAnchor.SETTINGS_CONNECTION),
    ) {
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
                connectionLabel = connectionTitle,
                connectionSectionInfo = connectionSectionInfo,
                allowWifiControl = allowWifiControl,
                belowConnectionTypeContent = belowConnectionTypeContent,
            )
        } else {
            val options = availableModes.map { mode ->
                connectionModeMenuOption(
                    mode = mode,
                    canUseAdvanced = canUseAdvanced,
                    camKitLabels = camModesOnly,
                )
            }
            SettingsOptionDropdown(
                options = options,
                selected = selectedMode,
                onSelected = onModeSelected,
                label = connectionTitle,
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
    onRequestAdvancedUnlock: (() -> Unit)? = null,
    fieldColors: TextFieldColors = settingsDropdownFieldColors(),
    menuBackground: Color = AppGlass.DialogSurface,
    descriptionColor: Color = Neo.TextSecondary,
    accent: Color = settingsChromeAccent(),
    dividerColor: Color = Neo.TextSecondary.copy(alpha = 0.28f),
    labelColor: Color = Neo.TextPrimary,
    mutedLabelColor: Color = Neo.TextMuted,
) {
    // Locked unlock affordance lives in the settings top bar (lock next to title).
    if (!canUseAdvanced) return

    SettingsUserTypeSelector(
        selected = selected,
        canUseAdvanced = true,
        onSelected = onSelected,
        modifier = modifier,
        onRequestAdvancedUnlock = onRequestAdvancedUnlock,
        label = stringResource(R.string.app_settings_user_type_section_title),
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

@Composable
fun SettingsUserTypeSelector(
    selected: SettingsUserType,
    canUseAdvanced: Boolean,
    onSelected: (SettingsUserType) -> Unit,
    modifier: Modifier = Modifier,
    onRequestAdvancedUnlock: (() -> Unit)? = null,
    label: String? = null,
    sectionInfo: String? = null,
    fieldColors: TextFieldColors = settingsDropdownFieldColors(),
    menuBackground: Color = AppGlass.DialogSurface,
    descriptionColor: Color = Neo.TextSecondary,
    accent: Color = settingsChromeAccent(),
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
        onSelected = { type ->
            if (type == SettingsUserType.ADVANCED && !canUseAdvanced) {
                onRequestAdvancedUnlock?.invoke()
            } else {
                onSelected(type)
            }
        },
        modifier = modifier,
        label = label,
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
    connectionLabel: String,
    connectionSectionInfo: String,
    allowWifiControl: Boolean = true,
    belowConnectionTypeContent: (@Composable () -> Unit)? = null,
) {
    val selectedFamily = if (allowWifiControl) {
        selectedMode.linkFamily
    } else {
        ConnectionLinkFamily.BLUETOOTH
    }
    val familyOptions = buildList {
        add(
            SettingsMenuOption(
                value = ConnectionLinkFamily.BLUETOOTH,
                label = stringResource(R.string.app_settings_connection_link_bluetooth),
                description = stringResource(R.string.app_settings_connection_link_bluetooth_description),
            ),
        )
        if (allowWifiControl) {
            add(
                SettingsMenuOption(
                    value = ConnectionLinkFamily.WIFI,
                    label = stringResource(R.string.app_settings_connection_link_wifi),
                    description = stringResource(R.string.app_settings_connection_link_wifi_description),
                ),
            )
        }
    }
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
        label = connectionLabel,
        sectionInfo = connectionSectionInfo,
    )
    // Normal (and any single-protocol link) has nothing to choose; skip the section.
    if (detailOptions.size > 1) {
        Spacer(modifier = Modifier.height(16.dp))
        SettingsOptionDropdown(
            options = detailOptions,
            selected = selectedMode,
            onSelected = onModeSelected,
            label = stringResource(
                if (selectedFamily == ConnectionLinkFamily.WIFI) {
                    R.string.app_settings_connection_wifi_protocol_title
                } else {
                    R.string.app_settings_connection_bluetooth_protocol_title
                },
            ),
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
        SettingsOptionDropdown(
            options = presetOptions,
            selected = selectedPreset,
            onSelected = onPresetSelected,
            label = stringResource(R.string.rc_vehicle_softap_perf_section_title),
            sectionInfo = stringResource(R.string.rc_vehicle_softap_perf_section_body),
        )
        Spacer(modifier = Modifier.height(20.dp))
        SettingsOptionDropdown(
            options = hudOptions,
            selected = selectedHudRate,
            onSelected = onHudRateSelected,
            label = stringResource(R.string.rc_vehicle_softap_hud_rate_section_title),
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
        label = stringResource(R.string.app_settings_connection_ble_binary),
        description = if (canUseAdvanced) {
            stringResource(R.string.app_settings_connection_ble_binary_description)
        } else {
            stringResource(R.string.app_settings_protocol_advanced_premium_required)
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
    label: String? = null,
    sectionInfo: String? = null,
    fieldColors: TextFieldColors = settingsDropdownFieldColors(accent = settingsChromeAccent()),
    menuBackground: Color = AppGlass.DialogSurface,
    descriptionColor: Color = Neo.TextSecondary,
    accent: Color = settingsChromeAccent(),
    dividerColor: Color = Neo.TextSecondary.copy(alpha = 0.28f),
    labelColor: Color = Neo.TextPrimary,
    mutedLabelColor: Color = Neo.TextMuted,
) {
    var expanded by remember { mutableStateOf(false) }
    var infoExpanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.value == selected } ?: options.firstOrNull()
    val hasInfo = !sectionInfo.isNullOrBlank() || !selectedOption?.description.isNullOrBlank()
    val isSingleOption = options.size <= 1

    key(selected, selectedOption?.label) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSingleOption) {
                val selectedLabel = selectedOption?.label.orEmpty()
                val summary = if (!label.isNullOrBlank() && selectedLabel.isNotBlank()) {
                    stringResource(R.string.app_settings_fixed_option_format, label, selectedLabel)
                } else {
                    selectedLabel.ifBlank { label.orEmpty() }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    modifier = Modifier.weight(1f),
                )
            } else {
                val fieldLabel: (@Composable () -> Unit)? = if (label.isNullOrBlank()) {
                    null
                } else {
                    {
                        Text(
                            text = label,
                            color = if (expanded) accent else Neo.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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
                        label = fieldLabel,
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
}

@Composable
fun settingsDropdownFieldColors(
    accent: Color = settingsChromeAccent(),
    text: Color = Neo.TextPrimary,
    secondary: Color = Neo.TextSecondary,
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = text,
    unfocusedTextColor = text,
    disabledTextColor = secondary,
    focusedBorderColor = accent,
    unfocusedBorderColor = secondary.copy(alpha = 0.55f),
    focusedTrailingIconColor = accent,
    unfocusedTrailingIconColor = secondary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedLabelColor = accent,
    unfocusedLabelColor = Neo.TextPrimary,
    disabledLabelColor = Neo.TextMuted,
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
