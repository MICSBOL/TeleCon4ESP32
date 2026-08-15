package com.micsbol.telecon4esp32.ui.tutorial

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

private const val TUTORIAL_VIDEO_STEP2_LIGHT = "videos/video_step2_light.mp4"
private const val TUTORIAL_VIDEO_STEP2_DARK = "videos/video_step2_dark.mp4"
private const val TUTORIAL_VIDEO_STEP4_LIGHT = "videos/video_step4_light.mp4"
private const val TUTORIAL_VIDEO_STEP4_DARK = "videos/video_step4_dark.mp4"
private const val TUTORIAL_VIDEO_STEP5_LIGHT = "videos/video_step5_light.mp4"
private const val TUTORIAL_VIDEO_STEP5_DARK = "videos/video_step5_dark.mp4"
private const val TUTORIAL_VIDEO_STEP7_ASSET = "videos/video_step7.mp4"
private const val TUTORIAL_VIDEO_STEP8_ASSET = "videos/video_step8.mp4"
private const val TUTORIAL_VIDEO_STEP9_ASSET = "videos/video_step9.mp4"
private const val TUTORIAL_VIDEO_STEP10_ASSET = "videos/video_step10.mp4"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(navController: NavController) {
    val tutorialSteps = listOf(
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 1) + " " +
                stringResource(R.string.tutorial_home_step_title),
            description = stringResource(R.string.tutorial_home_step_description),
            media = TutorialMedia.HomeScreenshot
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 2) + " " +
                stringResource(R.string.tutorial_codes_step_title),
            description = stringResource(R.string.tutorial_codes_step_description),
            media = TutorialMedia.ThemedVideo(
                lightAssetPath = TUTORIAL_VIDEO_STEP2_LIGHT,
                darkAssetPath = TUTORIAL_VIDEO_STEP2_DARK,
            )
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 3) + " " +
                stringResource(R.string.tutorial_arduino_ide_step_title),
            description = stringResource(R.string.tutorial_arduino_ide_step_description),
            media = TutorialMedia.Video("videos/video_step3.mp4")
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 4) + " " +
                stringResource(R.string.tutorial_bluetooth_step_title),
            description = stringResource(R.string.tutorial_bluetooth_step_description),
            media = TutorialMedia.ThemedVideo(
                lightAssetPath = TUTORIAL_VIDEO_STEP4_LIGHT,
                darkAssetPath = TUTORIAL_VIDEO_STEP4_DARK,
            )
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 5) + " " +
                stringResource(R.string.tutorial_settings_step_title),
            description = stringResource(R.string.tutorial_settings_step_description),
            media = TutorialMedia.ThemedVideo(
                lightAssetPath = TUTORIAL_VIDEO_STEP5_LIGHT,
                darkAssetPath = TUTORIAL_VIDEO_STEP5_DARK,
            )
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 6) + " " +
                stringResource(R.string.tutorial_rc_overview_title),
            description = stringResource(R.string.tutorial_rc_overview_description),
            media = TutorialMedia.RcScreenshot
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 7) + " " +
                stringResource(R.string.tutorial_rc_components_test_title),
            description = stringResource(R.string.tutorial_rc_components_test_description),
            media = TutorialMedia.Video(TUTORIAL_VIDEO_STEP7_ASSET)
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 8) + " " +
                stringResource(R.string.tutorial_rc_panel_telemetry_test_title),
            description = stringResource(R.string.tutorial_rc_panel_telemetry_test_description),
            media = TutorialMedia.Video(TUTORIAL_VIDEO_STEP8_ASSET)
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 9) + " " +
                stringResource(R.string.tutorial_rc_indicator_telemetry_test_title),
            description = stringResource(R.string.tutorial_rc_indicator_telemetry_test_description),
            media = TutorialMedia.Video(TUTORIAL_VIDEO_STEP9_ASSET)
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 10) + " " +
                stringResource(R.string.tutorial_rc_plot_telemetry_test_title),
            description = stringResource(R.string.tutorial_rc_plot_telemetry_test_description),
            media = TutorialMedia.Video(TUTORIAL_VIDEO_STEP10_ASSET)
        )
    )
    val pagerState = rememberPagerState(pageCount = { tutorialSteps.size })

    NeoScaffold(
        title = stringResource(R.string.home_title),
        subtitle = stringResource(R.string.app_tutorial_title),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
            ) { page ->
                TutorialPage(step = tutorialSteps[page])
            }

            Row(
                Modifier
                    .height(40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color =
                        if (pagerState.currentPage == iteration) Neo.Accent else Neo.TextSecondary.copy(
                            alpha = 0.4f
                        )
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(12.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun TutorialPage(step: TutorialStep) {
    val isDarkTheme = isSystemInDarkTheme()

    NeoCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        contentPadding = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = step.title,
                color = Neo.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val media = step.media) {
                    TutorialMedia.HomeScreenshot -> {
                        TutorialImageViewer(
                            imageResId = if (isDarkTheme) R.drawable.home_dark else R.drawable.home_light,
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    TutorialMedia.RcScreenshot -> {
                        TutorialImageViewer(
                            imageResId = R.drawable.rc_screen,
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is TutorialMedia.ThemedVideo -> {
                        TutorialVideoPlayer(
                            assetPath = if (isDarkTheme) media.darkAssetPath else media.lightAssetPath,
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is TutorialMedia.Video -> {
                        TutorialVideoPlayer(
                            assetPath = media.assetPath,
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 220.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Neo.Accent.copy(alpha = 0.25f)
                )
                val scrollState = rememberScrollState()
                Text(
                    text = step.description,
                    color = Neo.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .heightIn(max = 188.dp)
                        .verticalScroll(scrollState)
                )
            }
        }
    }
}

@Composable
private fun TutorialImageViewer(
    @DrawableRes imageResId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var isFullscreen by remember { mutableStateOf(false) }

    TutorialFullscreenHost(
        isFullscreen = isFullscreen,
        onFullscreenChange = { isFullscreen = it },
        embeddedModifier = modifier,
        fullscreenModifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) { contentModifier ->
        Image(
            painter = painterResource(imageResId),
            contentDescription = contentDescription,
            modifier = contentModifier,
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun TutorialFullscreenHost(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    embeddedModifier: Modifier,
    fullscreenModifier: Modifier,
    content: @Composable (contentModifier: Modifier) -> Unit,
) {
    val surface: @Composable (Modifier) -> Unit = { surfaceModifier ->
        TutorialFullscreenSurface(
            isFullscreen = isFullscreen,
            onFullscreenToggle = { onFullscreenChange(!isFullscreen) },
            modifier = surfaceModifier,
        ) {
            content(Modifier.fillMaxSize())
        }
    }

    if (isFullscreen) {
        BackHandler { onFullscreenChange(false) }
        Dialog(
            onDismissRequest = { onFullscreenChange(false) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
            ),
        ) {
            surface(fullscreenModifier)
        }
    } else {
        surface(embeddedModifier)
    }
}

@Composable
private fun TutorialFullscreenSurface(
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fullscreenLabel = if (isFullscreen) {
        stringResource(R.string.tutorial_video_exit_fullscreen)
    } else {
        stringResource(R.string.tutorial_video_enter_fullscreen)
    }
    val fullscreenIcon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen

    Box(modifier = modifier) {
        content()
        IconButton(
            onClick = onFullscreenToggle,
            modifier = Modifier.align(Alignment.TopEnd),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = fullscreenIcon,
                contentDescription = fullscreenLabel,
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun TutorialVideoPlayer(
    assetPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    val uri = remember(assetPath) {
        Uri.parse("file:///android_asset/$assetPath")
    }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    TutorialFullscreenHost(
        isFullscreen = isFullscreen,
        onFullscreenChange = { isFullscreen = it },
        embeddedModifier = modifier,
        fullscreenModifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) { contentModifier ->
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    this.contentDescription = contentDescription
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                if (playerView.player !== player) {
                    playerView.player = player
                }
            },
            modifier = contentModifier,
            onRelease = { playerView ->
                playerView.player = null
            },
        )
    }
}

sealed interface TutorialMedia {
    /**
     * Home screen reference image: [R.drawable.home_light] / [R.drawable.home_dark].
     * Badges 1–6 (right-aligned on rows 2–5 and Quick Start; Help top-right):
     * 1 Quick Start, 2 Modules, 3 Bluetooth, 4 Codes and Documents, 5 Tutorial, 6 Help.
     * See [R.string.tutorial_home_step_description].
     */
    data object HomeScreenshot : TutorialMedia
    data object RcScreenshot : TutorialMedia
    data class ThemedVideo(
        val lightAssetPath: String,
        val darkAssetPath: String,
    ) : TutorialMedia
    data class Video(val assetPath: String) : TutorialMedia
}

data class TutorialStep(
    val title: String,
    val description: String,
    val media: TutorialMedia
)

@Preview(showSystemUi = true)
@Composable
fun TutorialPagePreview() {
    TeleCon4Esp32Theme {
        TutorialPage(
            step = TutorialStep(
                title = "Step 1: Home screen",
                description = "Description...",
                media = TutorialMedia.HomeScreenshot
            )
        )
    }

}