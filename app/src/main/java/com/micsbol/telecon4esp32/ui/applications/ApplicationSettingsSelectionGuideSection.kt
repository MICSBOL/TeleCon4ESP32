package com.micsbol.telecon4esp32.ui.applications

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.resolveCameraLinkProfile
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.ui.codes.CodeAssetInfo
import com.micsbol.telecon4esp32.ui.codes.CodePackageExportViewModel
import com.micsbol.telecon4esp32.ui.codes.ZipExportEvent
import com.micsbol.telecon4esp32.ui.codes.codePackageAssetsMatching
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.components.settingsChromeAccent
import com.micsbol.telecon4esp32.ui.home.LocalFirstConnectionTutorial
import com.micsbol.telecon4esp32.ui.home.TutorialAnchor
import com.micsbol.telecon4esp32.ui.home.reportTutorialAnchor
import com.micsbol.telecon4esp32.ui.theme.Neo

private enum class ZipShareErrorDialog {
    NoApp,
    CopyFailed,
}


/**
 * Shows the matching sketch packages for the current board + connection.
 * Long role explanations live behind the Connection and SoftAP (i) icons.
 * Documentation PDFs open from the module docs button, not here.
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
    linkColor: Color = settingsChromeAccent(),
    useNeoCard: Boolean = true,
    exportViewModel: CodePackageExportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val exportState by exportViewModel.uiState.collectAsState()
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
    val matchedPackages = remember(
        applicationId,
        selectedBoard,
        selectedMode,
        useSoftApCamera,
    ) {
        codePackageAssetsMatching(
            applicationId = applicationId,
            board = selectedBoard,
            mode = selectedMode,
            useSoftApCamera = useSoftApCamera,
        )
    }
    val fallbackLabels = if (matchedPackages.isEmpty()) {
        selectionGuideCodeLinkLabels(
            profile = cameraProfile,
            selectedBoard = selectedBoard,
            selectedMode = selectedMode,
        )
    } else {
        emptyList()
    }

    var pendingZipExport by remember { mutableStateOf<CodeAssetInfo?>(null) }
    var zipShareError by remember { mutableStateOf<ZipShareErrorDialog?>(null) }
    val tutorial = LocalFirstConnectionTutorial.current
    LaunchedEffect(pendingZipExport) {
        tutorial?.onCodeExportDialogChanged(pendingZipExport != null)
    }
    LaunchedEffect(exportState.savedZipLocation) {
        if (exportState.savedZipLocation != null) {
            tutorial?.onMatchingCodeSaved()
        }
    }
    LaunchedEffect(exportViewModel) {
        exportViewModel.events.collect { event ->
            when (event) {
                ZipExportEvent.Shared -> tutorial?.onMatchingCodeSaved()
                ZipExportEvent.NoShareApp -> zipShareError = ZipShareErrorDialog.NoApp
                ZipExportEvent.Failed -> zipShareError = ZipShareErrorDialog.CopyFailed
            }
        }
    }

    pendingZipExport?.let { asset ->
        NeoDialog(
            onDismissRequest = { pendingZipExport = null },
            title = { NeoDialogTitle(text = stringResource(R.string.codes_zip_export_title)) },
            subtitle = { NeoDialogBody(text = stringResource(R.string.codes_zip_export_message)) },
            actions = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NeoPillButton(
                        text = stringResource(R.string.codes_zip_export_share),
                        onClick = {
                            pendingZipExport = null
                            exportViewModel.shareZip(asset)
                        },
                        fillMaxWidth = true,
                    )
                    NeoSecondaryButton(
                        text = stringResource(R.string.codes_zip_export_save),
                        onClick = {
                            pendingZipExport = null
                            exportViewModel.saveZip(asset)
                        },
                        fillMaxWidth = true,
                    )
                    NeoDialogTextAction(
                        text = stringResource(R.string.codes_pdf_cancel),
                        onClick = { pendingZipExport = null },
                    )
                }
            },
        )
    }

    zipShareError?.let { kind ->
        NeoDialog(
            onDismissRequest = { zipShareError = null },
            title = {
                NeoDialogTitle(
                    text = stringResource(
                        when (kind) {
                            ZipShareErrorDialog.NoApp -> R.string.codes_zip_share_no_app_title
                            ZipShareErrorDialog.CopyFailed ->
                                R.string.codes_zip_share_prepare_failed_title
                        }
                    )
                )
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(
                        when (kind) {
                            ZipShareErrorDialog.NoApp -> R.string.codes_zip_share_no_app_message
                            ZipShareErrorDialog.CopyFailed ->
                                R.string.codes_zip_share_prepare_failed_message
                        }
                    )
                )
            },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    NeoPillButton(
                        text = stringResource(R.string.codes_dialog_ok),
                        onClick = { zipShareError = null },
                        compact = true,
                    )
                }
            },
        )
    }

    exportState.saveError?.let { error ->
        NeoDialog(
            onDismissRequest = exportViewModel::dismissSaveError,
            title = {
                NeoDialogTitle(
                    text = stringResource(R.string.codes_zip_save_error_title),
                    color = Neo.Negative,
                )
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(R.string.codes_zip_save_error_details, error),
                )
            },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    NeoPillButton(
                        text = stringResource(R.string.codes_dialog_ok),
                        onClick = exportViewModel::dismissSaveError,
                        compact = true,
                    )
                }
            },
        )
    }

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
            if (exportState.isSavingZip) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.codes_zip_downloading_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                )
            } else if (exportState.savedZipLocation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.codes_zip_saved_message,
                        "ZIP",
                        exportState.savedZipLocation.orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .reportTutorialAnchor(TutorialAnchor.SETTINGS_MATCHING_CODE),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                matchedPackages.forEach { asset ->
                    SelectionGuideLink(
                        label = stringResource(asset.titleRes),
                        color = linkColor,
                        onClick = {
                            if (asset.isPublished &&
                                (asset.assetFileName != null || !asset.remoteZipUrl.isNullOrBlank())
                            ) {
                                pendingZipExport = asset
                            } else {
                                Toast.makeText(context, comingSoonToast, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
                fallbackLabels.forEach { label ->
                    SelectionGuideLink(
                        label = label,
                        color = linkColor,
                        onClick = {
                            Toast.makeText(context, comingSoonToast, Toast.LENGTH_SHORT).show()
                        },
                    )
                }
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
