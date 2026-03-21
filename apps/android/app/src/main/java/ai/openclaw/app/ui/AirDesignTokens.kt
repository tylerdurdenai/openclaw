package ai.openclaw.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object AirDesignTokens {
  val cornerSmall = 8.dp
  val cornerListItem = 16.dp
  val cornerContainer = 32.dp
  val cornerRounded = 1000.dp

  fun lightColors() =
    MobileColors(
      surface = Color(0xFFFAFAFA),
      surfaceStrong = Color(0xFFE3E9EA),
      cardSurface = Color(0xFFF7F7F7),
      border = Color(0xFFBDBDBD),
      borderStrong = Color(0xFFAAB5B8),
      text = Color(0xFF181B1C),
      textSecondary = Color(0xFF798B95),
      textTertiary = Color(0xFFB8CAD4),
      accent = Color(0xFF41A7D7),
      accentSoft = Color(0xFFDCEFF8),
      accentBorderStrong = Color(0xFF2C8AB6),
      success = Color(0xFF17B26A),
      successSoft = Color(0xFFDCEFEB),
      warning = Color(0xFFF99247),
      warningSoft = Color(0xFFFFDDC4),
      danger = Color(0xFFF3383B),
      dangerSoft = Color(0xFFFECDCA),
      codeBg = Color(0xFF141718),
      codeText = Color(0xFFE8ECEE),
      codeBorder = Color(0xFF2A2F32),
      codeAccent = Color(0xFF5BB8E0),
      chipBorderConnected = Color(0xFF8ED6B3),
      chipBorderConnecting = Color(0xFF8CCFEA),
      chipBorderWarning = Color(0xFFF4B07D),
      chipBorderError = Color(0xFFF3A0A2),
    )

  fun darkColors() =
    MobileColors(
      surface = Color(0xFF0F1112),
      surfaceStrong = Color(0xFF1A1D1F),
      cardSurface = Color(0xFF141718),
      border = Color(0xFF2E3336),
      borderStrong = Color(0xFF3C454A),
      text = Color(0xFFE8ECEE),
      textSecondary = Color(0xFF8A9BA5),
      textTertiary = Color(0xFF6C7B84),
      accent = Color(0xFF5BB8E0),
      accentSoft = Color(0xFF1A2A33),
      accentBorderStrong = Color(0xFF4A9BC0),
      success = Color(0xFF2CCD83),
      successSoft = Color(0xFF17362B),
      warning = Color(0xFFFFA86A),
      warningSoft = Color(0xFF3A281A),
      danger = Color(0xFFFF676A),
      dangerSoft = Color(0xFF3F1C1D),
      codeBg = Color(0xFF0E1011),
      codeText = Color(0xFFE8ECEE),
      codeBorder = Color(0xFF2A2F32),
      codeAccent = Color(0xFF5BB8E0),
      chipBorderConnected = Color(0xFF22503D),
      chipBorderConnecting = Color(0xFF244A5B),
      chipBorderWarning = Color(0xFF5A3A24),
      chipBorderError = Color(0xFF5C2628),
    )
}
