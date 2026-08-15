package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.SettingsUserType
import com.micsbol.telecon4esp32.domain.model.availableConnectionModes
import com.micsbol.telecon4esp32.domain.model.connectionModesForFamily
import com.micsbol.telecon4esp32.domain.model.preferredConnectionMode
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.ui.applications.CamBoardPerformanceWarningBanner
import com.micsbol.telecon4esp32.ui.applications.SettingsMenuOption
import com.micsbol.telecon4esp32.ui.applications.SettingsOptionDropdown
import com.micsbol.telecon4esp32.ui.applications.SettingsUserTypeSelector
import com.micsbol.telecon4esp32.ui.applications.connectionModeMenuOption
import com.micsbol.telecon4esp32.ui.applications.rememberCamAwareBoardSelection
import com.micsbol.telecon4esp32.ui.applications.settingsDropdownFieldColors
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass

@Composable
fun GreenhouseDeviceSettingsSection(
    selectedBoard: Esp32Board,
    onBoardSelected: (Esp32Board) -> Unit,
    userType: SettingsUserType = SettingsUserType.NORMAL,
    canUseAdvanced: Boolean = true,
    modifier: Modifier = Modifier,
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
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_settings_device_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GreenhouseGlass.TextOnGlassPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GreenhouseSettingsDropdown(
            options = options,
            selected = selectedBoard,
            onSelected = boardSelection.onSelect,
            sectionInfo = stringResource(R.string.app_settings_device_section_description),
        )
        if (boardSelection.showAtRiskBanner) {
            Spacer(modifier = Modifier.height(12.dp))
            CamBoardPerformanceWarningBanner(
                secondaryColor = GreenhouseGlass.TextOnGlassSecondary,
                accent = GreenhouseGlass.WarningOrange,
            )
        }
        boardSelection.WarningDialog()
    }
}

@Composable
fun GreenhouseUserTypeSettingsSection(
    selected: SettingsUserType,
    canUseAdvanced: Boolean,
    onSelected: (SettingsUserType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_settings_user_type_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GreenhouseGlass.TextOnGlassPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingsUserTypeSelector(
            selected = selected,
            canUseAdvanced = canUseAdvanced,
            onSelected = onSelected,
            sectionInfo = stringResource(R.string.app_settings_user_type_section_description),
            fieldColors = settingsDropdownFieldColors(
                accent = GreenhouseGlass.AccentGreen,
                text = GreenhouseGlass.TextOnGlassPrimary,
                secondary = GreenhouseGlass.TextOnGlassSecondary,
                container = GreenhouseGlass.ChipBackground,
            ),
            menuBackground = GreenhouseGlass.BadgeBackground,
            descriptionColor = GreenhouseGlass.TextOnGlassSecondary,
            accent = GreenhouseGlass.AccentGreen,
            dividerColor = GreenhouseGlass.TextOnGlassMuted.copy(alpha = 0.35f),
            labelColor = GreenhouseGlass.TextOnGlassPrimary,
            mutedLabelColor = GreenhouseGlass.TextOnGlassMuted,
        )
    }
}

@Composable
fun GreenhouseProtocolSettingsSection(
    selectedMode: BluetoothConnectionMode,
    userType: SettingsUserType,
    canUseAdvanced: Boolean,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    selectedBoard: Esp32Board = Esp32Board.DEV_KIT,
    modifier: Modifier = Modifier,
) {
    val applicationId = ApplicationId.GREENHOUSE
    val availableModes = applicationId.availableConnectionModes(selectedBoard, userType)
    val camModesOnly = selectedBoard == Esp32Board.CAM
    val kitBModesOnly = selectedBoard.isKitBDual
    val useDevKitLinkPicker = !camModesOnly && !kitBModesOnly
    val includesWifi = applicationId.availableConnectionModes(selectedBoard).any { it.isWifiLink }
    val connectionSectionInfo = stringResource(
        when {
            kitBModesOnly || camModesOnly -> R.string.app_settings_connection_section_description_cam
            useDevKitLinkPicker -> R.string.app_settings_connection_section_description_devkit
            else -> R.string.app_settings_connection_section_description
        },
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                if (includesWifi) {
                    R.string.app_settings_connection_section_title_general
                } else {
                    R.string.app_settings_connection_section_title
                },
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GreenhouseGlass.TextOnGlassPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.app_settings_protocol_prefix_label,
                applicationId.protocolPrefix(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = GreenhouseGlass.TextOnGlassSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (useDevKitLinkPicker) {
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
            val detailModes = applicationId.connectionModesForFamily(
                selectedBoard,
                selectedFamily,
                userType,
            )
            GreenhouseSettingsDropdown(
                options = familyOptions,
                selected = selectedFamily,
                onSelected = { family ->
                    if (family == selectedFamily) return@GreenhouseSettingsDropdown
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
            if (detailModes.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        if (selectedFamily == ConnectionLinkFamily.WIFI) {
                            R.string.app_settings_connection_wifi_protocol_title
                        } else {
                            R.string.app_settings_connection_bluetooth_protocol_title
                        },
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenhouseGlass.TextOnGlassPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                GreenhouseSettingsDropdown(
                    options = detailModes.map { mode ->
                        connectionModeMenuOption(
                            mode = mode,
                            canUseAdvanced = canUseAdvanced,
                            camKitLabels = false,
                        )
                    },
                    selected = selectedMode,
                    onSelected = onModeSelected,
                    sectionInfo = stringResource(
                        if (selectedFamily == ConnectionLinkFamily.WIFI) {
                            R.string.app_settings_connection_wifi_protocol_description
                        } else {
                            R.string.app_settings_connection_bluetooth_protocol_description
                        },
                    ),
                )
            }
        } else {
            GreenhouseSettingsDropdown(
                options = availableModes.map { mode ->
                    connectionModeMenuOption(
                        mode = mode,
                        canUseAdvanced = canUseAdvanced,
                        camKitLabels = true,
                    )
                },
                selected = selectedMode,
                onSelected = onModeSelected,
                sectionInfo = connectionSectionInfo,
            )
        }
    }
}

@Composable
private fun <T> GreenhouseSettingsDropdown(
    options: List<SettingsMenuOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    sectionInfo: String? = null,
) {
    SettingsOptionDropdown(
        options = options,
        selected = selected,
        onSelected = onSelected,
        sectionInfo = sectionInfo,
        fieldColors = settingsDropdownFieldColors(
            accent = GreenhouseGlass.AccentGreen,
            text = GreenhouseGlass.TextOnGlassPrimary,
            secondary = GreenhouseGlass.TextOnGlassSecondary,
            container = GreenhouseGlass.ChipBackground,
        ),
        menuBackground = GreenhouseGlass.BadgeBackground,
        descriptionColor = GreenhouseGlass.TextOnGlassSecondary,
        accent = GreenhouseGlass.AccentGreen,
        dividerColor = GreenhouseGlass.TextOnGlassMuted.copy(alpha = 0.35f),
        labelColor = GreenhouseGlass.TextOnGlassPrimary,
        mutedLabelColor = GreenhouseGlass.TextOnGlassMuted,
    )
}
