package com.micsbol.telecon4esp32.ui.applications

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.resolveCameraLinkProfile
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.theme.Neo

/**
 * Shows the matching sketch / documentation links for the current board + connection.
 * Long role explanations live behind the Connection and SoftAP (i) icons.
 */
@Composable
fun ApplicationSettingsSelectionGuideSection(
    applicationId: ApplicationId,
    selectedBoard: Esp32Board,
    selectedMode: BluetoothConnectionMode,
    modifier: Modifier = Modifier,
    useSoftApCamera: Boolean = false,
    titleColor: Color = Neo.TextPrimary,
    bodyColor: Color = Neo.TextSecondary,
    linkColor: Color = Neo.Accent,
    useNeoCard: Boolean = true,
) {
    val context = LocalContext.current
    val cameraProfile = remember(applicationId, selectedBoard, selectedMode, useSoftApCamera) {
        resolveCameraLinkProfile(applicationId, selectedBoard, selectedMode, useSoftApCamera)
    }
    val camKitLabels = cameraProfile == CameraLinkProfile.WIFI_SOFTAP
    val boardLabel = stringResource(
        when (selectedBoard) {
            Esp32Board.DEV_KIT,
            Esp32Board.CAM_AND_DEV_KIT,
            -> R.string.app_settings_device_dev_kit
            Esp32Board.CAM -> R.string.app_settings_device_cam
        },
    )
    val modeOption = connectionModeMenuOption(
        mode = selectedMode,
        canUseAdvanced = true,
        camKitLabels = camKitLabels,
    )
    val selectionTitle = stringResource(
        R.string.app_settings_selection_guide_title_format,
        boardLabel,
        modeOption.label,
    )
    val comingSoonToast = stringResource(R.string.app_settings_selection_guide_links_coming_soon)
    val codeLinkLabels = selectionGuideCodeLinkLabels(
        profile = cameraProfile,
        selectedBoard = selectedBoard,
        selectedMode = selectedMode,
    )

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (useNeoCard) {
                NeoSectionTitle(text = stringResource(R.string.app_settings_selection_guide_section_title))
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text(
                    text = stringResource(R.string.app_settings_selection_guide_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = selectionTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                codeLinkLabels.forEach { label ->
                    SelectionGuideLink(
                        label = label,
                        color = linkColor,
                        onClick = {
                            Toast.makeText(context, comingSoonToast, Toast.LENGTH_SHORT).show()
                        },
                    )
                }
                SelectionGuideLink(
                    label = stringResource(R.string.app_settings_selection_guide_docs_link),
                    color = linkColor,
                    onClick = {
                        Toast.makeText(context, comingSoonToast, Toast.LENGTH_SHORT).show()
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_settings_selection_guide_links_placeholder_note),
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor,
            )
        }
    }

    if (useNeoCard) {
        Column(modifier = modifier.fillMaxWidth()) {
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun selectionGuideCodeLinkLabels(
    profile: CameraLinkProfile,
    selectedBoard: Esp32Board,
    selectedMode: BluetoothConnectionMode,
): List<String> = when (profile) {
    CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT -> listOf(
        stringResource(devkitBluetoothSketchRes(selectedMode)),
        stringResource(R.string.app_settings_selection_guide_code_cam_video),
    )
    CameraLinkProfile.WIFI_SOFTAP -> listOf(
        stringResource(
            if (selectedMode == BluetoothConnectionMode.WIFI_CAM_STARTER) {
                R.string.app_settings_selection_guide_code_cam_starter
            } else {
                R.string.app_settings_selection_guide_code_cam
            },
        ),
    )
    CameraLinkProfile.CONTROL_ONLY -> listOf(
        when {
            selectedMode.isWifiLink -> stringResource(
                R.string.app_settings_selection_guide_code_devkit_wifi,
            )
            selectedMode.isBluetoothLink -> stringResource(devkitBluetoothSketchRes(selectedMode))
            selectedBoard.usesSoftApCamera -> stringResource(
                R.string.app_settings_selection_guide_code_cam,
            )
            else -> stringResource(R.string.app_settings_selection_guide_code_devkit)
        },
    )
}

private fun devkitBluetoothSketchRes(mode: BluetoothConnectionMode): Int = when (mode) {
    BluetoothConnectionMode.CLASSIC_SIMPLE ->
        R.string.app_settings_selection_guide_code_devkit_classic_simple
    BluetoothConnectionMode.CLASSIC_BINARY ->
        R.string.app_settings_selection_guide_code_devkit_classic_binary
    BluetoothConnectionMode.BLE_BINARY ->
        R.string.app_settings_selection_guide_code_devkit_ble
    else -> R.string.app_settings_selection_guide_code_devkit
}

@Composable
private fun SelectionGuideLink(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            color = color,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}
