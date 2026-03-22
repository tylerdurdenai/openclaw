package ai.openclaw.app.vault

import android.util.Base64
import android.util.Log

class VaultSyncManager(
  private val sender: suspend (method: String, paramsJson: String?) -> String,
) {
  suspend fun syncToServer(blob: ByteArray): Boolean {
    return try {
      val encoded = Base64.encodeToString(blob, Base64.NO_WRAP)
      Log.d("VaultSync", "blob bytes: ${blob.size}, encoded length: ${encoded.length}")
      val params = "{\"vault_blob\":\"$encoded\"}"
      val result = sender("vault.sync", params)
      Log.d("VaultSync", "vault.sync result: $result")
      result.contains("ok")
    } catch (e: Throwable) {
      Log.e("VaultSync", "vault.sync failed: ${e.message}", e)
      false
    }
  }
}
