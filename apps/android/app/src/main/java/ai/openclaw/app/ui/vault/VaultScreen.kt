package ai.openclaw.app.ui.vault

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ai.openclaw.app.vault.VaultApprovalResult
import ai.openclaw.app.vault.VaultApprovalState
import ai.openclaw.app.vault.VaultData
import ai.openclaw.app.vault.VaultField
import ai.openclaw.app.vault.VaultStore
import java.time.Instant

@Composable
fun VaultScreen(
  onSync: suspend () -> Boolean,
) {
  val context = LocalContext.current
  val store = remember(context) { VaultStore(context) }
  var data by remember { mutableStateOf(VaultData(fields = emptyMap(), updatedAt = Instant.now().toString())) }
  var editingField by remember { mutableStateOf<String?>(null) }
  // Track which field is temporarily revealed (cleared on next interaction)
  var revealedField by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  // Observe vault approval requests
  val pendingRequest by VaultApprovalState.pendingRequest.collectAsState()

  LaunchedEffect(Unit) {
    try {
      data = store.loadLocal()
    } catch (e: Throwable) {
      android.util.Log.e("VaultScreen", "loadLocal failed: ${e.message}", e)
      data = VaultData(fields = emptyMap(), updatedAt = Instant.now().toString())
    }
  }

  // Handle approval result — reveal the field if approved
  LaunchedEffect(pendingRequest) {
    if (pendingRequest == null) {
      // A request was resolved — don't auto-reveal, let user tap again
    }
  }

  // When a pending request exists, show the approval dialog
  val request = pendingRequest
  if (request != null) {
    VaultApprovalDialog(
      field = request.field,
      domain = request.domain,
      amount = request.amount,
      purpose = request.purpose,
      onApprove = {
        // Mark field as revealed temporarily
        revealedField = request.field
        VaultApprovalState.resolve(VaultApprovalResult.APPROVED)
      },
      onDeny = {
        VaultApprovalState.resolve(VaultApprovalResult.DENIED)
      },
    )
    return
  }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("The Vault")

    if (data.fields.isEmpty()) {
      Text("Nothing worth protecting yet. That's either zen or reckless.")
      Text("First rule: never leave your data unguarded.")
    }

    val grouped = listOf(
      "Personal" to listOf("first_name", "last_name", "dob", "phone", "email", "address"),
      "Payment" to listOf("credit_card", "card_expiry", "cvv"),
      "Travel" to listOf("passport", "membership_number"),
    )

    grouped.forEach { (section, keys) ->
      Text(section)
      keys.forEach { key ->
        val field = data.fields[key]
        val tier = store.tierFor(key)
        val isRevealed = revealedField == key
        val displayValue = if (isRevealed) {
          field?.value ?: ""
        } else {
          store.maskValue(key, field?.value.orEmpty())
        }

        VaultFieldRow(
          label = key,
          maskedValue = displayValue,
          tier = tier,
          onReveal = {
            val rawValue = field?.value.orEmpty()
            if (tier >= 2) {
              // Tier 2+: require approval before revealing
              scope.launch {
                val result = VaultApprovalState.emit(
                  context = context,
                  field = key,
                  domain = "this app",
                  amount = "",
                  purpose = "view secret",
                )
                if (result != VaultApprovalResult.APPROVED) {
                  Toast.makeText(context, "Access denied.", Toast.LENGTH_SHORT).show()
                }
              }
            } else {
              // Tier 1: reveal directly
              revealedField = key
            }
          },
          onEdit = { editingField = key },
        )
      }
    }

    Button(
      onClick = {
        scope.launch {
          revealedField = null  // Clear reveals on sync
          val ok = onSync()
          Toast.makeText(
            context,
            if (ok) "Sealed. Not even I can read this." else "Sync failed. Your secrets are still local. Try again.",
            Toast.LENGTH_LONG,
          ).show()
        }
      },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Lock it down")
    }
  }

  val field = editingField
  if (field != null) {
    VaultEditDialog(
      field = field,
      initialValue = data.fields[field]?.value.orEmpty(),
      onDismiss = { editingField = null },
      onSave = { newValue ->
        val now = Instant.now().toString()
        val updatedField = VaultField(key = field, value = newValue, tier = store.tierFor(field), updatedAt = now)
        val next = data.copy(fields = data.fields + (field to updatedField), updatedAt = now)
        data = next
        runCatching { store.saveLocal(next) }
        Toast.makeText(context, "Locked away. Smart move.", Toast.LENGTH_SHORT).show()
        editingField = null
      },
    )
  }
}
