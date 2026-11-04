package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Core Triad: JET BLACK, ROYAL AMETHYST, ELECTRIC CYAN
// ============================================================================

// 1. JET BLACK (Deep, rich obsidian & onyx tones)
val JetBlack = Color(0xFF09090D)             // Absolute background
val JetBlackElevated = Color(0xFF111118)     // Navigation / TopAppBar
val JetBlackCard = Color(0xFF171722)         // Card background
val JetBlackCardHover = Color(0xFF1E1E2C)    // Elevated surface / interactive
val JetBlackBorder = Color(0xFF28283C)       // Card borders & outlines
val JetBlackMuted = Color(0xFF45455E)        // Muted labels & dividers

// 2. ROYAL AMETHYST (Vivid, luxurious, commanding purples)
val RoyalAmethyst = Color(0xFF8B5CF6)        // Primary vibrant amethyst
val RoyalAmethystDeep = Color(0xFF6D28D9)    // Rich royal shade
val RoyalAmethystVivid = Color(0xFF7C3AED)   // Accent high-contrast
val RoyalAmethystGlow = Color(0xFFA78BFA)    // Amethyst glow / text
val RoyalAmethystContainer = Color(0xFF2E1065) // Dark amethyst container
val RoyalAmethystSubtle = Color(0xFF1E1238)  // Ambient background tint

// 3. ELECTRIC CYAN (Luminous, futuristic, crisp highlights)
val ElectricCyan = Color(0xFF00F0FF)         // Luminous electric cyan
val ElectricCyanNeon = Color(0xFF00E5FF)     // Pure neon cyan
val ElectricCyanGlow = Color(0xFF38BDF8)     // Soft cyan glow
val ElectricCyanMuted = Color(0xFF0891B2)    // Secondary cyan
val ElectricCyanContainer = Color(0xFF083344)// Dark cyan container
val ElectricCyanSubtle = Color(0xFF04202C)   // Ambient cyan tint

// Backward compatibility alias bindings
val BrandPrimary = RoyalAmethyst
val BrandPrimaryDark = RoyalAmethystDeep
val BrandPrimaryLight = RoyalAmethystGlow
val BrandSecondary = JetBlackCard
val BrandSecondaryLight = JetBlackCardHover
val BrandTertiary = ElectricCyan

// Status colors optimized for Jet Black & Amethyst theme
val StatusGreen = Color(0xFF10B981)
val StatusGreenBg = Color(0xFF064E3B)
val StatusRed = Color(0xFFF43F5E)
val StatusRedBg = Color(0xFF4C0519)
val StatusAmber = Color(0xFFF59E0B)
val StatusAmberBg = Color(0xFF451A03)
val StatusBlue = Color(0xFF38BDF8)
val StatusBlueBg = Color(0xFF0C4A6E)
val StatusCyan = ElectricCyan
val StatusCyanBg = ElectricCyanContainer
val StatusAmethyst = RoyalAmethyst
val StatusAmethystBg = RoyalAmethystContainer

// Standard surface mappings
val DarkBackground = JetBlack
val DarkSurface = JetBlackElevated
val DarkSurfaceVariant = JetBlackCard
val DarkOutline = JetBlackBorder

val LightBackground = Color(0xFF0D0D14)      // Even in default mode, respect the luxury Jet Black aesthetic
val LightSurface = JetBlackElevated
val LightSurfaceVariant = JetBlackCard
val LightOutline = JetBlackBorder
