package com.example.emitterapp.ui.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val route: String
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController? = null ) {

    // List of items to display in the cards
    val homeItems = listOf(
        HomeItem(Icons.Default.RocketLaunch, stringResource(R.string.home_item_rc_control), Screen.RcScreen.route),
        HomeItem(Icons.Default.Style, stringResource(R.string.home_item_select_ui), ""), // Add routes as needed
        HomeItem(Icons.Default.Settings, stringResource(R.string.home_item_personalize), ""),
        HomeItem(Icons.Default.Bluetooth, stringResource(R.string.home_item_bluetooth), Screen.Bluetooth.route),
        HomeItem(Icons.Default.VideoLibrary, stringResource(R.string.home_item_tutorial), ""),
//        HomeItem(Icons.Default.Help, "Help", "")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "RC-Emitter") },
                // --- MODIFICATION: Use theme colors ---
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Handle help action */ }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = stringResource(R.string.home_help)
                            // Tint is now inherited from actionIconContentColor
                        )
                    }
                }
            )
        },
        // --- MODIFICATION: Apply background color to the Scaffold itself ---
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp), // Add horizontal padding for the whole screen
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Use LazyColumn for a scrollable and performant list of cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Allow the list to take up available space
                verticalArrangement = Arrangement.spacedBy(12.dp) // Space between cards
            ) {
                items(homeItems) { item ->
                    HomeItemCard(
                        icon = item.icon,
                        title = item.title,
                        onClick = {
                            if (item.route.isNotEmpty()) {
                                navController?.navigate(item.route)
                            }
                        }
                    )
                }
            }

            // Bottom section with image and button
//            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.car_bouncing01),
                contentDescription = "car",
                modifier = Modifier
                    .weight(0.4f)
//                    .size(150.dp) // Use a fixed size for better layout control
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: Define Start action */ },
                // --- MODIFICATION: Use theme colors ---
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(
                    text = stringResource(R.string.home_start_button),
                    // --- MODIFICATION: Use theme typography ---
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.home_upgrade_pro),
                    // --- MODIFICATION: Use theme typography ---
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary, // Use accent color to draw attention
                    modifier = Modifier.clickable { /* TODO */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_version_info),
                    // --- MODIFICATION: Use theme typography ---
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Subdued color
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
@Composable
fun HomeItemCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable(onClick = onClick),
        // --- MODIFICATION: Use theme colors ---
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
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
}

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPreview() {
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