package com.monk.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════════
// MONK - Royal Silicon Valley Palette
// Understated luxury. Quiet confidence. Early Square meets Savile Row.
// ═══════════════════════════════════════════════════════════════════════════════

// Primary - Deep Charcoal (almost black, but warmer)
val Primary = Color(0xFF1A1A1A)
val PrimaryLight = Color(0xFF2D2D2D)
val PrimaryDark = Color(0xFF0D0D0D)

// Accent - Muted Brass/Gold (refined, not flashy)
val Accent = Color(0xFFB8977E)
val AccentLight = Color(0xFFD4C4B0)
val AccentDark = Color(0xFF8C7259)

// Surface Colors - Warm whites and creams
val Surface = Color(0xFFFAF9F7)
val SurfaceVariant = Color(0xFFF5F3F0)
val SurfaceElevated = Color(0xFFFFFFFF)

// Background
val Background = Color(0xFFFCFBF9)
val BackgroundSecondary = Color(0xFFF7F6F3)

// Text Colors - Rich, readable hierarchy
val OnPrimary = Color(0xFFFAF9F7)
val OnSurface = Color(0xFF1A1A1A)
val OnSurfaceVariant = Color(0xFF5C5C5C)
val TextMuted = Color(0xFF8A8A8A)
val TextHint = Color(0xFFB3B3B3)

// Status Colors - Muted, sophisticated
val Success = Color(0xFF4A6741)        // Forest green
val SuccessLight = Color(0xFFE8EFE6)
val Warning = Color(0xFFC4956A)        // Warm amber
val WarningLight = Color(0xFFFAF3ED)
val Error = Color(0xFF9B4D4D)          // Muted burgundy
val ErrorLight = Color(0xFFF5EAEA)

// Neutral Grays - Warm undertones
val Gray50 = Color(0xFFFAF9F7)
val Gray100 = Color(0xFFF5F3F0)
val Gray200 = Color(0xFFEBE8E4)
val Gray300 = Color(0xFFD9D5CF)
val Gray400 = Color(0xFFB8B3AB)
val Gray500 = Color(0xFF8A8580)
val Gray600 = Color(0xFF5C5955)
val Gray700 = Color(0xFF3D3B38)
val Gray800 = Color(0xFF262523)
val Gray900 = Color(0xFF1A1918)

// Focus Mode States
val FocusActive = Primary
val FocusInactive = Gray200
val FocusGlow = Accent.copy(alpha = 0.15f)

// Borders & Dividers
val Border = Color(0xFFE8E5E0)
val BorderLight = Color(0xFFF0EDE8)
val Divider = Color(0xFFEBE8E4)

// Legacy compatibility (keeping old names working)
val Purple80 = Accent
val PurpleGrey80 = Gray400
val Pink80 = AccentLight
val Purple40 = Primary
val PurpleGrey40 = Gray600
val Pink40 = AccentDark
