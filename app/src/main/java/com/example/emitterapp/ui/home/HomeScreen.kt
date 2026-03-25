package com.example.emitterapp.ui.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.emitterapp.R
import com.example.emitterapp.ui.navigation.Screen
import com.example.emitterapp.ui.theme.EmitterAppTheme

data class HomeItem(
    val icon: ImageVector,
    val title: String,
    val route: String,
    val icon2: ImageVector?,
    val route2: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController? = null,
    isConnecting: Boolean = false,
    errorMessage: String? = null,
    lastDeviceName: String? = null,
    onStartClick: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    val homeItems = listOf(
        HomeItem(
            Icons.Default.RocketLaunch,
            stringResource(R.string.home_item_rc_control),
            Screen.RcScreen.route,
            Icons.Default.Settings,
            Screen.RcStettingScreen.route
        ),
        HomeItem(Icons.Default.Style, stringResource(R.string.home_item_select_ui), "", null, ""),
        HomeItem(
            Icons.Default.Bluetooth,
            stringResource(R.string.home_item_bluetooth),
            Screen.Bluetooth.route,
            null,
            ""
        ),
        HomeItem(
            Icons.Default.Code,
            "Codes",
            Screen.Codes.route,
            null,
            ""
        ),
        HomeItem(
            Icons.Default.VideoLibrary,
            stringResource(R.string.home_item_tutorial),
            Screen.Tutorial.route,
            null,
            ""
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "RC-Emitter") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        IconButton(onClick = { /* TODO: Handle help action */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = stringResource(R.string.home_help)
                            )
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeOptionsList(
                        homeItems = homeItems,
                        navController = navController,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                    HomeSecondaryContent(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        imageModifier = Modifier.weight(1f).fillMaxWidth(),
                        lastDeviceName = lastDeviceName,
                        onStartClick = onStartClick
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HomeOptionsList(
                        homeItems = homeItems,
                        navController = navController,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    HomeSecondaryContent(
                        modifier = Modifier.weight(0.7f).fillMaxWidth(),
                        imageModifier = Modifier.weight(1f).fillMaxWidth(),
                        lastDeviceName = lastDeviceName,
                        onStartClick = onStartClick
                    )
                }
            }
        }

        // Error dialog
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text("Connection failed") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = onDismissError) { Text("OK") }
                }
            )
        }

        // Connecting overlay
        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (lastDeviceName != null) "Connecting to $lastDeviceName…"
                               else "Connecting…",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeOptionsList(
    homeItems: List<HomeItem>,
    navController: NavHostController?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(homeItems) { item ->
            HomeItemCard(
                icon = item.icon,
                title = item.title,
                onClick = {
                    if (item.route.isNotEmpty()) {
                        navController?.navigate(item.route)
                    }
                },
                icon2 = item.icon2,
                onClick2 = {
                    if (item.route2.isNotEmpty()) {
                        navController?.navigate(item.route2)
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeSecondaryContent(
    modifier: Modifier = Modifier,
    imageModifier: Modifier,
    lastDeviceName: String? = null,
    onStartClick: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.car_bouncing01),
            contentDescription = "car",
            contentScale = ContentScale.Fit,
            modifier = imageModifier
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = stringResource(R.string.home_start_button),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        if (lastDeviceName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lastDeviceName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_upgrade_pro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* TODO */ }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_version_info),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HomeItemCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    icon2: ImageVector? = null,
    onClick2: () -> Unit
) {
    Row {

        Card(
            modifier = Modifier
                .weight(1f)
                .height(70.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (icon2 != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Card(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(70.dp)
                    .clickable(onClick = onClick2),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon2,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(36.dp)
                    )
                }
            }
        }

    }
}

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    EmitterAppTheme {
        HomeScreen()
    }
}

@Preview(showSystemUi = true, name = "Landscape", device = "spec:width=840dp,height=420dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun HomeScreenLandscapePreview() {
    EmitterAppTheme {
        HomeScreen()
    }
}

@Preview(showSystemUi = true, name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SplashScreenDarkPreview() {
    EmitterAppTheme {
        HomeScreen()
    }
}