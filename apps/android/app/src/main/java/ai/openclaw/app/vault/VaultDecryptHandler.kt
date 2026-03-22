package ai.openclaw.app.vault

import ai.openclaw.app.gateway.GatewaySession
import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class VaultApprovalContext(
  val field: String,
  val domain: String?,
  val amount: String?,
  val purpose: String?,
)

class VaultDecryptHandler(
  private val context: Context,
  private val vaultStore: VaultStore,
  private val biometricAuth: suspend (reason: String, biometricOnly: Boolean) -> Boolean,
) {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun handle(command: String, paramsJson: String?): GatewaySession.InvokeResult {
    return when (command) {
      "vault.decrypt" -> handleDecrypt(paramsJson)
      "vault.sync" -> handleSync(paramsJson)
      else -> GatewaySession.InvokeResult.error("INVALID_REQUEST", "unknown vault command")
    }
  }

  private fun handleSync(paramsJson: String?): GatewaySession.InvokeResult {
    Log.d("VaultDebug", "handleSync called with: ${paramsJson?.take(200)}")
    val request =
      try {
        if (paramsJson.isNullOrBlank()) null else json.decodeFromString<VaultSyncRequest>(paramsJson)
      } catch (_: Throwable) {
        null
      } ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "malformed vault sync request")

    Log.d("VaultDebug", "vaultBlob length: ${request?.vaultBlob?.length}")

    val vaultData = try {
      vaultStore.decodeFromBlob(request.vaultBlob)
    } catch (err: Throwable) {
      Log.e("VaultDebug", "decodeFromBlob failed: ${err.message}", err)
      return GatewaySession.InvokeResult.error("UNAVAILABLE", "VAULT_DECRYPT_FAILED: ${err.message ?: "decrypt failed"}")
    }

    Log.d("VaultDebug", "decoded vaultData fields: ${vaultData.fields.keys}")

    try {
      vaultStore.saveLocal(vaultData)
      Log.d("VaultDebug", "saveLocal succeeded")
      return GatewaySession.InvokeResult.ok("{\"ok\":true}")
    } catch (err: Throwable) {
      Log.e("VaultDebug", "saveLocal failed: ${err.message}", err)
      return GatewaySession.InvokeResult.error("UNAVAILABLE", "VAULT_STORE_FAILED: ${err.message ?: "store failed"}")
    }
  }

  private suspend fun handleDecrypt(paramsJson: String?): GatewaySession.InvokeResult {
    val request =
      try {
        if (paramsJson.isNullOrBlank()) null else json.decodeFromString<VaultDecryptRequest>(paramsJson)
      } catch (_: Throwable) {
        null
      } ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "malformed vault request")

    val field = request.field.trim()
    if (!isAllowedField(field)) {
      return GatewaySession.InvokeResult.error("INVALID_REQUEST", "unsupported field")
    }

    val tier = fieldTier(field)

    // Tier 2+: biometric required before showing approval dialog
    if (tier >= 2) {
      val authReason = buildString {
        append("Authenticate to share $field")
        request.context?.domain?.let { append(" with $it") }
        request.context?.amount?.let { append(" ($$it)") }
      }
      val authenticated = biometricAuth.invoke(authReason, false)
      if (!authenticated) {
        Log.d("VaultApproval", "Biometric auth failed for tier $tier field: $field")
        return GatewaySession.InvokeResult.error("NOT_AUTHORIZED", "Authentication required")
      }
    }

    // Show approval dialog and wait for user response
    val domain = request.context?.domain ?: "unknown site"
    val amount = request.context?.amount?.let { "$$it" } ?: ""
    val purpose = request.context?.purpose ?: " undisclosed"

    val approvalResult = VaultApprovalState.emit(
      context = context,
      field = field,
      domain = domain,
      amount = amount,
      purpose = purpose,
    )

    when (approvalResult) {
      VaultApprovalResult.DENIED,
      VaultApprovalResult.TIMED_OUT -> {
        return GatewaySession.InvokeResult.error("DENIED", "Request denied")
      }
      VaultApprovalResult.APPROVED -> { /* continue */ }
    }

    // Decrypt the vault blob and extract the field value
    val blob = decodeBlobB64(request.vaultBlob)
      ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "invalid vault_blob")

    val keystore = VaultKeystore()
    val decrypted =
      try {
        keystore.decrypt(blob)
      } catch (err: Throwable) {
        return GatewaySession.InvokeResult.error("UNAVAILABLE", "VAULT_DECRYPT_FAILED: ${err.message ?: "decrypt failed"}")
      }

    return try {
      val vault = json.decodeFromString<VaultData>(decrypted.decodeToString())
      val value = vault.fields[field]?.value
        ?: return GatewaySession.InvokeResult.error("NOT_FOUND", "field missing from vault")

      val encryptedValue = rsaEncryptOaep(value.toByteArray(), request.browserPubkey)
      val response =
        VaultDecryptResponse(
          encryptedValue = Base64.getEncoder().encodeToString(encryptedValue),
          field = field,
          masked = VaultStoreMasking.mask(field, value),
        )
      GatewaySession.InvokeResult.ok(json.encodeToString(response))
    } catch (err: Throwable) {
      GatewaySession.InvokeResult.error("UNAVAILABLE", "VAULT_PARSE_FAILED: ${err.message ?: "parse failed"}")
    } finally {
      decrypted.fill(0)
    }
  }

  private fun rsaEncryptOaep(plain: ByteArray, browserPubkey: String): ByteArray {
    val pubKey: PublicKey =
      KeyFactory.getInstance("RSA")
        .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(browserPubkey.trim())))
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, pubKey)
    val out = cipher.doFinal(plain)
    plain.fill(0)
    return out
  }

  private fun decodeBlobB64(value: String): EncryptedBlob? {
    return try {
      val bytes = Base64.getDecoder().decode(value.trim())
      val version = java.nio.ByteBuffer.wrap(bytes, 0, 4).int
      if (version != 1) return null
      val ivSize = java.nio.ByteBuffer.wrap(bytes, 4, 4).int
      val iv = bytes.copyOfRange(8, 8 + ivSize)
      val ct = bytes.copyOfRange(8 + ivSize, bytes.size)
      EncryptedBlob(iv = iv, ciphertext = ct)
    } catch (_: Throwable) {
      null
    }
  }

  private fun isAllowedField(field: String): Boolean =
    field in VaultStore.tier1Fields || field in VaultStore.tier2Fields || field in VaultStore.tier3Fields

  private fun fieldTier(field: String): Int {
    return when (field) {
      in VaultStore.tier3Fields -> 3
      in VaultStore.tier2Fields -> 2
      else -> 1
    }
  }
}

@Serializable
private data class VaultDecryptRequest(
  @kotlinx.serialization.SerialName("vault_blob") val vaultBlob: String,
  val field: String,
  @kotlinx.serialization.SerialName("browser_pubkey") val browserPubkey: String,
  val context: VaultDecryptContext? = null,
)

@Serializable
private data class VaultDecryptContext(
  val domain: String? = null,
  val amount: String? = null,
  val purpose: String? = null,
)

@Serializable
private data class VaultDecryptResponse(
  @kotlinx.serialization.SerialName("encrypted_value") val encryptedValue: String,
  val field: String,
  val masked: String,
)

@Serializable
private data class VaultSyncRequest(
  @kotlinx.serialization.SerialName("vault_blob") val vaultBlob: String,
)

private object VaultStoreMasking {
  fun mask(field: String, value: String): String {
    return when (field) {
      "credit_card" -> {
        val digits = value.filter(Char::isDigit)
        "****${digits.takeLast(4)}"
      }
      "cvv" -> "***"
      "passport" -> if (value.length <= 4) "****" else "${value.first()}****${value.takeLast(3)}"
      "membership_number" -> "***${value.takeLast(3)}"
      "email" -> value.firstOrNull()?.plus("***") ?: "***"
      "phone" -> "****${value.takeLast(4)}"
      else -> "••••"
    }
  }
}
