package ai.openclaw.app.vault

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ai.openclaw.app.MainActivity
import ai.openclaw.app.R

object VaultNotificationHelper {
  private const val TAG = "VaultNotification"
  private const val VAULT_NOTIFICATION_ID = 0xVAULT // 30583

  fun showApprovalNotification(
    context: Context,
    field: String,
    domain: String,
    amount: String,
  ) {
    if (!hasNotificationPermission(context)) {
      Log.w(TAG, "Notification permission not granted")
      return
    }

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_NEW_TASK
      putExtra("open_vault_approval", true)
    }
    val launchPending =
      PendingIntent.getActivity(
        context,
        1,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val denyIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("vault_action", "deny")
    }
    val denyPending =
      PendingIntent.getActivity(
        context,
        2,
        denyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val title = "Vault access request"
    val text = buildString {
      append("$field")
      if (domain.isNotBlank()) append(" for $domain")
      if (amount.isNotBlank()) append(" ($$amount)")
    }

    val notification =
      NotificationCompat.Builder(context, ai.openclaw.app.NodeForegroundService.VAULT_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setAutoCancel(true)
        .setContentIntent(launchPending)
        .addAction(0, "Deny", denyPending)
        .build()

    try {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.notify(VAULT_NOTIFICATION_ID, notification)
      Log.d(TAG, "Showed vault approval notification: $field for $domain")
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to show notification: ${e.message}", e)
    }
  }

  fun dismissNotification(context: Context) {
    try {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.cancel(VAULT_NOTIFICATION_ID)
    } catch (_: Throwable) {}
  }

  private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }
}
