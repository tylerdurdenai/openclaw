package ai.openclaw.app.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VaultEditDialog(
  field: String,
  initialValue: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var value by remember(field, initialValue) { mutableStateOf(initialValue) }
  var error by remember(field, initialValue) { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Updating the records.") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
          value = value,
          onValueChange = {
            value = it
            error = null
          },
          label = { Text(field) },
          isError = error != null,
        )
        error?.let { Text(it) }
      }
    },
    confirmButton = {
      Button(onClick = {
        val validated = validateField(field, value)
        if (validated != null) {
          error = validated
        } else {
          onSave(value)
        }
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

private fun validateField(field: String, value: String): String? {
  val trimmed = value.trim()
  if (trimmed.isEmpty()) return "Check that again."
  return when (field) {
    "credit_card" -> if (isValidCard(trimmed)) null else "That's not a real card number. Nice try."
    "card_expiry" -> if (Regex("^(0[1-9]|1[0-2])/[0-9]{2}$").matches(trimmed)) null else "Check that again."
    "dob" -> if (Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(trimmed)) null else "Check that again."
    else -> null
  }
}

private fun isValidCard(number: String): Boolean {
  val digits = number.filter(Char::isDigit)
  if (digits.length !in 12..19) return false
  var sum = 0
  var alt = false
  for (i in digits.length - 1 downTo 0) {
    var n = digits[i].digitToInt()
    if (alt) {
      n *= 2
      if (n > 9) n -= 9
    }
    sum += n
    alt = !alt
  }
  return sum % 10 == 0
}
