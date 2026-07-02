package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberType

@Composable
fun SolarSystemTopBar(
    energyBalanceW: Int,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.solar_system_menu_content_description),
                    tint = CyberColors.NeonPrimary,
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.solar_system_screen_title).uppercase(),
                        style = CyberType.Title.copy(
                            fontSize = CyberType.SectionTitle.fontSize,
                            shadow = Shadow(
                                color = CyberColors.NeonPrimary.copy(alpha = 0.85f),
                                offset = Offset.Zero,
                                blurRadius = 12f,
                            ),
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(
                            R.string.solar_system_energy_balance,
                            energyBalanceW,
                        ),
                        style = CyberType.Meta.copy(color = CyberColors.Success),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                actions()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            CyberColors.NeonPrimary.copy(alpha = 0.8f),
                            CyberColors.NeonSecondary.copy(alpha = 0.5f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}
