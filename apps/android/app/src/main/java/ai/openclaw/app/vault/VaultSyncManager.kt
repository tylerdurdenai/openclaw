package ai.openclaw.app.vault

import android.util.Base64

class VaultSyncManager(
  private val sender: suspend (method: String, paramsJson: String) -> String,
) {
  suspend fun syncToServer(blob: ByteArray): Boolean {
    return try {
      val encoded = Base64.encodeToString(blob, Base64.NO_WRAP)
      val params = "{\"vault_blob\":\"$encoded\"}"
      sender("vault.sync", params)
      true
    } catch (_: Throwable) {
      false
    }
  }
}
