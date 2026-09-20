package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.components.HoloTurntableFlipbook
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.components.holoTurntableAssetDir
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.wallet.CoinUnlockPillButton

private val ProGold = Color(0xFFFFD54F)
private val ProGoldDeep = Color(0xFFFFB300)

enum class ApplicationTrailingAction {
    ENTER,
    UNLOCK,
    DEFAULT,
}

@Composable
fun ApplicationListItemCard(
    @DrawableRes thumbnailRes: Int,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    onCodesClick: () -> Unit,
    trailingAction: ApplicationTrailingAction = ApplicationTrailingAction.DEFAULT,
    onUnlockClick: () -> Unit = {},
    onSubscribeClick: () -> Unit = {},
    showExplorerSparkle: Boolean = false,
    onExplorerSparkleClick: () -> Unit = {},
    enabledProHighlight: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "appCardScale")
    val isLocked = trailingAction == ApplicationTrailingAction.UNLOCK
    val isProBadge = badge.equals("PRO", ignoreCase = true)
    val showBadge = badge.isNotBlank()
    var detailsExpanded by remember { mutableStateOf(false) }
    val expandRotation by animateFloatAsState(
        targetValue = if (detailsExpanded) 180f else 0f,
        label = "catalogExpandArrow",
    )
    val cardShape = RoundedCornerShape(22.dp)
    val enabledBorderPulse = rememberInfiniteTransition(label = "enabledProBorder")
    val enabledBorderAlpha by enabledBorderPulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "enabledProBorderAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .glassSurface(cornerRadius = 22.dp)
            .then(
                if (enabledProHighlight) {
                    Modifier.border(
                        width = 1.dp,
                        color = Neo.Positive.copy(alpha = enabledBorderAlpha),
                        shape = cardShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    if (trailingAction == ApplicationTrailingAction.ENTER ||
                        trailingAction == ApplicationTrailingAction.DEFAULT
                    ) {
                        onClick()
                    } else {
                        detailsExpanded = !detailsExpanded
                    }
                },
            )
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModuleHologramThumb(
                thumbnailRes = thumbnailRes,
                size = 56.dp,
                expandRotation = expandRotation,
                onExpandClick = { detailsExpanded = !detailsExpanded },
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = Neo.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).widthIn(min = 0.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (showExplorerSparkle) {
                ExplorerSparkleButton(onClick = onExplorerSparkleClick)
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (showBadge) {
                BadgeChip(
                    badge = badge,
                    onClick = if (isProBadge) onSubscribeClick else null,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            DocumentationIconButton(onClick = onCodesClick)
            if (trailingAction == ApplicationTrailingAction.ENTER ||
                trailingAction == ApplicationTrailingAction.DEFAULT
            ) {
                Spacer(modifier = Modifier.width(6.dp))
                ApplicationTrailingControl(action = trailingAction)
            }
        }

        AnimatedVisibility(
            visible = detailsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subtitle,
                    color = Neo.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                if (isLocked) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CoinUnlockPillButton(
                        coinCost = 0,
                        onClick = onUnlockClick,
                        compact = true,
                        fillMaxWidth = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplorerSparkleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "explorerSparkle")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "explorerSparkleAlpha",
    )
    val description = stringResource(R.string.applications_explorer_sparkle_content_description)
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Neo.Accent.copy(alpha = alpha),
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { this.alpha = alpha },
        )
    }
}

@Composable
private fun DocumentationIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .glassSurface(cornerRadius = 12.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_codes_gold),
            contentDescription = stringResource(R.string.applications_codes_content_description),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ModuleHologramThumb(
    @DrawableRes thumbnailRes: Int,
    size: Dp,
    expandRotation: Float,
    onExpandClick: () -> Unit,
) {
    val expandDescription = stringResource(
        if (expandRotation > 90f) {
            R.string.applications_collapse_details_content_description
        } else {
            R.string.applications_expand_details_content_description
        },
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .glassSurface(cornerRadius = 14.dp)
            .clickable(onClick = onExpandClick)
            .semantics {
                role = Role.Button
                contentDescription = expandDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(thumbnailRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(expandRotation),
            )
        }
    }
}

@Composable
private fun ApplicationTrailingControl(
    action: ApplicationTrailingAction,
    modifier: Modifier = Modifier,
) {
    when (action) {
        ApplicationTrailingAction.ENTER -> {
            Image(
                painter = painterResource(R.drawable.ic_chevron_enter_gold),
                contentDescription = null,
                modifier = modifier.size(24.dp),
            )
        }
        ApplicationTrailingAction.DEFAULT -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Neo.TextSecondary,
                modifier = modifier.size(20.dp),
            )
        }
        ApplicationTrailingAction.UNLOCK -> Unit
    }
}

@Composable
private fun BadgeChip(
    badge: String,
    onClick: (() -> Unit)? = null,
) {
    val isPro = badge.equals("PRO", ignoreCase = true)
    val accent = when {
        isPro -> ProGold
        badge.equals("SOON", ignoreCase = true) ||
            badge.equals("PRONTO", ignoreCase = true) -> Neo.TextSecondary
        else -> Neo.Positive
    }
    val subscribeDescription = stringResource(R.string.applications_pro_subscribe_content_description)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isPro) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ProGold.copy(alpha = 0.28f),
                                ProGoldDeep.copy(alpha = 0.18f),
                            ),
                        ),
                    )
                } else {
                    Modifier.background(accent.copy(alpha = 0.16f))
                },
            )
            .then(
                if (isPro) {
                    Modifier.border(1.dp, ProGold.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isPro) {
                                subscribeDescription
                            } else {
                                badge
                            }
                        }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = badge.uppercase(),
            color = if (isPro) ProGold else accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Opaque trading-card layout for Home: gold frame, art window, always-visible description.
 */
@Composable
fun HomeModuleHeroCard(
    applicationId: ApplicationId,
    @DrawableRes thumbnailRes: Int,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    onCodesClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: ApplicationTrailingAction = ApplicationTrailingAction.DEFAULT,
    onUnlockClick: () -> Unit = {},
    showExplorerSparkle: Boolean = false,
    onExplorerSparkleClick: () -> Unit = {},
    enabledProHighlight: Boolean = false,
    animateHologram: Boolean = true,
    isFront: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "homeHeroScale")
    val isLocked = trailingAction == ApplicationTrailingAction.UNLOCK
    val cardShape = RoundedCornerShape(18.dp)
    val innerShape = RoundedCornerShape(10.dp)
    val turntableDir = applicationId.holoTurntableAssetDir()?.takeIf { animateHologram }
    val moveName = stringResource(
        when (applicationId) {
            ApplicationId.CONTROL_PANEL -> R.string.home_module_card_move_control
            ApplicationId.RC_VEHICLE_PRO -> R.string.home_module_card_move_rc
        },
    )
    val linkLabel = stringResource(
        when (applicationId) {
            ApplicationId.CONTROL_PANEL -> R.string.home_module_card_link_control
            ApplicationId.RC_VEHICLE_PRO -> R.string.home_module_card_link_rc
        },
    )
    val statusLabel = stringResource(
        if (isLocked) {
            R.string.home_module_card_status_locked
        } else {
            R.string.home_module_card_status_ready
        },
    )
    val highlightAlpha by rememberInfiniteTransition(label = "moduleCardGlow").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "moduleCardGlowAlpha",
    )
    val borderColor = when {
        isFront && enabledProHighlight -> Neo.Positive.copy(alpha = highlightAlpha)
        isFront -> Neo.AccentHighlight
        else -> Neo.TextMuted.copy(alpha = 0.45f)
    }
    val titleColor = if (isFront) Neo.TextPrimary else Neo.TextMuted
    val bodyColor = if (isFront) Neo.TextSecondary else Neo.TextMuted.copy(alpha = 0.7f)
    val accentLabel = if (isFront) Neo.AccentHighlight else Neo.TextMuted
    val titleSize = if (isFront) 18.sp else 14.sp
    val moveSize = if (isFront) 15.sp else 12.sp
    val bodySize = if (isFront) 13.sp else 11.sp
    val borderWidth = if (isFront) 3.dp else 1.5.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (isFront) AppGlass.BackgroundMid else AppGlass.BackgroundTop)
            .border(width = borderWidth, color = borderColor, shape = cardShape)
            .padding(if (isFront) 12.dp else 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_module_card_stage),
                color = accentLabel,
                fontSize = if (isFront) 10.sp else 8.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = titleColor,
                fontSize = titleSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).widthIn(min = 0.dp),
            )
            if (badge.isNotBlank()) {
                BadgeChip(
                    badge = badge,
                    onClick = if (isFront) onClick else null,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .scale(pressScale)
                .clip(innerShape)
                .background(AppGlass.BackgroundTop)
                .border(
                    1.dp,
                    if (isFront) Neo.Accent.copy(alpha = 0.55f) else Neo.TextMuted.copy(alpha = 0.25f),
                    innerShape,
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .semantics {
                    role = Role.Button
                    contentDescription = title
                },
            contentAlignment = Alignment.Center,
        ) {
            val hologramModifier = Modifier.fillMaxSize()
            if (turntableDir != null) {
                HoloTurntableFlipbook(
                    assetDir = turntableDir,
                    contentDescription = title,
                    modifier = hologramModifier,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(
                    painter = painterResource(thumbnailRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = hologramModifier,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(innerShape)
                .background(AppGlass.BackgroundBottom)
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = moveName,
                    color = titleColor,
                    fontSize = moveSize,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = bodyColor,
                fontSize = bodySize,
                lineHeight = if (isFront) 18.sp else 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = linkLabel,
                color = accentLabel,
                fontSize = if (isFront) 12.sp else 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusLabel,
                color = if (isFront) Neo.TextSecondary else Neo.TextMuted.copy(alpha = 0.65f),
                fontSize = if (isFront) 12.sp else 10.sp,
            )
            if (isFront) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showExplorerSparkle) {
                        ExplorerSparkleButton(onClick = onExplorerSparkleClick)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (isLocked) {
                        CoinUnlockPillButton(
                            coinCost = 0,
                            onClick = onUnlockClick,
                            compact = true,
                            fillMaxWidth = false,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    DocumentationIconButton(onClick = onCodesClick)
                }
            }
        }
    }
}

