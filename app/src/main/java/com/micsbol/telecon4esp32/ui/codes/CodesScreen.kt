package com.micsbol.telecon4esp32.ui.codes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
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
import androidx.compose.ui.unit.sp
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
        CodeAssetGrid(
            modifier = Modifier.padding(paddingValues),
            applicationId = applicationId,
            uiState = uiState,
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
                        pendingZipExport = asset
                    }
                }
            },
            onDismissSaveError = {
                viewModel.dismissSaveError()
            }
        )
    }
}

@Composable
private fun CodeAssetGrid(
    modifier: Modifier = Modifier,
    applicationId: ApplicationId,
    uiState: CodesUiState,
    onAssetClick: (CodeAssetInfo) -> Unit,
    onDismissSaveError: () -> Unit,
) {
    val currentLanguage = currentCodeAssetLanguage()
    val availableAssets = remember(applicationId, currentLanguage) {
        codeAssetsFor(applicationId, currentLanguage)
    }

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
                        saveError
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
                        onClick = onDismissSaveError,
                        compact = true,
                    )
                }
            }
        )
    }

    if (availableAssets.isEmpty()) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            NeoIconBadge(icon = Icons.Default.DocumentScanner, size = 56.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.application_codes_empty_title),
                color = Neo.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.application_codes_empty_message),
                color = Neo.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        availableAssets.forEach { asset ->
            val zipMessage = when {
                asset.type != CodeAssetType.Zip -> null
                uiState.isSavingZip -> stringResource(
                    R.string.codes_zip_saving_message,
                    asset.outputFileName
                )

                uiState.savedZipLocation != null -> stringResource(
                    R.string.codes_zip_saved_message,
                    asset.outputFileName,
                    uiState.savedZipLocation
                )

                else -> stringResource(
                    R.string.codes_zip_card_hint,
                    asset.outputFileName
                )
            }

            CodeAssetGridItem(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                assetInfo = asset,
                supportingText = zipMessage,
                isLoading = asset.type == CodeAssetType.Zip && uiState.isSavingZip,
                onClick = { onAssetClick(asset) }
            )
        }
    }
}

@Composable
private fun CodeAssetGridItem(
    modifier: Modifier = Modifier,
    assetInfo: CodeAssetInfo,
    supportingText: String?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val title = stringResource(assetInfo.titleRes)

    NeoCard(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = !isLoading, onClick = onClick),
        contentPadding = 14.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Neo.Accent
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
