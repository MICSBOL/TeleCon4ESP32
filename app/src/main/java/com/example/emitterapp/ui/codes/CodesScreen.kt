package com.example.emitterapp.ui.codes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
private fun CodesDialogTitle(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Composable
private fun CodesDialogBody(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Dialog frame using app colors: elevated surface, primary accent border (stronger in dark theme). */
@Composable
private fun CodesStyledDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: (@Composable () -> Unit)? = null,
    horizontalMargin: Dp = 16.dp,
    actions: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin),
            shape = RoundedCornerShape(28.dp),
            color = colorScheme.surfaceContainerHigh,
            tonalElevation = if (isDark) 4.dp else 2.dp,
            shadowElevation = if (isDark) 18.dp else 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = colorScheme.primary.copy(alpha = if (isDark) 0.52f else 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                title()
                if (text != null) {
                    text.invoke()
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f)
                )
                actions()
            }
        }
    }
}

@Composable
fun CodesScreen(
    viewModel: CodesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pdfDialogKind by remember { mutableStateOf<PdfDialogKind?>(null) }
    var pendingZipExport by remember { mutableStateOf<CodeAssetInfo?>(null) }
    var zipShareError by remember { mutableStateOf<ZipShareErrorDialog?>(null) }

    pdfDialogKind?.let { kind ->
        CodesStyledDialog(
            onDismissRequest = { pdfDialogKind = null },
            title = {
                CodesDialogTitle(
                    text = stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_title
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_title
                        }
                    )
                )
            },
            text = {
                CodesDialogBody(
                    text = stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_message
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_message
                        }
                    )
                )
            },
            actions = {
                val primary = MaterialTheme.colorScheme.primary
                val onPrimary = MaterialTheme.colorScheme.onPrimary
                when (kind) {
                    PdfDialogKind.NoReader -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { pdfDialogKind = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = primary)
                            ) {
                                Text(
                                    text = stringResource(R.string.codes_pdf_cancel),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    context.launchPlayStorePdfReaderSearch()
                                    pdfDialogKind = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primary,
                                    contentColor = onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = if (isSystemInDarkTheme()) 4.dp else 2.dp,
                                    pressedElevation = 6.dp
                                )
                            ) {
                                Text(stringResource(R.string.codes_pdf_find_reader))
                            }
                        }
                    }

                    PdfDialogKind.CopyFailed -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { pdfDialogKind = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primary,
                                    contentColor = onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = if (isSystemInDarkTheme()) 4.dp else 2.dp
                                )
                            ) {
                                Text(stringResource(R.string.codes_dialog_ok))
                            }
                        }
                    }
                }
            }
        )
    }

    pendingZipExport?.let { asset ->
        CodesStyledDialog(
            onDismissRequest = { pendingZipExport = null },
            title = { CodesDialogTitle(text = stringResource(R.string.codes_zip_export_title)) },
            text = {
                CodesDialogBody(text = stringResource(R.string.codes_zip_export_message))
            },
            actions = {
                val scheme = MaterialTheme.colorScheme
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            pendingZipExport = null
                            when (
                                shareZipAssetExternally(
                                    context,
                                    asset.assetFileName,
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isSystemInDarkTheme()) 4.dp else 2.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.codes_zip_export_share),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            pendingZipExport = null
                            viewModel.saveZipAsset(
                                assetFileName = asset.assetFileName,
                                outputFileName = asset.outputFileName
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, scheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.primary)
                    ) {
                        Text(
                            text = stringResource(R.string.codes_zip_export_save),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    TextButton(
                        onClick = { pendingZipExport = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = scheme.primary)
                    ) {
                        Text(
                            text = stringResource(R.string.codes_pdf_cancel),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        )
    }

    zipShareError?.let { kind ->
        CodesStyledDialog(
            onDismissRequest = { zipShareError = null },
            title = {
                CodesDialogTitle(
                    text = stringResource(
                        when (kind) {
                            ZipShareErrorDialog.NoApp -> R.string.codes_zip_share_no_app_title
                            ZipShareErrorDialog.CopyFailed ->
                                R.string.codes_zip_share_prepare_failed_title
                        }
                    )
                )
            },
            text = {
                CodesDialogBody(
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
                val scheme = MaterialTheme.colorScheme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { zipShareError = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isSystemInDarkTheme()) 4.dp else 2.dp
                        )
                    ) {
                        Text(stringResource(R.string.codes_dialog_ok))
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeAssetGrid(
    uiState: CodesUiState,
    onAssetClick: (CodeAssetInfo) -> Unit,
    onDismissSaveError: () -> Unit
) {
    val currentLanguage = Locale.getDefault().language
    val availableAssets = remember(currentLanguage) { availableCodeAssets(currentLanguage) }

    val saveError = uiState.saveError
    if (saveError != null) {
        CodesStyledDialog(
            onDismissRequest = onDismissSaveError,
            title = {
                CodesDialogTitle(
                    text = stringResource(R.string.codes_zip_save_error_title),
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                CodesDialogBody(
                    text = stringResource(
                        R.string.codes_zip_save_error_details,
                        saveError
                    )
                )
            },
            actions = {
                val scheme = MaterialTheme.colorScheme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismissSaveError,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isSystemInDarkTheme()) 4.dp else 2.dp
                        )
                    ) {
                        Text(stringResource(R.string.codes_dialog_ok))
                    }
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
