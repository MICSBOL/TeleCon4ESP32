package com.micsbol.telecon4esp32.ui.codes

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.control_panel.LockScreenOrientation
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmwareSketchesScreen(navController: NavController) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var openingPdf by remember { mutableStateOf(false) }
    var pdfDialogKind by remember { mutableStateOf<PdfOpenResult?>(null) }
    val sketchGuide = remember {
        documentationAssetsFor(currentCodeAssetLanguage())
            .first { it.id == FIRMWARE_SKETCHES_DOC_ID }
    }
    val navigateBack: () -> Unit = { navController.navigateUp() }
    BackHandler(onBack = navigateBack)

    pdfDialogKind?.let { kind ->
        NeoDialog(
            onDismissRequest = { pdfDialogKind = null },
            title = {
                NeoDialogTitle(
                    text = stringResource(
                        when (kind) {
                            PdfOpenResult.NoActivity -> R.string.codes_pdf_no_reader_title
                            else -> R.string.codes_pdf_open_failed_title
                        },
                    ),
                )
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(
                        when (kind) {
                            PdfOpenResult.NoActivity -> R.string.codes_pdf_no_reader_message
                            else -> R.string.codes_pdf_open_failed_message
                        },
                    ),
                )
            },
            actions = {
                if (kind == PdfOpenResult.NoActivity) {
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
                } else {
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
            },
        )
    }

    NeoScaffold(
        title = stringResource(R.string.firmware_sketches_title),
        subtitle = stringResource(R.string.firmware_sketches_subtitle),
        onNavigateBack = navigateBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                Text(
                    text = stringResource(R.string.firmware_sketches_bluetooth_title),
                    color = Neo.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.firmware_sketches_bluetooth_body),
                    color = Neo.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                Text(
                    text = stringResource(R.string.firmware_sketches_cam_pair_title),
                    color = Neo.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.firmware_sketches_cam_pair_body),
                    color = Neo.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            NeoPillButton(
                text = stringResource(R.string.firmware_sketches_open_pdf),
                onClick = {
                    if (openingPdf || sketchGuide.assetFileName == null) return@NeoPillButton
                    scope.launch {
                        openingPdf = true
                        val result = try {
                            openPublishedPdf(context, sketchGuide)
                        } finally {
                            openingPdf = false
                        }
                        when (result) {
                            PdfOpenResult.Success -> Unit
                            PdfOpenResult.NoActivity -> pdfDialogKind = PdfOpenResult.NoActivity
                            PdfOpenResult.CopyFailed -> pdfDialogKind = PdfOpenResult.CopyFailed
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
