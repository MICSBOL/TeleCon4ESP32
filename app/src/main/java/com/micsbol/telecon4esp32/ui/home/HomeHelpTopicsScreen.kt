package com.micsbol.telecon4esp32.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.NeoScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHelpTopicsScreen(navController: NavController) {
    NeoScaffold(
        title = stringResource(R.string.home_help_topics_title),
        subtitle = stringResource(R.string.home_help_topics_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(helpTopics, key = { it.problemRes }) { topic ->
                HomeHelpTopicItem(topic = topic)
            }
        }
    }
}
