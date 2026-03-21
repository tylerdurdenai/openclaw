package ai.openclaw.app.vault

import ai.openclaw.app.gateway.GatewaySession
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
  private val approval: suspend (VaultApprovalContext) -> Boolean = { false },
  private val biometricAuth: suspend (Int) -> Boolean = { tier -> tier < 2 },
) {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun handle(command: String, paramsJson: String?): GatewaySession.InvokeResult {
    return when (command) {
      "vault.decrypt" -> handleDecrypt(paramsJson)
      "vault.sync" -> GatewaySession.InvokeResult.ok("{\"ok\":true}")
      else -> GatewaySession.InvokeResult.error("INVALID_REQUEST", "INVALID_REQUEST: unknown vault command")
    }
  }

  private suspend fun handleDecrypt(paramsJson: String?): GatewaySession.InvokeResult {
    val request =
      try {
        if (paramsJson.isNullOrBlank()) null else json.decodeFromString<VaultDecryptRequest>(paramsJson)
      } catch (_: Throwable) {
        null
      } ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "INVALID_REQUEST: malformed vault request")

    val field = request.field.trim()
    if (!isAllowedField(field)) {
      return GatewaySession.InvokeResult.error("INVALID_REQUEST", "INVALID_REQUEST: unsupported field")
    }

    if (!approval(VaultApprovalContext(
      field = field,
      domain = request.context?.domain,
      amount = request.context?.amount,
      purpose = request.context?.purpose,
    ))) {
      return GatewaySession.InvokeResult.error("DENIED", "Denied. Good instincts.")
    }

    val tier = fieldTier(field)
    if (tier >= 2 && !biometricAuth(tier)) {
      return GatewaySession.InvokeResult.error("NOT_AUTHORIZED", "Biometric required")
    }

    val blob = decodeBlobB64(request.vaultBlob)
      ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "INVALID_REQUEST: invalid vault_blob")
    val keystore = VaultKeystore()
    val decrypted =
      try {
        keystore.decrypt(blob)
      } catch (err: Throwable) {
        return GatewaySession.InvokeResult.error("UNAVAILABLE", "VAULT_DECRYPT_FAILED: ${err.message ?: "decrypt failed"}")
      }

    try {
      val vault = json.decodeFromString<VaultData>(decrypted.decodeToString())
      val value = vault.fields[field]?.value
        ?: return GatewaySession.InvokeResult.error("NOT_FOUND", "NOT_FOUND: field missing")

      val encryptedValue = rsaEncryptOaep(value.toByteArray(), request.browserPubkey)
      val response =
        VaultDecryptResponse(
          encryptedValue = Base64.getEncoder().encodeToString(encryptedValue),
          field = field,
          masked = VaultStoreMasking.mask(field, value),
        )
      return GatewaySession.InvokeResult.ok(json.encodeToString(response))
    } catch (err: Throwable) {
      return GatewaySession.InvokeResult.error("UNAVAILABLE", "VAULT_PARSE_FAILED: ${err.message ?: "parse failed"}")
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
