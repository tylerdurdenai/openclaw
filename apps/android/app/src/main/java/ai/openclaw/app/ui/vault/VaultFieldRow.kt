package ai.openclaw.app.ui.vault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileCardSurface
import ai.openclaw.app.ui.mobileCaption1
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary

@Composable
fun VaultFieldRow(
  label: String,
  maskedValue: String,
  tier: Int,
  onReveal: () -> Unit,
  onEdit: () -> Unit,
) {
  val tierText = when (tier) {
    1 -> "🟢 Auto"
    2 -> "🟡 Biometric"
    else -> "🔴 Biometric+"
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = mobileCardSurface,
    border = BorderStroke(1.dp, mobileBorder),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = mobileCaption1, color = mobileTextSecondary)
        Text(text = maskedValue.ifBlank { "—" }, color = mobileText, style = mobileCaption1)
        Text(text = tierText, color = mobileTextSecondary, style = mobileCaption1)
      }
      Row {
        IconButton(onClick = onReveal) {
          Icon(imageVector = Icons.Default.Visibility, contentDescription = "Reveal")
        }
        IconButton(onClick = onEdit) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
        }
      }
    }
  }
}
