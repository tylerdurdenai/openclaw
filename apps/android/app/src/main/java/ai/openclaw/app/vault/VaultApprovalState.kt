package ai.openclaw.app.vault

import android.content.Context
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
    val timeoutRunnable = Runnable {
      if (_pendingRequest.value?.deferred === deferred) {
        _pendingRequest.value = null
        deferred.complete(VaultApprovalResult.TIMED_OUT)
        Log.d("VaultApproval", "Approval timed out")
      }
    }
    handler.postDelayed(timeoutRunnable, 60_000)

    deferred.invokeOnCompletion { handler.removeCallbacks(timeoutRunnable) }

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

  data class AuthResult(
    val success: Boolean,
    val errorCode: Int,
    val errorMessage: String,
  )

  suspend fun authenticate(
    reason: String,
    biometricOnly: Boolean = false,
  ): AuthResult = suspendCancellableCoroutine { cont ->
    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        Log.d(TAG, "Biometric auth succeeded")
        if (cont.isActive) cont.resume(AuthResult(success = true, errorCode = 0, errorMessage = ""))
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        Log.w(TAG, "Biometric auth error: $errorCode — $errString")
        if (cont.isActive) {
          cont.resume(
            AuthResult(
              success = false,
              errorCode = errorCode,
              errorMessage = errString.toString(),
            ),
          )
        }
      }

      override fun onAuthenticationFailed() {
        Log.w(TAG, "Biometric auth failed")
        // Don't resume — let user retry
      }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)

    val promptInfo =
      BiometricPrompt.PromptInfo.Builder()
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

    activity.runOnUiThread {
      if (!cont.isActive || activity.isFinishing || activity.isDestroyed) {
        if (cont.isActive) cont.resume(AuthResult(success = false, errorCode = -2, errorMessage = "activity not running"))
        return@runOnUiThread
      }
      try {
        biometricPrompt.authenticate(promptInfo)
      } catch (e: Throwable) {
        Log.e(TAG, "BiometricPrompt auth failed to start: ${e.message}", e)
        if (cont.isActive) {
          cont.resume(AuthResult(success = false, errorCode = -1, errorMessage = e.message ?: "unknown error"))
        }
      }
    }
  }

  fun canAuthenticate(biometricOnly: Boolean = false): Int {
    val authenticators =
      if (biometricOnly) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
      } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
          BiometricManager.Authenticators.DEVICE_CREDENTIAL
      }
    val biometricManager = BiometricManager.from(activity)
    return biometricManager.canAuthenticate(authenticators)
  }
}
