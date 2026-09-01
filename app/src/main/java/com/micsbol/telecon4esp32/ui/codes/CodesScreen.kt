package com.micsbol.telecon4esp32.ui.codes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.availableConnectionModes
import com.micsbol.telecon4esp32.domain.model.showSoftApCameraOverlaySetting
import com.micsbol.telecon4esp32.domain.model.usesCamera
import com.micsbol.telecon4esp32.ui.applications.SettingsMenuOption
import com.micsbol.telecon4esp32.ui.applications.SettingsOptionDropdown
import com.micsbol.telecon4esp32.ui.applications.SoftApCameraOverlaySettingsSection
import com.micsbol.telecon4esp32.ui.applications.connectionModeMenuOption
import com.micsbol.telecon4esp32.ui.applications.navigateToApplicationSettings
import com.micsbol.telecon4esp32.ui.applications.titleRes
import com.micsbol.telecon4esp32.util.hostedPdfUrl
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoIconBadge
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.theme.Neo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private enum class PdfOpenResult {
    Success,
    NoActivity,
    CopyFailed
}

private enum class PdfDialogKind {
    NoReader,
    CopyFailed
}

private enum class ZipSharePrepareResult {
    Success,
    NoActivity,
    CopyFailed
}

private enum class ZipShareErrorDialog {
    NoApp,
    CopyFailed
}

private fun shareZipAssetExternally(
    context: Context,
    assetFileName: String,
    outputFileName: String
): ZipSharePrepareResult {
    return try {
        val tempFile = File(context.cacheDir, outputFileName)
        context.assets.open(assetFileName).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.codes_zip_share_chooser_title)
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
            ZipSharePrepareResult.Success
        } catch (_: ActivityNotFoundException) {
            ZipSharePrepareResult.NoActivity
        }
    } catch (_: IOException) {
        ZipSharePrepareResult.CopyFailed
    }
}

private fun openPdfUrlExternally(context: Context, url: String): PdfOpenResult {
    return try {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(hostedPdfUrl(url)))
        context.startActivity(viewIntent)
        PdfOpenResult.Success
    } catch (_: ActivityNotFoundException) {
        PdfOpenResult.NoActivity
    }
}

private fun openPdfAssetExternally(context: Context, assetFileName: String): PdfOpenResult {
    return try {
        val tempFile = File(context.cacheDir, assetFileName)
        context.assets.open(assetFileName).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        val chooser = Intent.createChooser(
            viewIntent,
            context.getString(R.string.codes_pdf_open_with)
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
            PdfOpenResult.Success
        } catch (_: ActivityNotFoundException) {
            PdfOpenResult.NoActivity
        }
    } catch (_: IOException) {
        PdfOpenResult.CopyFailed
    }
}

private fun Context.launchPlayStorePdfReaderSearch() {
    val query = "pdf reader"
    val marketUri = Uri.parse(
        "market://search?q=${Uri.encode(query)}&c=apps"
    )
    val webUri = Uri.parse(
        "https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps"
    )
    try {
        startActivity(Intent(Intent.ACTION_VIEW, marketUri))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodesScreen(
    navController: NavController,
    applicationId: ApplicationId,
    viewModel: CodesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pdfDialogKind by remember { mutableStateOf<PdfDialogKind?>(null) }
    var pendingZipExport by remember { mutableStateOf<CodeAssetInfo?>(null) }
    var zipShareError by remember { mutableStateOf<ZipShareErrorDialog?>(null) }

    pdfDialogKind?.let { kind ->
        NeoDialog(
            onDismissRequest = { pdfDialogKind = null },
            title = {
                NeoDialogTitle(
                    text = stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_title
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_title
                        }
                    )
                )
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_message
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_message
                        }
                    )
                )
            },
            actions = {
                when (kind) {
                    PdfDialogKind.NoReader -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NeoSecondaryButton(
                                text = stringResource(R.string.codes_pdf_cancel),
                                onClick = { pdfDialogKind = null },
                                compact = true,
                            )
                            NeoPillButton(
                                text = stringResource(R.string.codes_pdf_find_reader),
                                onClick = {
                                    context.launchPlayStorePdfReaderSearch()
                                    pdfDialogKind = null
                                },
                                compact = true,
                            )
                        }
                    }

                    PdfDialogKind.CopyFailed -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            NeoPillButton(
                                text = stringResource(R.string.codes_dialog_ok),
                                onClick = { pdfDialogKind = null },
                                compact = true,
                            )
                        }
                    }
                }
            }
        )
    }

    pendingZipExport?.let { asset ->
        val zipFileName = checkNotNull(asset.assetFileName)
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
                            when (
                                shareZipAssetExternally(
                                    context,
                                    zipFileName,
                                    asset.outputFileName
                                )
                            ) {
                                ZipSharePrepareResult.Success -> Unit
                                ZipSharePrepareResult.NoActivity ->
                                    zipShareError = ZipShareErrorDialog.NoApp

                                ZipSharePrepareResult.CopyFailed ->
                                    zipShareError = ZipShareErrorDialog.CopyFailed
                            }
                        },
                        fillMaxWidth = true,
                    )
                    NeoSecondaryButton(
                        text = stringResource(R.string.codes_zip_export_save),
                        onClick = {
                            pendingZipExport = null
                            viewModel.saveZipAsset(
                                assetFileName = zipFileName,
                                outputFileName = asset.outputFileName
                            )
                        },
                        fillMaxWidth = true,
                    )
                    NeoDialogTextAction(
                        text = stringResource(R.string.codes_pdf_cancel),
                        onClick = { pendingZipExport = null },
                    )
                }
            }
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
            }
        )
    }

    val navigateBack: () -> Unit = { navController.navigateUp() }

    BackHandler(onBack = navigateBack)

    NeoScaffold(
        title = stringResource(applicationId.titleRes()),
        onNavigateBack = navigateBack,
    ) { paddingValues ->
        CodesFilteredContent(
            modifier = Modifier.padding(paddingValues),
            applicationId = applicationId,
            viewModel = viewModel,
            uiState = uiState,
            onOpenSettings = {
                navController.navigateToApplicationSettings(applicationId)
            },
            onAssetClick = { asset ->
                when (asset.type) {
                    CodeAssetType.Pdf -> {
                        val result = when {
                            asset.remoteUrlRes != null ->
                                openPdfUrlExternally(context, context.getString(asset.remoteUrlRes))

                            asset.assetFileName != null ->
                                openPdfAssetExternally(context, asset.assetFileName)

                            else -> PdfOpenResult.CopyFailed
                        }
                        when (result) {
                            PdfOpenResult.Success -> Unit
                            PdfOpenResult.NoActivity ->
                                pdfDialogKind = PdfDialogKind.NoReader

                            PdfOpenResult.CopyFailed ->
                                pdfDialogKind = PdfDialogKind.CopyFailed
                        }
                    }

                    CodeAssetType.Zip -> {
                        if (asset.isPublished) {
                            pendingZipExport = asset
                        }
                    }
                }
            },
            onDismissSaveError = {
                viewModel.dismissSaveError()
            },
        )
    }
}

@Composable
private fun CodesFilteredContent(
    modifier: Modifier = Modifier,
    applicationId: ApplicationId,
    viewModel: CodesViewModel,
    uiState: CodesUiState,
    onOpenSettings: () -> Unit,
    onAssetClick: (CodeAssetInfo) -> Unit,
    onDismissSaveError: () -> Unit,
) {
    val filter by viewModel.filter.collectAsState()
    val currentLanguage = currentCodeAssetLanguage()
    val camKitLabels = applicationId.usesCamera() && filter.board == Esp32Board.CAM
    val showDevicePicker = applicationId.usesCamera()
    val availableModes = remember(applicationId, filter.board) {
        applicationId.availableConnectionModes(filter.board)
    }
    val matchedAssets = remember(
        applicationId,
        currentLanguage,
        filter.board,
        filter.mode,
        filter.useSoftApCamera,
    ) {
        codeAssetsMatching(
            applicationId,
            currentLanguage,
            filter.board,
            filter.mode,
            filter.useSoftApCamera,
        )
    }
    val matchedZipAssets = matchedAssets.filter { it.type == CodeAssetType.Zip }
    val documentationAssets = matchedAssets.filter {
        it.type == CodeAssetType.Pdf && it.isPublished
    }
    val isRoleAOverlay = filter.board.isKitBDual ||
        (
            filter.board.normalizedControlBoard() == Esp32Board.DEV_KIT &&
                filter.useSoftApCamera &&
                filter.mode.isBluetoothLink
            )
    val isRoleBCam = applicationId.usesCamera() && filter.board == Esp32Board.CAM
    val boardLabel = stringResource(
        when (filter.board) {
            Esp32Board.DEV_KIT -> R.string.app_settings_device_dev_kit
            Esp32Board.CAM -> R.string.app_settings_device_cam
            Esp32Board.CAM_AND_DEV_KIT -> R.string.app_settings_device_cam_and_dev_kit
        },
    )
    val modeLabel = connectionModeMenuOption(
        mode = filter.mode,
        canUseAdvanced = true,
        camKitLabels = camKitLabels,
    ).label

    val saveError = uiState.saveError
    if (saveError != null) {
        NeoDialog(
            onDismissRequest = onDismissSaveError,
            title = {
                NeoDialogTitle(
                    text = stringResource(R.string.codes_zip_save_error_title),
                    color = Neo.Negative,
                )
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(
                        R.string.codes_zip_save_error_details,
                        saveError,
                    ),
                )
            },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    NeoPillButton(
                        text = stringResource(R.string.codes_dialog_ok),
                        onClick = onDismissSaveError,
                        compact = true,
                    )
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CodesSettingsFirstBanner(
                message = stringResource(R.string.codes_settings_first_banner),
                actionLabel = stringResource(R.string.codes_open_settings),
                onAction = onOpenSettings,
            )
        }
        if (showDevicePicker) {
            item {
                SettingsOptionDropdown(
                    options = listOf(
                        SettingsMenuOption(
                            value = Esp32Board.DEV_KIT,
                            label = stringResource(R.string.app_settings_device_dev_kit),
                            description = stringResource(R.string.app_settings_device_dev_kit_description),
                        ),
                        SettingsMenuOption(
                            value = Esp32Board.CAM,
                            label = stringResource(R.string.app_settings_device_cam),
                            description = stringResource(R.string.app_settings_device_cam_description),
                        ),
                    ),
                    selected = filter.board.normalizedControlBoard(),
                    onSelected = viewModel::onBoardSelected,
                    label = stringResource(R.string.codes_filter_device_title),
                    sectionInfo = stringResource(R.string.codes_filter_device_description),
                )
            }
        }
        if (showSoftApCameraOverlaySetting(applicationId, filter.board)) {
            item {
                SoftApCameraOverlaySettingsSection(
                    enabled = filter.useSoftApCamera,
                    onEnabledChange = viewModel::onUseSoftApCameraSelected,
                )
            }
        }
        item {
            SettingsOptionDropdown(
                options = availableModes.map { mode ->
                    connectionModeMenuOption(
                        mode = mode,
                        canUseAdvanced = true,
                        camKitLabels = camKitLabels,
                    )
                },
                selected = filter.mode,
                onSelected = viewModel::onModeSelected,
                label = stringResource(R.string.codes_filter_mode_title),
                sectionInfo = buildString {
                    append(stringResource(R.string.codes_filter_mode_description))
                    if (isRoleAOverlay) {
                        append("\n\n")
                        append(stringResource(R.string.codes_filter_role_a_note))
                    } else if (isRoleBCam) {
                        append("\n\n")
                        append(stringResource(R.string.codes_filter_role_b_note))
                    }
                },
            )
        }

        if (matchedZipAssets.isNotEmpty()) {
            items(matchedZipAssets, key = { it.id ?: it.assetFileName ?: it.titleRes }) { asset ->
                if (asset.isPublished) {
                    val zipMessage = when {
                        uiState.isSavingZip -> stringResource(
                            R.string.codes_zip_saving_message,
                            asset.outputFileName,
                        )
                        uiState.savedZipLocation != null -> stringResource(
                            R.string.codes_zip_saved_message,
                            asset.outputFileName,
                            uiState.savedZipLocation,
                        )
                        else -> {
                            val device = asset.targetDeviceLabelRes?.let { stringResource(it) }
                            if (device != null) {
                                stringResource(
                                    R.string.app_settings_selection_guide_title_format,
                                    device,
                                    asset.outputFileName,
                                )
                            } else {
                                stringResource(R.string.codes_zip_card_hint, asset.outputFileName)
                            }
                        }
                    }
                    CodeAssetGridItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        assetInfo = asset,
                        supportingText = zipMessage,
                        isLoading = uiState.isSavingZip,
                        onClick = { onAssetClick(asset) },
                    )
                } else {
                    ComingSoonCodeCard(
                        label = stringResource(
                            R.string.codes_coming_soon_for_device,
                            stringResource(asset.titleRes),
                        ),
                    )
                }
            }
        } else {
            item {
                NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Text(
                        text = stringResource(R.string.codes_matching_empty_title),
                        color = Neo.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.codes_matching_empty_message,
                            boardLabel,
                            modeLabel,
                        ),
                        color = Neo.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        items(documentationAssets, key = { "doc-${it.assetFileName}-${it.remoteUrlRes}" }) { asset ->
            CodeAssetGridItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                assetInfo = asset,
                supportingText = null,
                isLoading = false,
                onClick = { onAssetClick(asset) },
            )
        }
    }
}

@Composable
fun CodesSettingsFirstBanner(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val warning = Neo.Warning
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(warning.copy(alpha = 0.20f))
            .border(1.5.dp, warning.copy(alpha = 0.65f), shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = warning,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = Neo.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(10.dp))
            NeoPillButton(
                text = actionLabel,
                onClick = onAction,
                fillMaxWidth = true,
                compact = true,
            )
        }
    }
}

@Composable
private fun ComingSoonCodeCard(label: String) {
    NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Text(
            text = label,
            color = Neo.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun CodeAssetGridItem(
    modifier: Modifier = Modifier,
    assetInfo: CodeAssetInfo,
    supportingText: String?,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(assetInfo.titleRes)

    NeoCard(
        modifier = modifier
            .clickable(enabled = !isLoading, onClick = onClick),
        contentPadding = 14.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Neo.Accent,
                )
            } else {
                NeoIconBadge(icon = assetInfo.icon, size = 56.dp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Neo.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = supportingText,
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
