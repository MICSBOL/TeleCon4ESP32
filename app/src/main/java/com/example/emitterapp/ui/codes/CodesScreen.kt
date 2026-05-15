package com.example.emitterapp.ui.codes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.emitterapp.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

private const val ESP32_BT_CONTROLLER_ZIP = "ESP32_BT_Controller-main.zip"

private enum class CodeAssetType {
    Pdf,
    Zip
}

private data class CodeAssetInfo(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val assetFileName: String,
    val type: CodeAssetType,
    val outputFileName: String = assetFileName
)

private enum class PdfOpenResult {
    Success,
    NoActivity,
    CopyFailed
}

private enum class PdfDialogKind {
    NoReader,
    CopyFailed
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

private fun availableCodeAssets(language: String): List<CodeAssetInfo> {
    val fastGuide = if (language == "es") {
        CodeAssetInfo(
            titleRes = R.string.codes_document_fast_guide,
            icon = Icons.Default.Dock,
            assetFileName = "fast_guide_esp.pdf",
            type = CodeAssetType.Pdf
        )
    } else {
        CodeAssetInfo(
            titleRes = R.string.codes_document_fast_guide,
            icon = Icons.Default.Dock,
            assetFileName = "fast_guide_eng.pdf",
            type = CodeAssetType.Pdf
        )
    }

    val generalDocumentation = if (language == "es") {
        CodeAssetInfo(
            titleRes = R.string.codes_document_general,
            icon = Icons.Default.DocumentScanner,
            assetFileName = "general_documentation_es.pdf",
            type = CodeAssetType.Pdf
        )
    } else {
        CodeAssetInfo(
            titleRes = R.string.codes_document_general,
            icon = Icons.Default.DocumentScanner,
            assetFileName = "general_documentation_en.pdf",
            type = CodeAssetType.Pdf
        )
    }

    return listOf(
        generalDocumentation,
        fastGuide,
        CodeAssetInfo(
            titleRes = R.string.codes_esp32_bt_controller_zip,
            icon = Icons.Default.Code,
            assetFileName = ESP32_BT_CONTROLLER_ZIP,
            type = CodeAssetType.Zip
        )
    )
}

@Composable
fun CodesScreen(
    viewModel: CodesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pdfDialogKind by remember { mutableStateOf<PdfDialogKind?>(null) }

    pdfDialogKind?.let { kind ->
        AlertDialog(
            onDismissRequest = { pdfDialogKind = null },
            title = {
                Text(
                    stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_title
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_title
                        }
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_message
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_message
                        }
                    )
                )
            },
            confirmButton = {
                when (kind) {
                    PdfDialogKind.NoReader -> {
                        TextButton(
                            onClick = {
                                context.launchPlayStorePdfReaderSearch()
                                pdfDialogKind = null
                            }
                        ) {
                            Text(stringResource(R.string.codes_pdf_find_reader))
                        }
                    }
                    PdfDialogKind.CopyFailed -> {
                        TextButton(onClick = { pdfDialogKind = null }) {
                            Text(stringResource(R.string.codes_dialog_ok))
                        }
                    }
                }
            },
            dismissButton = if (kind == PdfDialogKind.NoReader) {
                {
                    TextButton(onClick = { pdfDialogKind = null }) {
                        Text(stringResource(R.string.codes_pdf_cancel))
                    }
                }
            } else {
                null
            }
        )
    }

    CodeAssetGrid(
        uiState = uiState,
        onAssetClick = { asset ->
            when (asset.type) {
                CodeAssetType.Pdf -> {
                    when (openPdfAssetExternally(context, asset.assetFileName)) {
                        PdfOpenResult.Success -> Unit
                        PdfOpenResult.NoActivity ->
                            pdfDialogKind = PdfDialogKind.NoReader

                        PdfOpenResult.CopyFailed ->
                            pdfDialogKind = PdfDialogKind.CopyFailed
                    }
                }

                CodeAssetType.Zip -> viewModel.saveZipAsset(
                    assetFileName = asset.assetFileName,
                    outputFileName = asset.outputFileName
                )
            }
        },
        onDismissSaveError = {
            viewModel.dismissSaveError()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeAssetGrid(
    uiState: CodesUiState,
    onAssetClick: (CodeAssetInfo) -> Unit,
    onDismissSaveError: () -> Unit
) {
    val currentLanguage = Locale.getDefault().language
    val availableAssets = remember(currentLanguage) { availableCodeAssets(currentLanguage) }

    if (uiState.saveError != null) {
        AlertDialog(
            onDismissRequest = onDismissSaveError,
            title = { Text(stringResource(R.string.codes_zip_save_error_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.codes_zip_save_error_details,
                        uiState.saveError
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissSaveError) {
                    Text(stringResource(R.string.codes_dialog_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.codes_and_documents_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(8.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        R.string.codes_zip_download_prompt,
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

    Card(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = !isLoading, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                Icon(
                    imageVector = assetInfo.icon,
                    contentDescription = title,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
