package com.example.assistenteperitomobile.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PeritoSemanticColors(
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val groupedBg: Color,
    val secondaryGroupedBg: Color,
    val labelTertiary: Color,
    val brandSoft: Color
)

private val LightPeritoSemanticColors = PeritoSemanticColors(
    success = AppleGreen,
    successSoft = AppleGreenSoft,
    warning = AppleOrange,
    warningSoft = AppleOrangeSoft,
    groupedBg = AppleGroupedBg,
    secondaryGroupedBg = AppleSecondaryGroupedBg,
    labelTertiary = AppleLabelTertiary,
    brandSoft = AppleBlueSoft
)

private val DarkPeritoSemanticColors = PeritoSemanticColors(
    success = Color(0xFF4ADE80),
    successSoft = Color(0xFF1B4D2E),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0xFF422A08),
    groupedBg = Color(0xFF221F1E),
    secondaryGroupedBg = Color(0xFF383331),
    labelTertiary = Color(0xFF9C908C),
    brandSoft = Color(0xFF5C1523)
)

val LocalPeritoSemanticColors = staticCompositionLocalOf { LightPeritoSemanticColors }

private val DavezAppleLightColorScheme = lightColorScheme(
    primary = DavezBordeaux,
    onPrimary = AppleWhite,
    primaryContainer = AppleBlueSoft,
    onPrimaryContainer = DavezBordeaux,
    secondary = AppleGreen,
    onSecondary = AppleWhite,
    secondaryContainer = AppleGreenSoft,
    onSecondaryContainer = AppleLabelPrimary,
    tertiary = DavezFotografiaSlate,
    onTertiary = AppleWhite,
    tertiaryContainer = DavezTravertine,
    onTertiaryContainer = AppleLabelPrimary,
    error = AppleRed,
    onError = AppleWhite,
    errorContainer = AppleRedSoft,
    onErrorContainer = AppleRed,
    background = DavezAlabaster,
    onBackground = AppleLabelPrimary,
    surface = AppleWhite,
    onSurface = AppleLabelPrimary,
    surfaceVariant = AppleGroupedBg,
    onSurfaceVariant = AppleLabelSecondary,
    surfaceContainerLowest = AppleWhite,
    surfaceContainerLow = DavezAlabaster,
    surfaceContainer = AppleGroupedBg,
    surfaceContainerHigh = DavezTravertine,
    surfaceContainerHighest = AppleSecondaryGroupedBg,
    outline = DavezStoneBorder,
    outlineVariant = AppleSecondaryGroupedBg
)

private val DavezAppleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE27D8C),
    onPrimary = Color(0xFF2E000A),
    primaryContainer = Color(0xFF5C1523),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF003919),
    secondaryContainer = Color(0xFF1B4D2E),
    onSecondaryContainer = Color(0xFFB4F1C5),
    tertiary = Color(0xFF90B4CE),
    onTertiary = Color(0xFF00344D),
    tertiaryContainer = Color(0xFF24485D),
    onTertiaryContainer = Color(0xFFCBE7FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141211),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF1E1B1A),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF2D2826),
    onSurfaceVariant = Color(0xFFD0C3BE),
    surfaceContainerLowest = Color(0xFF0F0D0C),
    surfaceContainerLow = Color(0xFF1A1716),
    surfaceContainer = Color(0xFF221F1E),
    surfaceContainerHigh = Color(0xFF2C2827),
    surfaceContainerHighest = Color(0xFF383331),
    outline = Color(0xFF554D4A),
    outlineVariant = Color(0xFF3E3836)
)

@Composable
fun AssistentePeritoMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DavezAppleDarkColorScheme else DavezAppleLightColorScheme
    val semanticColors = if (darkTheme) DarkPeritoSemanticColors else LightPeritoSemanticColors
    CompositionLocalProvider(LocalPeritoSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
