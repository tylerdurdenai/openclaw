package ai.openclaw.app.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.openclaw.app.ui.mobileBody
import ai.openclaw.app.ui.mobileDisplay

@Composable
fun VaultApprovalDialog(
  field: String,
  domain: String,
  amount: String,
  purpose: String,
  onApprove: () -> Unit,
  onDeny: () -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start,
    ) {
      Text("Incoming request", style = mobileDisplay)
      Text(
        "Someone wants your $field for $domain. $amount. Your call.",
        style = mobileBody,
        modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
      )
      Text("Purpose: $purpose", style = mobileBody, modifier = Modifier.padding(bottom = 24.dp))
      Button(onClick = onApprove, modifier = Modifier.fillMaxWidth()) { Text("Approve") }
      TextButton(onClick = onDeny, modifier = Modifier.fillMaxWidth()) { Text("Deny") }
    }
  }
}
