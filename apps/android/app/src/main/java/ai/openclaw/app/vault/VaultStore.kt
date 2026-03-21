package ai.openclaw.app.vault

import android.content.Context
import java.io.File
import java.nio.ByteBuffer
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class VaultField(
  val key: String,
  val value: String,
  val tier: Int,
  val updatedAt: String,
)

@Serializable
data class VaultData(
  val version: Int = 1,
  val fields: Map<String, VaultField>,
  val updatedAt: String,
)

class VaultStore(
  context: Context,
  private val keystore: VaultKeystore = VaultKeystore(),
) {
  companion object {
    val tier1Fields = setOf("first_name", "last_name", "dob", "phone", "email", "address")
    val tier2Fields = setOf("credit_card", "card_expiry", "passport", "membership_number")
    val tier3Fields = setOf("cvv")

    private const val VERSION = 1
  }

  private val json = Json { ignoreUnknownKeys = true }
  private val storageFile: File = File(context.filesDir, "vault_local_blob.bin")

  fun loadLocal(): VaultData {
    if (!storageFile.exists()) {
      return VaultData(fields = emptyMap(), updatedAt = Instant.now().toString())
    }
    val blobBytes = storageFile.readBytes()
    if (blobBytes.isEmpty()) {
      return VaultData(fields = emptyMap(), updatedAt = Instant.now().toString())
    }

    val encrypted = decodeBlob(blobBytes)
    val plain = keystore.decrypt(encrypted)
    return try {
      json.decodeFromString<VaultData>(plain.decodeToString())
    } finally {
      plain.fill(0)
    }
  }

  fun saveLocal(data: VaultData) {
    val encoded = json.encodeToString(data).encodeToByteArray()
    try {
      val blob = keystore.encrypt(encoded)
      storageFile.writeBytes(encodeBlob(blob))
    } finally {
      encoded.fill(0)
    }
  }

  fun exportBlob(): ByteArray {
    val data = loadLocal()
    val plain = json.encodeToString(data).encodeToByteArray()
    return try {
      encodeBlob(keystore.encrypt(plain))
    } finally {
      plain.fill(0)
    }
  }

  fun maskValue(field: String, value: String): String {
    if (value.isBlank()) return ""
    return when (field) {
      "credit_card" -> {
        val digits = value.filter(Char::isDigit)
        val last4 = digits.takeLast(4)
        if (last4.isEmpty()) "****" else "****$last4"
      }
      "cvv" -> "***"
      "passport" -> {
        if (value.length <= 2) "**"
        else "${value.first()}****${value.takeLast(3)}"
      }
      "phone" -> {
        val trimmed = value.trim()
        if (trimmed.length <= 4) "****" else "****${trimmed.takeLast(4)}"
      }
      "email" -> {
        val at = value.indexOf('@')
        if (at <= 1) "***"
        else "${value.first()}***${value.substring(at)}"
      }
      "membership_number" -> {
        if (value.length <= 3) "***" else "***${value.takeLast(3)}"
      }
      else -> "••••"
    }
  }

  fun tierFor(field: String): Int {
    return when (field) {
      in tier1Fields -> 1
      in tier2Fields -> 2
      in tier3Fields -> 3
      else -> 1
    }
  }

  private fun encodeBlob(blob: EncryptedBlob): ByteArray {
    val ivSize = blob.iv.size
    val out = ByteBuffer.allocate(4 + 4 + ivSize + blob.ciphertext.size)
    out.putInt(VERSION)
    out.putInt(ivSize)
    out.put(blob.iv)
    out.put(blob.ciphertext)
    return out.array()
  }

  private fun decodeBlob(bytes: ByteArray): EncryptedBlob {
    val buf = ByteBuffer.wrap(bytes)
    val version = buf.int
    require(version == VERSION) { "Unsupported vault blob version: $version" }
    val ivSize = buf.int
    require(ivSize in 12..32) { "Invalid IV size" }

    val iv = ByteArray(ivSize)
    buf.get(iv)
    val ciphertext = ByteArray(buf.remaining())
    buf.get(ciphertext)
    return EncryptedBlob(iv = iv, ciphertext = ciphertext)
  }
}
