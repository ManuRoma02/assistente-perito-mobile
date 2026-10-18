package com.example.assistenteperitomobile.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun AssistentePeritoMobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DavezAppleLightColorScheme,
        typography = Typography,
        content = content
    )
}
