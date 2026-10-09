package com.micsbol.telecon4esp32.ui.control_panel

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.codes.controlPanelSoftApVideoCodeAsset
import com.micsbol.telecon4esp32.ui.codes.CodeAssetInfo
import com.micsbol.telecon4esp32.ui.codes.ZipSharePrepareResult
import com.micsbol.telecon4esp32.ui.codes.materializeZip
import com.micsbol.telecon4esp32.ui.codes.shareZipFile
import kotlinx.coroutines.launch
import com.micsbol.telecon4esp32.ui.theme.HudCyanBright

/** Video-only SoftAP sketch for a second ESP32-CAM. Control stays on Bluetooth. */
@Composable
fun CameraFirmwareCodeLinks(
    modifier: Modifier = Modifier,
) {
    val videoCode = remember { controlPanelSoftApVideoCodeAsset() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = Color.White.copy(alpha = 0.28f),
        )
        Text(
            text = stringResource(R.string.control_panel_camera_code_section),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CameraCodeLink(
            titleRes = R.string.control_panel_camera_softap_video_title,
            sketchRes = R.string.codes_esp32_cam_softap_video,
            asset = videoCode,
            captionRes = R.string.control_panel_camera_softap_video_link_body,
            linkColor = HudCyanBright,
        )
    }
}

@Composable
private fun CameraCodeLink(
    @StringRes titleRes: Int,
    @StringRes sketchRes: Int,
    asset: CodeAssetInfo,
    @StringRes captionRes: Int,
    linkColor: Color,
    blinkDelayMs: Int = 0,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.app_settings_selection_guide_links_coming_soon)
    val downloading = stringResource(R.string.codes_zip_downloading_message)
    val noApp = stringResource(R.string.codes_zip_share_no_app_message)
    val copyFailed = stringResource(R.string.codes_zip_share_prepare_failed_message)
    val sketchName = stringResource(sketchRes).breakAfterUnderscores()
    val blinkAlpha = rememberLinkBlinkAlpha(blinkDelayMs)
    val openCode: () -> Unit = {
        val published = asset.isPublished &&
            (asset.assetFileName != null || !asset.remoteZipUrl.isNullOrBlank())
        if (!published) {
            Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, downloading, Toast.LENGTH_SHORT).show()
            scope.launch {
                val message = try {
                    val file = materializeZip(context, asset)
                    when (shareZipFile(context, file)) {
                        ZipSharePrepareResult.Success -> null
                        ZipSharePrepareResult.NoActivity -> noApp
                        ZipSharePrepareResult.CopyFailed -> copyFailed
                    }
                } catch (_: Exception) {
                    copyFailed
                }
                if (message != null) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        Unit
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = openCode)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(titleRes),
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.38f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = sketchName,
            color = if (enabled) {
                linkColor.copy(alpha = blinkAlpha)
            } else {
                Color.White.copy(alpha = 0.38f)
            },
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            textDecoration = if (enabled) TextDecoration.Underline else TextDecoration.None,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(captionRes),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = if (enabled) 0.62f else 0.38f),
            textAlign = TextAlign.Center,
        )
    }
}

private fun String.breakAfterUnderscores(): String = replace("_", "_\u200b")

@Composable
private fun rememberLinkBlinkAlpha(startDelayMs: Int): Float {
    val transition = rememberInfiniteTransition(label = "cameraCodeLinkBlink")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(startDelayMs),
        ),
        label = "cameraCodeLinkAlpha",
    )
    return alpha
}
