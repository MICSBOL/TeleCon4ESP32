package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.theme.HudCyan
import com.micsbol.telecon4esp32.ui.theme.HudCyanBright
import com.micsbol.telecon4esp32.ui.theme.HudCyanMuted
import com.micsbol.telecon4esp32.ui.theme.HudTextSecondary
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected
import com.micsbol.telecon4esp32.ui.theme.syncopate
import kotlin.math.min

private val BlueprintNavyDeep = Color(0xFF061018)
private val BlueprintGrid = Color(0x2200E5FF)
private val BlueprintLine = HudCyanBright.copy(alpha = 0.85f)
private val BlueprintLineDim = HudCyan.copy(alpha = 0.45f)

@Composable
fun BlueprintGridBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlueprintNavyDeep),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 24.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = BlueprintGrid,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 0.6f,
                )
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = BlueprintGrid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.6f,
                )
                y += step
            }
        }
    }
}

@Composable
fun BlueprintCornerReticles(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val inset = 14.dp.toPx()
        val r = 10.dp.toPx()
        listOf(
            Offset(inset, inset),
            Offset(size.width - inset, inset),
            Offset(inset, size.height - inset),
            Offset(size.width - inset, size.height - inset),
        ).forEach { center ->
            drawCircle(BlueprintLineDim, r, center, style = Stroke(1f))
            drawLine(BlueprintLineDim, Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1f)
            drawLine(BlueprintLineDim, Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1f)
        }
    }
}

@Composable
fun BlueprintHeader(
    title: String,
    subtitle: String,
    version: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontFamily = syncopate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.5.sp,
                ),
                color = BlueprintLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle.uppercase(),
                style = TextStyle(
                    fontFamily = syncopate,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                ),
                color = BlueprintLine.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = version,
                style = MaterialTheme.typography.labelSmall,
                color = HudCyanMuted,
                letterSpacing = 0.5.sp,
            )
        }
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(100.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawEsp32Schematic(size.width, size.height)
            }
        }
    }
}

@Composable
fun BlueprintMenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    statusConnected: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBlueprintPlusCornerFrame(size.width, size.height)
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BlueprintLine,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = TextStyle(
                        fontFamily = syncopate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                    ),
                    color = BlueprintLine,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = TextStyle(
                        fontFamily = syncopate,
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                    ),
                    color = HudTextSecondary.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statusConnected) StatusConnected else StatusDisconnected,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BlueprintLine.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun BlueprintStartButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val lineColor = if (enabled) BlueprintLine else BlueprintLine.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBlueprintChamferStartFrame(size.width, size.height, lineColor)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontFamily = syncopate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 3.sp,
                ),
                color = lineColor,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(lineColor, size.minDimension / 2f - 1f, center, style = Stroke(1.2f))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = lineColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun BlueprintFooter(
    title: String,
    tagline: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontFamily = syncopate,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            ),
            color = BlueprintLine.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tagline.uppercase(),
            style = TextStyle(
                fontFamily = syncopate,
                fontWeight = FontWeight.Normal,
                fontSize = 8.sp,
                letterSpacing = 1.5.sp,
            ),
            color = HudCyanMuted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun DrawScope.drawBlueprintPlusCornerFrame(width: Float, height: Float) {
    val pad = 1f
    val sw = 1f
    val plusHalf = 5f
    val color = BlueprintLine

    drawLine(color, Offset(pad, pad), Offset(width - pad, pad), sw)
    drawLine(color, Offset(pad, height - pad), Offset(width - pad, height - pad), sw)

    val corners = listOf(
        Offset(pad, pad),
        Offset(width - pad, pad),
        Offset(pad, height - pad),
        Offset(width - pad, height - pad),
    )
    corners.forEach { c ->
        drawLine(color, Offset(c.x - plusHalf, c.y), Offset(c.x + plusHalf, c.y), sw)
        drawLine(color, Offset(c.x, c.y - plusHalf), Offset(c.x, c.y + plusHalf), sw)
    }

    val tickStep = width / 8f
    for (i in 1 until 8) {
        val x = pad + tickStep * i
        drawLine(color.copy(alpha = 0.35f), Offset(x, pad), Offset(x, pad + 3f), 0.8f)
        drawLine(color.copy(alpha = 0.35f), Offset(x, height - pad), Offset(x, height - pad - 3f), 0.8f)
    }
}

private fun DrawScope.drawBlueprintChamferStartFrame(width: Float, height: Float, color: Color) {
    val pad = 2f
    val c = min(width, height) * 0.12f
    val innerPad = 6f
    val sw = 1.2f

    fun chamferPath(inset: Float, chamfer: Float): Path = Path().apply {
        val l = pad + inset
        val t = pad + inset
        val r = width - pad - inset
        val b = height - pad - inset
        moveTo(l + chamfer, t)
        lineTo(r - chamfer, t)
        lineTo(r, t + chamfer)
        lineTo(r, b - chamfer)
        lineTo(r - chamfer, b)
        lineTo(l + chamfer, b)
        lineTo(l, b - chamfer)
        lineTo(l, t + chamfer)
        close()
    }

    val outer = chamferPath(0f, c)
    val inner = chamferPath(innerPad, c - innerPad * 0.35f)

    drawPath(outer, color = color, style = Stroke(sw))
    drawPath(inner, color = color.copy(alpha = 0.7f), style = Stroke(sw * 0.85f))

    val tri = c * 0.35f
    listOf(
        Triple(Offset(pad + c, pad), Offset(pad + c - tri, pad), Offset(pad + c, pad + tri)),
        Triple(Offset(width - pad - c, pad), Offset(width - pad - c + tri, pad), Offset(width - pad - c, pad + tri)),
        Triple(Offset(width - pad, pad + c), Offset(width - pad - tri, pad + c - tri), Offset(width - pad - tri, pad + c)),
        Triple(Offset(width - pad, height - pad - c), Offset(width - pad - tri, height - pad - c + tri), Offset(width - pad - tri, height - pad - c)),
        Triple(Offset(width - pad - c, height - pad), Offset(width - pad - c + tri, height - pad), Offset(width - pad - c, height - pad - tri)),
        Triple(Offset(pad + c, height - pad), Offset(pad + c - tri, height - pad), Offset(pad + c, height - pad - tri)),
        Triple(Offset(pad, height - pad - c), Offset(pad + tri, height - pad - c + tri), Offset(pad + tri, height - pad - c)),
        Triple(Offset(pad, pad + c), Offset(pad + tri, pad + c - tri), Offset(pad + tri, pad + c)),
    ).forEach { (a, b, corner) ->
        drawPath(
            Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
                lineTo(corner.x, corner.y)
                close()
            },
            color = color.copy(alpha = 0.55f),
        )
    }
}

private fun DrawScope.drawEsp32Schematic(w: Float, h: Float) {
    val color = BlueprintLine.copy(alpha = 0.75f)
    val sw = 1f
    val boardW = w * 0.42f
    val boardH = h * 0.72f
    val left = w * 0.52f - boardW / 2f
    val top = h * 0.08f

    drawRect(color, topLeft = Offset(left, top), size = Size(boardW, boardH), style = Stroke(sw))
    drawRect(
        color.copy(alpha = 0.5f),
        topLeft = Offset(left + boardW * 0.15f, top + boardH * 0.12f),
        size = Size(boardW * 0.7f, boardH * 0.35f),
        style = Stroke(sw * 0.8f),
    )
    drawRect(
        color.copy(alpha = 0.4f),
        topLeft = Offset(left + boardW * 0.25f, top + boardH * 0.78f),
        size = Size(boardW * 0.5f, boardH * 0.12f),
        style = Stroke(sw * 0.8f),
    )

    val pinCount = 8
    val pinSpacing = boardH * 0.85f / pinCount
    for (i in 0 until pinCount) {
        val py = top + boardH * 0.08f + pinSpacing * i
        drawLine(color.copy(alpha = 0.35f), Offset(left - 4f, py), Offset(left, py), sw * 0.8f)
        drawLine(color.copy(alpha = 0.35f), Offset(left + boardW, py), Offset(left + boardW + 4f, py), sw * 0.8f)
    }

    val dimY = top + boardH + 8f
    drawLine(color.copy(alpha = 0.5f), Offset(left, dimY), Offset(left + boardW, dimY), sw * 0.7f)
    drawLine(color.copy(alpha = 0.5f), Offset(left, dimY - 3f), Offset(left, dimY + 3f), sw * 0.7f)
    drawLine(color.copy(alpha = 0.5f), Offset(left + boardW, dimY - 3f), Offset(left + boardW, dimY + 3f), sw * 0.7f)

    val dimX = left - 10f
    drawLine(color.copy(alpha = 0.5f), Offset(dimX, top), Offset(dimX, top + boardH), sw * 0.7f)
    drawLine(color.copy(alpha = 0.5f), Offset(dimX - 3f, top), Offset(dimX + 3f, top), sw * 0.7f)
    drawLine(color.copy(alpha = 0.5f), Offset(dimX - 3f, top + boardH), Offset(dimX + 3f, top + boardH), sw * 0.7f)
}
