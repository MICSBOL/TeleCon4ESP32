package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.NeoIconBadge
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.wallet.CoinUnlockPillButton

enum class ApplicationTrailingAction {
    ENTER,
    UNLOCK,
    DEFAULT,
}

@Composable
fun ApplicationListItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    onCodesClick: () -> Unit,
    trailingAction: ApplicationTrailingAction = ApplicationTrailingAction.DEFAULT,
    onUnlockClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "appCardScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .glassSurface(cornerRadius = 22.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeoIconBadge(icon = icon, size = 48.dp, selected = pressed)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Neo.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Neo.TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            BadgeChip(badge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_codes_gold),
                    contentDescription = stringResource(R.string.applications_codes_content_description),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onCodesClick),
                )
                ApplicationTrailingControl(
                    action = trailingAction,
                    onUnlockClick = onUnlockClick,
                )
            }
        }
    }
}

@Composable
private fun ApplicationTrailingControl(
    action: ApplicationTrailingAction,
    onUnlockClick: () -> Unit,
) {
    when (action) {
        ApplicationTrailingAction.UNLOCK -> {
            CoinUnlockPillButton(
                coinCost = 0,
                onClick = onUnlockClick,
                compact = true,
            )
        }
        ApplicationTrailingAction.ENTER -> {
            Image(
                painter = painterResource(R.drawable.ic_chevron_enter_gold),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        ApplicationTrailingAction.DEFAULT -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Neo.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BadgeChip(badge: String) {
    val accent = when (badge.uppercase()) {
        "PRO" -> Neo.Negative
        "SOON", "PRONTO" -> Neo.TextSecondary
        "UNLOCKED", "DESBLOQ." -> Neo.Positive
        else -> Neo.Positive
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = badge.uppercase(),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
