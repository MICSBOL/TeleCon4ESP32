package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.micsbol.telecon4esp32.ui.theme.syncopate
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.theme.HudCyan
import com.micsbol.telecon4esp32.ui.theme.HudCyanBright
import com.micsbol.telecon4esp32.ui.theme.HudCyanDim
import com.micsbol.telecon4esp32.ui.theme.HudCyanMuted
import com.micsbol.telecon4esp32.ui.theme.HudMenuBg
import com.micsbol.telecon4esp32.ui.theme.HudMenuNeon
import com.micsbol.telecon4esp32.ui.theme.HudTextSecondary
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected
import kotlin.random.Random

val HudCardShape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
private val HudChamfer = 10.dp

/** Simple chamfered border for panels and info areas — not the full title-frame decoration. */
fun Modifier.hudSimpleBorder(
    color: Color = HudCyan,
    strokeWidth: Dp = 1.dp,
    chamfer: Dp = HudChamfer,
    glow: Boolean = true,
): Modifier = drawBehind {
    val sw = strokeWidth.toPx()
    val c = chamfer.toPx()
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(c, 0f)
        lineTo(w - c, 0f)
        lineTo(w, c)
        lineTo(w, h - c)
        lineTo(w - c, h)
        lineTo(c, h)
        lineTo(0f, h - c)
        lineTo(0f, c)
        close()
    }
    if (glow) {
        drawPath(path, color = color.copy(alpha = 0.25f), style = Stroke(width = sw * 4))
    }
    drawPath(path, color = color, style = Stroke(width = sw))
    val tick = c * 0.6f
    drawLine(color.copy(alpha = 0.7f), Offset(0f, c), Offset(tick, c), sw)
    drawLine(color.copy(alpha = 0.7f), Offset(w - tick, c), Offset(w, c), sw)
    drawLine(color.copy(alpha = 0.7f), Offset(0f, h - c), Offset(tick, h - c), sw)
    drawLine(color.copy(alpha = 0.7f), Offset(w - tick, h - c), Offset(w, h - c), sw)
}

@Composable
fun HudStarfieldBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        List(80) {
            Star(
                x = Random(42 + it).nextFloat(),
                y = Random(84 + it).nextFloat(),
                radius = Random(126 + it).nextFloat() * 1.8f + 0.4f,
                alpha = Random(200 + it).nextFloat() * 0.5f + 0.2f,
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050A12), Color(0xFF0A1628), Color(0xFF050A12))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { star ->
                drawCircle(
                    color = HudCyanBright.copy(alpha = star.alpha),
                    radius = star.radius,
                    center = Offset(star.x * size.width, star.y * size.height),
                )
            }
        }
    }
}

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

@Composable
fun HudFramedBox(
    modifier: Modifier = Modifier,
    fillBackground: Boolean = false,
    verticalDividerFraction: Float? = null,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawHudAngularFrame(
                width = size.width,
                height = size.height,
                color = HudCyan,
                brightColor = HudCyanBright,
                fillBackground = fillBackground,
                verticalDividerFraction = verticalDividerFraction,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = HudCyan.copy(alpha = 0.7f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .hudSimpleBorder(color = borderColor)
            .background(HudCyan.copy(alpha = 0.04f))
            .padding(12.dp),
        content = content,
    )
}

@Composable
fun HudTitleFrame(
    title: String,
    version: String,
    modifier: Modifier = Modifier,
) {
    val titleGlow = Shadow(
        color = HudCyanBright.copy(alpha = 0.85f),
        offset = Offset.Zero,
        blurRadius = 10f,
    )
    HudFramedBox(
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth(),
        fillBackground = false,
        contentPadding = PaddingValues(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontFamily = syncopate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.2.sp,
                    shadow = titleGlow,
                ),
                color = HudCyanBright,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = version,
                style = TextStyle(
                    fontFamily = syncopate,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    shadow = Shadow(
                        color = HudCyan.copy(alpha = 0.5f),
                        offset = Offset.Zero,
                        blurRadius = 6f,
                    ),
                ),
                color = HudCyanMuted,
            )
        }
    }
}

private fun DrawScope.drawHudAngularFrame(
    width: Float,
    height: Float,
    color: Color,
    brightColor: Color,
    fillBackground: Boolean = false,
    verticalDividerFraction: Float? = null,
) {
    val c = min(width, height) * 0.14f
    val sw = 1.2f
    val swThick = 2f
    val pad = 2f

    fun octagonPath(inset: Float, chamfer: Float): Path = Path().apply {
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

    val outerPath = octagonPath(inset = 0f, chamfer = c)
    val innerPath = octagonPath(inset = 5f, chamfer = c * 0.82f)

    if (fillBackground) {
        drawPath(outerPath, color = HudMenuBg.copy(alpha = 0.92f))
    }

    drawPath(outerPath, color = color.copy(alpha = 0.18f), style = Stroke(width = sw * 5))
    drawPath(outerPath, color = brightColor.copy(alpha = 0.55f), style = Stroke(width = swThick))
    drawPath(innerPath, color = brightColor.copy(alpha = 0.9f), style = Stroke(width = sw))

    // Top-center stepped cap
    val midX = width / 2f
    val topY = pad + 1f
    val capHalf = width * 0.11f
    drawLine(brightColor.copy(alpha = 0.7f), Offset(midX - capHalf, topY - 1f), Offset(midX + capHalf, topY - 1f), sw)
    drawLine(brightColor, Offset(midX - capHalf * 0.75f, topY + 3f), Offset(midX + capHalf * 0.75f, topY + 3f), swThick)

    // Diagonal hatch — top-right interior
    val hatchLeft = width * 0.72f
    val hatchTop = pad + 6f
    val hatchBottom = height * 0.42f
    var hy = hatchTop
    while (hy < hatchBottom) {
        drawLine(
            color = color.copy(alpha = 0.35f),
            start = Offset(hatchLeft, hy),
            end = Offset(hatchLeft + (hatchBottom - hy) * 0.55f, hy + (hatchBottom - hy) * 0.55f),
            strokeWidth = 0.8f,
        )
        hy += 5f
    }

    // Corner vertex dots
    val dotR = 2.2f
    listOf(
        Offset(pad + c, pad),
        Offset(width - pad - c, pad),
        Offset(width - pad, pad + c),
        Offset(width - pad, height - pad - c),
        Offset(width - pad - c, height - pad),
        Offset(pad + c, height - pad),
        Offset(pad, height - pad - c),
        Offset(pad, pad + c),
    ).forEach { center ->
        drawCircle(brightColor.copy(alpha = 0.35f), dotR * 2.5f, center)
        drawCircle(brightColor, dotR, center)
    }

    // Side tabs — left and right edge
    val tabY = height * 0.38f
    val tabLen = 8f
    drawLine(brightColor.copy(alpha = 0.8f), Offset(pad - 1f, tabY), Offset(pad + tabLen, tabY), swThick)
    drawLine(brightColor.copy(alpha = 0.8f), Offset(width - pad - tabLen, tabY), Offset(width - pad + 1f, tabY), swThick)

    // Bottom corner ticks
    val tickLen = c * 0.45f
    val b = height - pad
    drawLine(color.copy(alpha = 0.6f), Offset(pad, b - c), Offset(pad + tickLen, b - c + tickLen * 0.3f), sw)
    drawLine(color.copy(alpha = 0.6f), Offset(width - pad, b - c), Offset(width - pad - tickLen, b - c + tickLen * 0.3f), sw)

    // Optional vertical divider for menu cards
    verticalDividerFraction?.let { fraction ->
        val dividerX = width * fraction
        drawLine(
            color = brightColor.copy(alpha = 0.12f),
            start = Offset(dividerX, pad + c * 0.55f),
            end = Offset(dividerX, height - pad - c * 0.55f),
            strokeWidth = swThick * 3f,
        )
        drawLine(
            color = brightColor.copy(alpha = 0.85f),
            start = Offset(dividerX, pad + c * 0.7f),
            end = Offset(dividerX, height - pad - c * 0.7f),
            strokeWidth = sw,
        )
    }
}

@Composable
fun HudBluetoothStatusRing(
    connected: Boolean,
    connecting: Boolean,
    statusText: String,
    modifier: Modifier = Modifier,
) {
    val ringColor = when {
        connecting -> HudCyanDim
        connected -> StatusConnected
        else -> StatusDisconnected
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                listOf(0.95f, 0.78f, 0.62f).forEachIndexed { i, scale ->
                    drawCircle(
                        color = ringColor.copy(alpha = 0.15f + i * 0.1f),
                        radius = (size.minDimension / 2) * scale,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f),
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = statusText,
                tint = ringColor,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = statusText.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ringColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun HudMenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    statusConnected: Boolean = false,
    badgeText: String? = null,
) {
    val neon = HudMenuNeon
    HudFramedBox(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable(onClick = onClick),
        fillBackground = false,
        verticalDividerFraction = 0.25f,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                HudHexIconFrame(
                    icon = icon,
                    contentDescription = title,
                    modifier = Modifier.size(50.dp),
                    neon = neon,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 8.dp),
            ) {
                Text(
                    text = title.uppercase(),
                    style = TextStyle(
                        fontFamily = syncopate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.4.sp,
                        shadow = Shadow(neon.copy(alpha = 0.9f), Offset.Zero, 12f),
                    ),
                    color = neon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = TextStyle(
                        fontFamily = syncopate,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    ),
                    color = HudTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = badgeText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = neon,
                        letterSpacing = 0.5.sp,
                    )
                } else if (statusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (statusConnected) StatusConnected else StatusDisconnected)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (statusConnected) StatusConnected else StatusDisconnected,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = neon,
                modifier = Modifier
                    .padding(end = 18.dp)
                    .size(22.dp),
            )
        }
    }
}

@Composable
private fun HudHexIconFrame(
    icon: ImageVector,
    contentDescription: String?,
    neon: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawHudHexIconFrame(center, size.minDimension / 2f - 4f, neon)
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = neon,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun DrawScope.drawNeonStrokeLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
) {
    drawLine(color.copy(alpha = 0.10f), start, end, strokeWidth * 6f)
    drawLine(color.copy(alpha = 0.22f), start, end, strokeWidth * 3.5f)
    drawLine(color.copy(alpha = 0.55f), start, end, strokeWidth * 1.8f)
    drawLine(color, start, end, strokeWidth)
}

private fun DrawScope.drawHudHexIconFrame(center: Offset, radius: Float, neon: Color) {
    val strokeOuter = 1.6f
    val strokeInner = 0.85f
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 5f), 0f)

    fun hexPath(r: Float): Path = Path().apply {
        for (i in 0..5) {
            val angle = (PI / 3.0 * i - PI / 2.0).toFloat()
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    fun vertex(i: Int, r: Float): Offset {
        val angle = (PI / 3.0 * i - PI / 2.0).toFloat()
        return Offset(center.x + r * cos(angle), center.y + r * sin(angle))
    }

    drawCircle(color = Color(0xFF060A0E), radius = radius + 1f, center = center)

    val outer = hexPath(radius)
    val inner = hexPath(radius - 4f)

    // Outer hex — glow + dashed stroke (gaps at vertices)
    drawPath(outer, color = neon.copy(alpha = 0.12f), style = Stroke(strokeOuter * 4f))
    drawPath(
        outer,
        color = neon,
        style = Stroke(width = strokeOuter, pathEffect = dashEffect),
    )

    // Inner hex — solid thin double line
    drawPath(
        inner,
        color = neon.copy(alpha = 0.65f),
        style = Stroke(width = strokeInner),
    )

    // Corner bracket ticks outside each vertex
    for (i in 0..5) {
        val v = vertex(i, radius + 3f)
        val vIn = vertex(i, radius)
        val dx = v.x - center.x
        val dy = v.y - center.y
        val len = kotlin.math.hypot(dx, dy)
        if (len > 0f) {
            val nx = dx / len
            val ny = dy / len
            val tickLen = 4f
            drawNeonStrokeLine(
                v,
                Offset(v.x + nx * tickLen, v.y + ny * tickLen),
                neon.copy(alpha = 0.7f),
                0.7f,
            )
        }
        drawCircle(neon.copy(alpha = 0.35f), 1.5f, vIn)
    }
}

@Composable
fun HudSystemStatusPanel(modifier: Modifier = Modifier) {
    HudPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SignalCellularAlt, null, tint = HudCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SYSTEM STATUS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HudCyanBright,
                letterSpacing = 1.sp,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HudStatusRow(Icons.Default.Memory, "CPU FREQUENCY", "240 MHz")
        HudStatusRow(Icons.Default.SdStorage, "FLASH SIZE", "4 MB")
        HudStatusRow(Icons.Default.Memory, "FREE HEAP", "—")
        HudStatusRow(Icons.Default.Schedule, "UPTIME", "—")
    }
}

@Composable
private fun HudStatusRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = HudCyan.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HudCyanMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = HudCyanBright,
        )
    }
}

@Composable
fun HudRoverViewport(modifier: Modifier = Modifier) {
    HudPanel(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height * 0.72f
                val platformR = size.minDimension * 0.38f
                for (i in 1..4) {
                    drawCircle(
                        color = HudCyan.copy(alpha = 0.12f),
                        radius = platformR * (i / 4f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f),
                    )
                }
                drawCircle(
                    color = HudCyan.copy(alpha = 0.3f),
                    radius = platformR,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                    ),
                )
                drawWireframeRover(Offset(cx, cy - platformR * 0.55f), platformR * 0.9f)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWireframeRover(
    center: Offset,
    scale: Float,
) {
    val s = scale / 100f
    val color = HudCyanBright
    val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round)
    val bodyW = 50f * s
    val bodyH = 22f * s
    val left = center.x - bodyW / 2
    val top = center.y - bodyH / 2
    drawRect(color, topLeft = Offset(left, top), size = Size(bodyW, bodyH), style = stroke)
    drawLine(color, Offset(left + bodyW * 0.2f, top), Offset(left + bodyW * 0.2f, top - 18f * s), 1.5f)
    drawLine(color, Offset(left + bodyW * 0.2f, top - 18f * s), Offset(left + bodyW * 0.45f, top - 18f * s), 1.5f)
    drawCircle(color, 4f * s, Offset(left + bodyW * 0.28f, top - 18f * s), style = stroke)
    drawCircle(color, 4f * s, Offset(left + bodyW * 0.38f, top - 18f * s), style = stroke)
    val wheelR = 10f * s
    listOf(
        Offset(left + 6f * s, top + bodyH),
        Offset(left + bodyW - 6f * s, top + bodyH),
        Offset(left + 6f * s, top + bodyH + 4f * s),
        Offset(left + bodyW - 6f * s, top + bodyH + 4f * s),
    ).forEach { wheelCenter ->
        drawLine(color, Offset(wheelCenter.x, top + bodyH * 0.7f), wheelCenter, 1.5f)
        drawCircle(color, wheelR, wheelCenter, style = stroke)
    }
    drawLine(color, Offset(left + bodyW - 8f * s, top + 4f * s), Offset(left + bodyW - 8f * s, top - 8f * s), 1.5f)
}

@Composable
fun HudHexStartButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    HudFramedBox(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        fillBackground = false,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = if (enabled) HudCyanBright else HudCyanBright.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) HudCyanBright else HudCyanBright.copy(alpha = 0.5f),
                    letterSpacing = 1.sp,
                )
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) HudCyanMuted else HudCyanMuted.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

@Composable
fun HudFooterBar(
    signalLabel: String,
    modeLabel: String,
    powerLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hudSimpleBorder(color = HudCyan.copy(alpha = 0.5f))
            .background(HudCyan.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudFooterSection(Icons.Default.SignalCellularAlt, "SIGNAL", signalLabel)
        HudFooterDivider()
        HudFooterSection(Icons.Default.SportsEsports, "MODE", modeLabel)
        HudFooterDivider()
        HudFooterSection(Icons.Default.Bolt, "POWER", powerLabel)
    }
}

@Composable
private fun HudFooterSection(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = HudCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = HudCyanMuted,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = HudCyanBright,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        HudMiniWaveform()
    }
}

@Composable
private fun HudFooterDivider() {
    Canvas(modifier = Modifier
        .height(36.dp)
        .width(1.dp)) {
        drawLine(
            HudCyan.copy(alpha = 0.3f),
            Offset(0f, 0f),
            Offset(0f, size.height),
            1f,
        )
    }
}

@Composable
private fun HudMiniWaveform() {
    Canvas(
        modifier = Modifier
            .width(48.dp)
            .height(12.dp),
    ) {
        val mid = size.height / 2
        val step = size.width / 8
        var x = 0f
        val points = listOf(0.3f, 0.7f, 0.5f, 1f, 0.4f, 0.8f, 0.6f, 0.3f, 0.5f)
        points.forEachIndexed { i, amp ->
            val y = mid - amp * mid * 0.8f
            if (i > 0) {
                drawLine(HudCyan.copy(alpha = 0.6f), Offset(x - step, mid), Offset(x, y), 1f)
            }
            x += step
        }
    }
}
