package ai.openclaw.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ai.openclaw.app.R

// ---------------------------------------------------------------------------
// MobileColors – semantic color tokens with light + dark variants
// ---------------------------------------------------------------------------

internal data class MobileColors(
  val surface: Color,
  val surfaceStrong: Color,
  val cardSurface: Color,
  val border: Color,
  val borderStrong: Color,
  val text: Color,
  val textSecondary: Color,
  val textTertiary: Color,
  val accent: Color,
  val accentSoft: Color,
  val accentBorderStrong: Color,
  val success: Color,
  val successSoft: Color,
  val warning: Color,
  val warningSoft: Color,
  val danger: Color,
  val dangerSoft: Color,
  val codeBg: Color,
  val codeText: Color,
  val codeBorder: Color,
  val codeAccent: Color,
  val chipBorderConnected: Color,
  val chipBorderConnecting: Color,
  val chipBorderWarning: Color,
  val chipBorderError: Color,
)

internal fun lightMobileColors() = AirDesignTokens.lightColors()

internal fun darkMobileColors() = AirDesignTokens.darkColors()

internal val LocalMobileColors = staticCompositionLocalOf { lightMobileColors() }

internal object MobileColorsAccessor {
  val current: MobileColors
    @Composable get() = LocalMobileColors.current
}

// ---------------------------------------------------------------------------
// Backward-compatible top-level accessors (composable getters)
// ---------------------------------------------------------------------------
// These allow existing call sites to keep using `mobileSurface`, `mobileText`, etc.
// without converting every file at once. Each resolves to the themed value.

internal val mobileSurface: Color @Composable get() = LocalMobileColors.current.surface
internal val mobileSurfaceStrong: Color @Composable get() = LocalMobileColors.current.surfaceStrong
internal val mobileCardSurface: Color @Composable get() = LocalMobileColors.current.cardSurface
internal val mobileBorder: Color @Composable get() = LocalMobileColors.current.border
internal val mobileBorderStrong: Color @Composable get() = LocalMobileColors.current.borderStrong
internal val mobileText: Color @Composable get() = LocalMobileColors.current.text
internal val mobileTextSecondary: Color @Composable get() = LocalMobileColors.current.textSecondary
internal val mobileTextTertiary: Color @Composable get() = LocalMobileColors.current.textTertiary
internal val mobileAccent: Color @Composable get() = LocalMobileColors.current.accent
internal val mobileAccentSoft: Color @Composable get() = LocalMobileColors.current.accentSoft
internal val mobileAccentBorderStrong: Color @Composable get() = LocalMobileColors.current.accentBorderStrong
internal val mobileSuccess: Color @Composable get() = LocalMobileColors.current.success
internal val mobileSuccessSoft: Color @Composable get() = LocalMobileColors.current.successSoft
internal val mobileWarning: Color @Composable get() = LocalMobileColors.current.warning
internal val mobileWarningSoft: Color @Composable get() = LocalMobileColors.current.warningSoft
internal val mobileDanger: Color @Composable get() = LocalMobileColors.current.danger
internal val mobileDangerSoft: Color @Composable get() = LocalMobileColors.current.dangerSoft
internal val mobileCodeBg: Color @Composable get() = LocalMobileColors.current.codeBg
internal val mobileCodeText: Color @Composable get() = LocalMobileColors.current.codeText
internal val mobileCodeBorder: Color @Composable get() = LocalMobileColors.current.codeBorder
internal val mobileCodeAccent: Color @Composable get() = LocalMobileColors.current.codeAccent

// Background gradient – light fades white→gray, dark fades near-black→dark-gray
internal val mobileBackgroundGradient: Brush
  @Composable get() {
    val colors = LocalMobileColors.current
    return Brush.verticalGradient(
      listOf(
        colors.surface,
        colors.surfaceStrong,
        colors.surfaceStrong,
      ),
    )
  }

// ---------------------------------------------------------------------------
// Typography tokens (theme-independent)
// ---------------------------------------------------------------------------

internal val mobileHeadingFontFamily =
  FontFamily(
    Font(resId = R.font.poppins_400_regular, weight = FontWeight.Normal),
    Font(resId = R.font.poppins_500_medium, weight = FontWeight.Medium),
    Font(resId = R.font.poppins_600_semibold, weight = FontWeight.SemiBold),
  )

internal val mobileBodyFontFamily =
  FontFamily(
    Font(resId = R.font.dm_sans_400_regular, weight = FontWeight.Normal),
    Font(resId = R.font.dm_sans_600_semibold, weight = FontWeight.SemiBold),
  )

internal val mobileCodeFontFamily =
  FontFamily.Monospace

internal val mobileFontFamily = mobileBodyFontFamily

internal val mobileDisplay =
  TextStyle(
    fontFamily = mobileHeadingFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 32.sp,
    lineHeight = 41.sp,
    letterSpacing = (-1.28).sp,
  )

internal val mobileTitle1 =
  TextStyle(
    fontFamily = mobileHeadingFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = (-1.12).sp,
  )

internal val mobileTitle2 =
  TextStyle(
    fontFamily = mobileHeadingFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 22.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.88).sp,
  )

internal val mobileHeadline =
  TextStyle(
    fontFamily = mobileHeadingFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 19.sp,
    lineHeight = 27.sp,
    letterSpacing = (-0.76).sp,
  )

internal val mobileBody =
  TextStyle(
    fontFamily = mobileBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  )

internal val mobileCallout =
  TextStyle(
    fontFamily = mobileBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 21.sp,
  )

internal val mobileCaption1 =
  TextStyle(
    fontFamily = mobileBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp,
  )

internal val mobileCaption2 =
  TextStyle(
    fontFamily = mobileBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp,
  )
