package com.autolounge.park.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.autolounge.park.R

/**
 * AutoLounge Luxury Cockpit Dynamic Palette (Dual Mode: Day Light & Night Dark)
 */
data class AutoLoungeColors(
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceBorder: Color,
    val textPure: Color,
    val textMuted: Color,
    val textDim: Color,
    val primary: Color,
    val secondary: Color,
    val isDark: Boolean
)

fun darkAutoLoungeColors(theme: CarVisualTheme): AutoLoungeColors = AutoLoungeColors(
    background = Color(0xFF0E131D),     // Stitch surface background
    surface = Color(0xFF0E131D),        // Stitch surface
    surfaceCard = Color(0xFF171C26),    // Stitch surface-container-low
    surfaceBorder = Color(0xFF252A35),  // Stitch surface-container-high / hairline
    textPure = Color(0xFFDEE2F1),       // Stitch on-surface
    textMuted = Color(0xFFBAC9CC),      // Stitch on-surface-variant
    textDim = Color(0xFF6F7B8A),        // Stitch dim label
    primary = Color(0xFF00E5FF),        // Stitch primary-container (Hyper Cyan)
    secondary = Color(0xFFFABC4D),      // Stitch secondary (Warm Gold)
    isDark = true
)

fun lightAutoLoungeColors(theme: CarVisualTheme): AutoLoungeColors = AutoLoungeColors(
    background = Color(0xFFF1F5F9),     // Daylight cockpit
    surface = Color(0xFFFFFFFF),        // Pure white panel
    surfaceCard = Color(0xFFE8EEF5),    // Crisp daylight tile
    surfaceBorder = Color(0xFFCBD5E1),  // Soft metallic hairline
    textPure = Color(0xFF090D16),       // Deep text
    textMuted = Color(0xFF475569),      // Neutral slate
    textDim = Color(0xFF64748B),        // Muted slate
    primary = Color(0xFF00B8D4),
    secondary = Color(0xFFFABC4D),
    isDark = false
)

val LocalAutoLoungeColors = staticCompositionLocalOf {
    darkAutoLoungeColors(CarVisualTheme.STEALTH_PLATINUM)
}

/**
 * Dynamic automotive color palette references.
 */
object AutoLoungePalette {
    val ObsidianVoid: Color @Composable get() = LocalAutoLoungeColors.current.background
    val CarbonElevated: Color @Composable get() = LocalAutoLoungeColors.current.surface
    val SurfaceCard: Color @Composable get() = LocalAutoLoungeColors.current.surfaceCard
    val SurfaceBorder: Color @Composable get() = LocalAutoLoungeColors.current.surfaceBorder
    val TextPure: Color @Composable get() = LocalAutoLoungeColors.current.textPure
    val TextMuted: Color @Composable get() = LocalAutoLoungeColors.current.textMuted
    val TextDim: Color @Composable get() = LocalAutoLoungeColors.current.textDim
    val IsDark: Boolean @Composable get() = LocalAutoLoungeColors.current.isDark

    // Automotive accent colors (Google Stitch OEM palette)
    val PlatinumPure = Color(0xFFDEE2F1)     // Stitch on-surface (Platin)
    val AcidMint = Color(0xFF5BE9AD)         // Stitch tertiary-container (Lucid Mint)
    val NordicFrost = Color(0xFF00E5FF)      // Stitch primary-container (Hyper Cyan)
    val SatinTitanium = Color(0xFFBAC9CC)    // Stitch on-surface-variant (Titanium)
    val AmberSignal = Color(0xFFFABC4D)      // Stitch secondary (Warm Gold / VIP)
    val CrimsonWarning = Color(0xFFFF5252)   // Alert Red

    // Backward compatibility aliases
    val EmeraldNeon = AcidMint
    val SapphireGlow = NordicFrost
    val GoldChampagne = AmberSignal
}

enum class CarVisualTheme(val id: String, val title: String, val primary: Color, val secondary: Color) {
    STEALTH_PLATINUM("stealth_platinum", "Stealth Platin", AutoLoungePalette.PlatinumPure, AutoLoungePalette.SatinTitanium),
    ACID_MINT("acid_mint", "Acid Mint", AutoLoungePalette.AcidMint, AutoLoungePalette.PlatinumPure),
    NORDIC_FROST("nordic_frost", "Nordic Frost", AutoLoungePalette.NordicFrost, AutoLoungePalette.PlatinumPure),
    SATIN_TITANIUM("satin_titanium", "Satin Titanyum", AutoLoungePalette.SatinTitanium, AutoLoungePalette.PlatinumPure),
    WARM_AMBER("warm_amber", "Warm Amber", AutoLoungePalette.AmberSignal, AutoLoungePalette.PlatinumPure);

    companion object {
        val ONYX_EMERALD get() = ACID_MINT
        val CYBER_SAPPHIRE get() = NORDIC_FROST
        val CHAMPAGNE_GOLD get() = WARM_AMBER
        val CARBON_STEALTH get() = SATIN_TITANIUM

        fun fromId(id: String): CarVisualTheme = when (id) {
            "onyx_emerald" -> ACID_MINT
            "cyber_sapphire" -> NORDIC_FROST
            "champagne_gold" -> WARM_AMBER
            "carbon_stealth" -> SATIN_TITANIUM
            else -> entries.find { it.id == id } ?: STEALTH_PLATINUM
        }
    }
}

val AppFontFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Light),
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
    Font(R.font.manrope_extrabold, FontWeight.Black)
)

/**
 * AutoLounge Otomotiv Tipografi Sistemi (Material 3 Tam Hiyerarşi)
 * Araç ekranları (800x480 - 1920x1080) ve mobil cihazlarda yüksek glanceability (hızlı bakışta okunurluk),
 * dengeli harf aralığı (letter-spacing) ve satır yüksekliği (line-height) sunar.
 */
val AutoLoungeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.1).sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.35.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun AutoLoungeTheme(
    carTheme: CarVisualTheme = CarVisualTheme.STEALTH_PLATINUM,
    themeMode: String = "system", // "system" | "dark" | "light"
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val colors = if (isDark) darkAutoLoungeColors(carTheme) else lightAutoLoungeColors(carTheme)

    val colorScheme = if (isDark) {
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceCard,
            primary = colors.primary,
            secondary = colors.secondary,
            onBackground = colors.textPure,
            onSurface = colors.textPure,
            onSurfaceVariant = colors.textMuted,
            outline = colors.surfaceBorder,
            error = AutoLoungePalette.CrimsonWarning
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.surfaceCard,
            primary = colors.primary,
            secondary = colors.secondary,
            onBackground = colors.textPure,
            onSurface = colors.textPure,
            onSurfaceVariant = colors.textMuted,
            outline = colors.surfaceBorder,
            error = AutoLoungePalette.CrimsonWarning
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAutoLoungeColors provides colors,
        LocalTextStyle provides AutoLoungeTypography.bodyMedium,
        // Arapça seçiliyse tüm arayüz sağdan sola
        androidx.compose.ui.platform.LocalLayoutDirection provides
            if (com.autolounge.park.i18n.I18n.isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl
            else androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AutoLoungeTypography,
            content = content
        )
    }
}
