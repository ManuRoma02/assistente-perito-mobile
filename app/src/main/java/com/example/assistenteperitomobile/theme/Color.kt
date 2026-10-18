package com.example.assistenteperitomobile.theme

import androidx.compose.ui.graphics.Color

// Davide Restelli ("davez") Architectural Palette + Apple HIG Light Mode Craft (/apple-design)
// Extracted directly from C:\Users\manue\Dropbox\davez\src\styles.css & homepage.component.css:
//   --brand-color: #4f0b18 | --brand-color-hover: #6a1628 | --brand-color-soft: #7d2a3b
//   .wing-edilizia: #5c3a40 | .wing-fotografia: #3a4f5c
//   --background-color-1: #f8f4f1 | --background-color-2: #efe7e2 | --background-color-3: #d7c9bf
//   --text-color-dark: #191716 | --text-color-medium: #2f2b2a | --text-color-light-medium: #6f6a69

val DavezBordeaux = Color(0xFF4F0B18)           // --brand-color (Deep Imperial Bordeaux)
val DavezBordeauxHover = Color(0xFF6A1628)      // --brand-color-hover (Rich Crimson Wine)
val DavezBordeauxSoft = Color(0xFF7D2A3B)       // --brand-color-soft (Warm Terracotta Burgundy)
val DavezEdiliziaCharcoal = Color(0xFF5C3A40)   // .wing-edilizia (Warm Architectural Charcoal Burgundy)
val DavezFotografiaSlate = Color(0xFF3A4F5C)    // .wing-fotografia (Cool Architectural Slate)
val DavezAlabaster = Color(0xFFF8F4F1)          // --background-color-1 (Alabaster Chalk)
val DavezTravertine = Color(0xFFEFE7E2)         // --background-color-2 (Warm Travertine)
val DavezStoneBorder = Color(0xFFD7C9BF)        // --background-color-3 (Warm Stone Border)

// Semantic UI Tokens mapped to davez Architecture + Apple HIG
val AppleWhite = Color(0xFFFFFFFF)              // --surface-elevated pure white cards & sheets
val AppleGroupedBg = Color(0xFFF4EDE8)          // Warm Alabaster/Travertine surface for AI bubbles & pills
val AppleSecondaryGroupedBg = Color(0xFFE4D9D0) // Hairline architectural stone borders
val AppleTertiaryGroupedBg = Color(0xFFD7C9BF)  // --background-color-3

val AppleBlue = Color(0xFF5E1122)               // Primary Architectural Bordeaux accent & User Bubble
val AppleBlueSoft = Color(0xFFF7ECEF)           // Soft Bordeaux-tinted surface for inline pills & chips
val AppleBlueDark = Color(0xFF4F0B18)           // --brand-color Deep Bordeaux

val AppleGreen = Color(0xFF2B7A4B)              // Architectural Emerald/Sage (Online dot, Verified, Active GPU)
val AppleGreenSoft = Color(0xFFEAF5EE)

val AppleOrange = Color(0xFFD97706)             // Warm Ochre/Amber (Conflict Warning)
val AppleOrangeSoft = Color(0xFFFEF3E2)

val AppleRed = Color(0xFFDC2626)                // Destructive / Live Recording Pulse
val AppleRedSoft = Color(0xFFFEECEC)

val AppleLabelPrimary = Color(0xFF191716)       // --text-color-dark (Deep Espresso, >15:1 contrast)
val AppleLabelSecondary = Color(0xFF433D3B)     // --text-color-medium (Warm Graphite)
val AppleLabelTertiary = Color(0xFF6F6A69)      // --text-color-light-medium (Muted Stone)
