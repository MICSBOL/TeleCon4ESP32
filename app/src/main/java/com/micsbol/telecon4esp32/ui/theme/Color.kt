package com.micsbol.telecon4esp32.ui.theme

import androidx.compose.ui.graphics.Color

// --- HUD neon cyan palette ---
val HudCyan = Color(0xFF00E5FF)
val HudCyanBright = Color(0xFF00FFFF)
val HudCyanDim = Color(0xFF00B8D4)
val HudCyanGlow = Color(0x6600E5FF)
val HudCyanMuted = Color(0x9900E5FF)
val HudFrameAccent = Color(0xFF50E3FE)
val HudMenuNeon = Color(0xFF50E3FE)
val HudMenuBg = Color(0xFF080C10)
val HudTextSecondary = Color(0xFFE0F0F5)

val TechBlue = HudCyanDim
val TechBlueBright = HudCyan
val TechBlueDark = Color(0xFF007A8F)

val TechCyan = HudCyan
val TechCyanBright = HudCyanBright
val TechCyanDark = HudCyanDim

val TechOnPrimary = Color(0xFF001018)
val TechOnAccent = Color(0xFF050A12)

val EmitterViolet = HudCyan
val EmitterVioletLight = HudCyanBright
val EmitterVioletDark = HudCyanDim
val EmitterOnViolet = TechOnPrimary

val EmitterAmber = HudCyanBright
val EmitterAmberDark = HudCyanDim
val EmitterOnAmber = TechOnAccent

val CyanPrimary = HudCyan
val CyanPrimaryDark = HudCyanDim

// --- Light surfaces (HUD-tinted slate) ---
val LightBackground = Color(0xFF0C1220)
val LightSurface = Color(0xFF111B2E)
val LightSurfaceVariant = Color(0xFF1A2840)
val LightOnBackground = Color(0xFFE0F7FA)
val LightOnSurface = Color(0xFFB2EBF2)
val LightOnSurfaceVariant = Color(0xFF80DEEA)
val LightOutline = Color(0xFF00E5FF)
val LightPrimaryContainer = Color(0xFF0D2137)

// --- Dark surfaces (deep space navy) ---
val DarkBackground = Color(0xFF050A12)
val DarkSurface = Color(0xFF0A1420)
val DarkSurfaceVariant = Color(0xFF0F1E30)
val DarkOnBackground = Color(0xFFE0F7FA)
val DarkOnSurface = Color(0xFFB2EBF2)
val DarkOnSurfaceVariant = Color(0xFF4DD0E1)
val DarkOutline = Color(0xFF00E5FF)
val DarkPrimaryContainer = Color(0xFF0D2137)

// --- Status ---
val StatusConnected = HudCyanBright
val StatusDisconnected = Color(0xFFFF5252)

// --- Hardware / plot accents ---
val AccentOrange = Color(0xFFFFB300)
val AccentRed = Color(0xFFEF5350)

val AccentBlue = HudCyan
val AccentGreen = Color(0xFF39FF14)
val DarkGridLine = Color(0x6600E5FF)
val DarkAxisLine = Color(0x9900E5FF)
val LightGridLine = Color(0x4400E5FF)
val LightAxisLine = Color(0x7700E5FF)

val PlotCyan = HudCyan
val PlotRed = Color(0xFFF43F5E)
val PlotYellow = Color(0xFFFACC15)
val PlotGreen = Color(0xFF4ADE80)
val PlotMagenta = Color(0xFFD946EF)
val PlotOrange = Color(0xFFF97316)

val backgroundSplash = DarkBackground
val backgroundPurple = DarkPrimaryContainer
val titleColor = DarkOnBackground
