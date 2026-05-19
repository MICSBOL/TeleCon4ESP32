package com.example.emitterapp.ui.tutorial

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.emitterapp.R
import com.example.emitterapp.ui.theme.EmitterAppTheme

private const val TUTORIAL_VIDEO_STEP5_ASSET = "videos/video_step5.mp4"
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
            media = TutorialMedia.Video("videos/video_step2.mp4")
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
            media = TutorialMedia.Video("videos/video_step4.mp4")
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step, 5) + " " +
                stringResource(R.string.tutorial_settings_step_title),
            description = stringResource(R.string.tutorial_settings_step_description),
            media = TutorialMedia.Video(TUTORIAL_VIDEO_STEP5_ASSET)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_tutorial_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
                        if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.3f
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

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
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
                        Image(
                            painter = painterResource(
                                if (isDarkTheme) R.drawable.home_dark else R.drawable.home_light
                            ),
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    TutorialMedia.RcScreenshot -> {
                        Image(
                            painter = painterResource(R.drawable.rc_screen),
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    is TutorialMedia.Video -> {
                        TutorialVideoPlayer(
                            assetPath = media.assetPath,
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize()
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                val scrollState = rememberScrollState()
                Text(
                    text = step.description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .heightIn(max = 188.dp)
                        .verticalScroll(scrollState)
                )
            }
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

    if (isFullscreen) {
        BackHandler { isFullscreen = false }
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            TutorialVideoPlayerSurface(
                player = player,
                contentDescription = contentDescription,
                isFullscreen = true,
                onFullscreenToggle = { isFullscreen = false },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        }
    } else {
        TutorialVideoPlayerSurface(
            player = player,
            contentDescription = contentDescription,
            isFullscreen = false,
            onFullscreenToggle = { isFullscreen = true },
            modifier = modifier,
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun TutorialVideoPlayerSurface(
    player: ExoPlayer,
    contentDescription: String,
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullscreenLabel = if (isFullscreen) {
        stringResource(R.string.tutorial_video_exit_fullscreen)
    } else {
        stringResource(R.string.tutorial_video_enter_fullscreen)
    }
    val fullscreenIcon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen

    Box(modifier = modifier) {
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
            modifier = Modifier.fillMaxSize(),
            onRelease = { playerView ->
                playerView.player = null
            },
        )
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

sealed interface TutorialMedia {
    /** Home screen reference image (light/dark drawable chosen from theme). */
    data object HomeScreenshot : TutorialMedia
    data object RcScreenshot : TutorialMedia
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
    EmitterAppTheme {
        TutorialPage(
            step = TutorialStep(
                title = "Step 1: Home screen",
                description = "Description...",
                media = TutorialMedia.HomeScreenshot
            )
        )
    }

}