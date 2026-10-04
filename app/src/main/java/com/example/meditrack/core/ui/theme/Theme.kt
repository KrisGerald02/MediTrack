package com.example.meditrack.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun appColorScheme(theme: String) = when (theme) {
    "PURPLE" -> lightColorScheme(
        primary = Color(0xFF7C3AED), primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF4C1D95), secondary = Color(0xFFA855F7),
        secondaryContainer = Color(0xFFF3E8FF), tertiary = Color(0xFF9333EA),
        tertiaryContainer = Color(0xFFF3E8FF), background = Color(0xFFFCF9FF),
        surface = Color.White, surfaceVariant = Color(0xFFF3EEFA), onBackground = Color(0xFF271638),
        onSurface = Color(0xFF271638), onSurfaceVariant = Color(0xFF715E7E)
    )
    "BLUE" -> lightColorScheme(
        primary = Color(0xFF2563EB), primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A), secondary = Color(0xFF0EA5E9),
        secondaryContainer = Color(0xFFE0F2FE), tertiary = Color(0xFF3B82F6),
        tertiaryContainer = Color(0xFFDDEAFE), background = Color(0xFFF7FAFF),
        surface = Color.White, surfaceVariant = Color(0xFFEDF3FC), onBackground = Color(0xFF17233B),
        onSurface = Color(0xFF17233B), onSurfaceVariant = Color(0xFF5B6980)
    )
    "GREEN" -> lightColorScheme(
        primary = Color(0xFF168A62), primaryContainer = Color(0xFFD7F5E9),
        onPrimaryContainer = Color(0xFF075A3D), secondary = Color(0xFF22A879),
        secondaryContainer = Color(0xFFE2F7EF), tertiary = Color(0xFF36A270),
        tertiaryContainer = Color(0xFFDDF5E9), background = Color(0xFFF7FCFA),
        surface = Color.White, surfaceVariant = Color(0xFFEDF7F3), onBackground = Color(0xFF17372C),
        onSurface = Color(0xFF17372C), onSurfaceVariant = Color(0xFF59736A)
    )
    else -> lightColorScheme(
        primary = Blue, onPrimary = Color.White, primaryContainer = BlueContainer,
        onPrimaryContainer = BlueDark, secondary = Teal, secondaryContainer = Mint,
        tertiary = Coral, tertiaryContainer = Color(0xFFFFE3ED), onTertiaryContainer = Color(0xFF71113B),
        background = AppBackground, surface = Color.White, surfaceVariant = SurfaceSoft,
        onBackground = Ink, onSurface = Ink, onSurfaceVariant = Muted, error = Color(0xFFBA1A1A)
    )
}

@Composable
fun MediTrackTheme(
    theme: String = "PINK",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = appColorScheme(theme),
        typography = MediTrackTypography,
        content = content
    )
}
