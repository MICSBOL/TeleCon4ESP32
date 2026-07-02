package com.micsbol.telecon4esp32.ui.cyber.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.theme.syncopate

/**
 * Color system for the cyber-industrial dashboard.
 * Centralised so every component pulls from the same palette.
 */
object CyberColors {
    val NeonPrimary = Color(0xFF18F0FF)
    val NeonSecondary = Color(0xFF00AEEF)
    val PanelBlue = Color(0xFF071426)
    val Background = Color(0xFF03060B)
    val BackgroundDeep = Color(0xFF05080D)

    val Danger = Color(0xFFFF4D4D)
    val Warning = Color(0xFFFFD84D)
    val Success = Color(0xFF4DFF9B)

    val TextPrimary = Color(0xFFD9E7F5)
    val TextSecondary = Color(0xFF8B9CB3)

    // Derived panel fills used for the layered gradient look.
    val PanelFillTop = Color(0xFF0A1B30)
    val PanelFillBottom = Color(0xFF050B16)
    val PanelStroke = NeonSecondary.copy(alpha = 0.55f)
    val InnerEdge = NeonPrimary.copy(alpha = 0.12f)
}

/**
 * Orbitron-like typography hierarchy. The project ships the [syncopate] family
 * which reads as a futuristic, slightly-expanded display face.
 */
object CyberType {
    val Title = TextStyle(
        fontFamily = syncopate,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.5.sp,
        color = CyberColors.NeonPrimary,
    )

    val SectionTitle = TextStyle(
        fontFamily = syncopate,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 1.2.sp,
        color = CyberColors.NeonPrimary,
    )

    val Label = TextStyle(
        fontFamily = syncopate,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
        color = CyberColors.TextSecondary,
    )

    val TelemetryValue = TextStyle(
        fontFamily = syncopate,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.8.sp,
        color = CyberColors.TextPrimary,
    )

    val Meta = TextStyle(
        fontFamily = syncopate,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.sp,
        color = CyberColors.TextSecondary,
    )
}
