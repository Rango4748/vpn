package com.netshield.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A calmer, more premium palette than a literal gold-on-black skin:
// deep charcoal base + a single warm amber accent, used sparingly.
object NetShieldColors {
    val Background = Color(0xFF0B0C0E)
    val Surface = Color(0xFF15171A)
    val SurfaceVariant = Color(0xFF1D2024)
    val Accent = Color(0xFFE0A73C)       // muted amber, not neon gold
    val AccentDim = Color(0xFF8C6A26)
    val TextPrimary = Color(0xFFF5F3EF)
    val TextSecondary = Color(0xFF9A9C9F)
    val Success = Color(0xFF4CD37B)
    val Danger = Color(0xFFE5544D)
    val Divider = Color(0xFF262A2E)
}

private val DarkScheme = darkColorScheme(
    primary = NetShieldColors.Accent,
    onPrimary = Color(0xFF1A1200),
    background = NetShieldColors.Background,
    onBackground = NetShieldColors.TextPrimary,
    surface = NetShieldColors.Surface,
    onSurface = NetShieldColors.TextPrimary,
    surfaceVariant = NetShieldColors.SurfaceVariant,
    error = NetShieldColors.Danger
)

@Composable
fun NetShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = NetShieldColors.TextPrimary),
            titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, color = NetShieldColors.TextPrimary),
            bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = NetShieldColors.TextSecondary)
        ),
        content = content
    )
}
