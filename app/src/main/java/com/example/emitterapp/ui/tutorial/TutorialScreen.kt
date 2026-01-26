package com.example.emitterapp.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.emitterapp.R
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import com.example.emitterapp.ui.theme.EmitterAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(navController: NavController) {
    val tutorialSteps = listOf(
        TutorialStep("Step 1: The Joysticks", "Description...", R.drawable.tutorial_01),
        TutorialStep("Step 2: The Switches", "Description...", R.drawable.tutorial_02),
        TutorialStep("Step 3: The Knobs", "Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...Description...", R.drawable.tutorial_03)
        // ...
    )
    val pagerState = rememberPagerState(pageCount = { tutorialSteps.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Tutorial") },
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
                    .height(50.dp)
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
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // Fill the card
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)
            )

            AsyncImage(
                model = step.animationResId,
                contentDescription = "Tutorial Animation",
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
            )

            Column(
                modifier = Modifier
                    .weight(0.25f)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
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

data class TutorialStep(
    val title: String,
    val description: String,
    val animationResId: Int
)

@Preview(showSystemUi = true)
@Composable
fun TutorialPagePreview() {
    EmitterAppTheme {
        TutorialPage(
            step = TutorialStep(
                title = "Step 1: The Joysticks",
                description = "Description...",
                animationResId = R.drawable.tutorial_01
            )
        )
    }

}