package com.example.emitterapp.ui.tutorial

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.emitterapp.R
import com.example.emitterapp.ui.theme.EmitterAppTheme

/** Portrait tutorial clips are authored around this size; keeps letterboxing consistent in the card. */
private const val TUTORIAL_VIDEO_ASPECT_WIDTH = 412f
private const val TUTORIAL_VIDEO_ASPECT_HEIGHT = 915f

/**
 * Placeholder MP4 for every tutorial step until step-specific files exist.
 * Add the file at: app/src/main/assets/videos/testvideo.mp4
 */
const val TUTORIAL_PLACEHOLDER_VIDEO_ASSET = "videos/testvideo.mp4"
const val TUTORIAL_PLACEHOLDER_VIDEO_ASSET_01 = "videos/video_step1.mp4"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(navController: NavController) {
    val tutorialSteps = listOf(
        TutorialStep(
            stringResource(R.string.tutorial_step, 1) + " " + stringResource(R.string.tutorial_home_step_title),
            stringResource(R.string.tutorial_home_step_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 2) + " " + stringResource(R.string.tutorial_codes_step_title),
            stringResource(R.string.tutorial_codes_step_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET_01
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 3) + " " + stringResource(R.string.tutorial_bluetooth_step_title),
            stringResource(R.string.tutorial_bluetooth_step_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 4) + " " + stringResource(R.string.tutorial_settings_step_title),
            stringResource(R.string.tutorial_settings_step_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 5) + " " + stringResource(R.string.tutorial_rc_overview_title),
            stringResource(R.string.tutorial_rc_overview_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 6) + " " + stringResource(R.string.tutorial_rc_left_stick_title),
            stringResource(R.string.tutorial_rc_left_stick_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 7) + " " + stringResource(R.string.tutorial_rc_right_stick_title),
            stringResource(R.string.tutorial_rc_right_stick_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 8) + " " + stringResource(R.string.tutorial_rc_left_knob_title),
            stringResource(R.string.tutorial_rc_left_knob_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 9) + " " + stringResource(R.string.tutorial_rc_right_knob_title),
            stringResource(R.string.tutorial_rc_right_knob_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 10) + " " + stringResource(R.string.tutorial_rc_left_switches_title),
            stringResource(R.string.tutorial_rc_left_switches_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 11) + " " + stringResource(R.string.tutorial_rc_right_switches_title),
            stringResource(R.string.tutorial_rc_right_switches_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
        ),
        TutorialStep(
            stringResource(R.string.tutorial_step, 12) + " " + stringResource(R.string.tutorial_rc_button_events_title),
            stringResource(R.string.tutorial_rc_button_events_description),
            TUTORIAL_PLACEHOLDER_VIDEO_ASSET
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
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
        ,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(0.65f),
                contentAlignment = Alignment.Center
            ) {
                TutorialVideoPlayer(
                    assetPath = step.videoAssetPath,
                    contentDescription = step.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(TUTORIAL_VIDEO_ASPECT_WIDTH / TUTORIAL_VIDEO_ASPECT_HEIGHT)
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                val scrollState = rememberScrollState()

                Text(
                    text = step.description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxHeight()
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
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                this.contentDescription = contentDescription
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        modifier = modifier,
        onRelease = { view ->
            val playerView = view as PlayerView
            playerView.player?.release()
            playerView.player = null
        }
    )
}

data class TutorialStep(
    val title: String,
    val description: String,
    /**
     * Path to an MP4 under `app/src/main/assets/` (e.g. `videos/testvideo.mp4`).
     * Clips are expected around 412x915 portrait for this layout.
     */
    val videoAssetPath: String
)

@Preview(showSystemUi = true)
@Composable
fun TutorialPagePreview() {
    EmitterAppTheme {
        TutorialPage(
            step = TutorialStep(
                title = "Step 1: The Joysticks",
                description = "Description...",
                videoAssetPath = TUTORIAL_PLACEHOLDER_VIDEO_ASSET
            )
        )
    }

}