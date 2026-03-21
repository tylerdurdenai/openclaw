package ai.openclaw.app.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class EncryptedBlob(
  val iv: ByteArray,
  val ciphertext: ByteArray,
)

class VaultKeystore {
  companion object {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "tyler_vault_key"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
  }

  private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

  fun encrypt(plainBytes: ByteArray): EncryptedBlob {
    val cipher = Cipher.getInstance(AES_MODE)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val out = cipher.doFinal(plainBytes)
    return EncryptedBlob(iv = cipher.iv, ciphertext = out)
  }

  fun decrypt(blob: EncryptedBlob): ByteArray {
    val cipher = Cipher.getInstance(AES_MODE)
    cipher.init(
      Cipher.DECRYPT_MODE,
      getOrCreateKey(),
      GCMParameterSpec(GCM_TAG_LENGTH_BITS, blob.iv),
    )
    return cipher.doFinal(blob.ciphertext)
  }

  private fun getOrCreateKey(): SecretKey {
    val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val spec =
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
      ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setUserAuthenticationRequired(true)
        .setInvalidatedByBiometricEnrollment(true)
        .build()
    keyGenerator.init(spec)
    return keyGenerator.generateKey()
  }
}
