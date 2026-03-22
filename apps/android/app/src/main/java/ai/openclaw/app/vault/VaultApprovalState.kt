package ai.openclaw.app.vault

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VaultApprovalRequest(
  val field: String,
  val domain: String,
  val amount: String,
  val purpose: String,
  val deferred: CompletableDeferred<VaultApprovalResult>,
)

enum class VaultApprovalResult {
  APPROVED,
  DENIED,
  TIMED_OUT,
}

object VaultApprovalState {
  private val _pendingRequest = MutableStateFlow<VaultApprovalRequest?>(null)
  val pendingRequest: StateFlow<VaultApprovalRequest?> = _pendingRequest.asStateFlow()

  private val handler = Handler(Looper.getMainLooper())

  suspend fun emit(
    context: Context,
    field: String,
    domain: String,
    amount: String,
    purpose: String,
  ): VaultApprovalResult {
    if (_pendingRequest.value != null) {
      Log.w("VaultApproval", "Approval already pending, rejecting duplicate")
      return VaultApprovalResult.DENIED
    }

    val deferred = CompletableDeferred<VaultApprovalResult>()

    _pendingRequest.value = VaultApprovalRequest(
      field = field,
      domain = domain,
      amount = amount,
      purpose = purpose,
      deferred = deferred,
    )

    Log.d("VaultApproval", "Pending approval: $field for $domain $amount")

    // Timeout after 60 seconds
    handler.postDelayed({
      if (_pendingRequest.value?.deferred === deferred) {
        _pendingRequest.value = null
        deferred.complete(VaultApprovalResult.TIMED_OUT)
        Log.d("VaultApproval", "Approval timed out")
      }
    }, 60_000)

    return deferred.await()
  }

  fun resolve(result: VaultApprovalResult) {
    _pendingRequest.value?.deferred?.complete(result)
    _pendingRequest.value = null
    Log.d("VaultApproval", "Approval resolved: $result")
  }

  fun isPending(): Boolean = _pendingRequest.value != null

  fun getPendingRequest(): VaultApprovalRequest? = _pendingRequest.value
}

class VaultBiometricAuth(private val activity: FragmentActivity) {
  companion object {
    private const val TAG = "VaultBiometric"
  }

  interface AuthResult {
    val success: Boolean
    val errorCode: Int?
    val errorMessage: String?
  }

  private class BiometricAuthResult(
    override val success: Boolean,
    override val errorCode: Int?,
    override val errorMessage: String?,
  ) : AuthResult

  suspend fun authenticate(
    reason: String,
    biometricOnly: Boolean = false,
  ): AuthResult = suspend { callback ->
    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        Log.d(TAG, "Biometric auth succeeded")
        callback(BiometricAuthResult(success = true, errorCode = null, errorMessage = null))
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        Log.w(TAG, "Biometric auth error: $errorCode — $errString")
        // On user cancel, treat as denial. On hardware unavailable, try device credential.
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
          errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
          errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
          errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT
        ) {
          if (!biometricOnly) {
            // Fall through to device credential
            Log.d(TAG, "Falling back to device credential")
            callback(BiometricAuthResult(success = false, errorCode = errorCode, errorMessage = errString.toString()))
            return
          }
          callback(BiometricAuthResult(success = false, errorCode = errorCode, errorMessage = errString.toString()))
        } else {
          callback(BiometricAuthResult(success = false, errorCode = errorCode, errorMessage = errString.toString()))
        }
      }

      override fun onAuthenticationFailed() {
        Log.w(TAG, "Biometric auth failed")
        // Don't complete — let user retry
      }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Verify it's you")
      .setSubtitle(reason)
      .apply {
        if (biometricOnly) {
          setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        } else {
          setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
              BiometricManager.Authenticators.DEVICE_CREDENTIAL,
          )
        }
      }
      .build()

    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    activity.runOnUiThread {
      try {
        biometricPrompt.authenticate(promptInfo)
      } catch (e: Throwable) {
        Log.e(TAG, "BiometricPrompt auth failed to start: ${e.message}", e)
        callback(BiometricAuthResult(success = false, errorCode = -1, errorMessage = e.message))
      }
    }
  }.await()

  fun canAuthenticate(biometricOnly: Boolean = false): Int {
    val authenticators = if (biometricOnly) {
      BiometricManager.Authenticators.BIOMETRIC_STRONG
    } else {
      BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
    val biometricManager = BiometricManager.from(activity)
    return biometricManager.canAuthenticate(authenticators)
  }
}

private fun <T> suspend(block: (T) -> Unit): kotlin.coroutines.SuspendCoroutine<T> =
  kotlin.coroutines.intrinsics.suspendCoroutine { cont -> block({ cont.resumeWith(it) }) }
