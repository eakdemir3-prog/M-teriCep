package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.cloud.AppointmentStatus
import com.example.cloud.CloudGate
import com.example.cloud.CloudRepository
import com.example.cloud.UserRole
import com.example.util.Formatters
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

/**
 * Randevu bildirimleri (sunucu gerektirmeden):
 *  - İşletme: yeni randevu isteği gelince
 *  - Müşteri: randevusu onaylanınca / reddedilince / işletme iptal edince
 * Arka planda yaklaşık her 30 dakikada bir kontrol edilir.
 */
class AppointmentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    val ctx = applicationContext
    if (!CloudGate.isConfigured(ctx)) return Result.success()
    val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
    if (!DueReminderWorker.canNotify(ctx)) return Result.success()

    val prefs = ctx.getSharedPreferences("appointment_watch", Context.MODE_PRIVATE)
    val lastCheck = prefs.getLong("last_check_${user.uid}", System.currentTimeMillis() - 30 * 60_000L)
    val now = System.currentTimeMillis()
    val repo = CloudRepository()

    try {
      val profile = repo.getUser(user.uid) ?: return Result.success()
      when (profile.role) {
        UserRole.BUSINESS -> {
          val fresh = repo.businessPendingOnce(user.uid).filter { it.createdAt > lastCheck }
          if (fresh.isNotEmpty()) {
            val lines = fresh.sortedBy { it.start }.map {
              "${it.customerName} • ${it.serviceName} • ${Formatters.formatDateTime(it.start)}"
            }
            notify(ctx, 5201, "${fresh.size} yeni randevu isteği", lines)
          }
        }
        UserRole.CUSTOMER -> {
          val changed = repo.customerAppointmentsOnce(user.uid).filter {
            it.updatedAt > lastCheck && it.cancelledBy != user.uid &&
              it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED) &&
              it.updatedAt > it.createdAt
          }
          changed.forEach {
            val title = when (it.status) {
              AppointmentStatus.CONFIRMED -> "Randevunuz onaylandı"
              AppointmentStatus.REJECTED -> "Randevu isteğiniz reddedildi"
              else -> "Randevunuz iptal edildi"
            }
            notify(ctx, 5300 + (it.id.hashCode() and 0xFF), title, listOf("${it.businessName} • ${it.serviceName} • ${Formatters.formatDateTime(it.start)}"))
            if (it.status == AppointmentStatus.CONFIRMED) scheduleReminder(ctx, it.start, it.businessName, it.serviceName)
          }
        }
        null -> Unit
      }
      prefs.edit().putLong("last_check_${user.uid}", now).apply()
    } catch (e: Exception) {
      return Result.retry()
    }
    return Result.success()
  }

  companion object {
    private const val CHANNEL_ID = "randevular"
    private const val WORK_NAME = "mustericep_randevu_kontrol"

    fun schedule(context: Context) {
      val request = PeriodicWorkRequestBuilder<AppointmentWorker>(30, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
      WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Randevudan 2 saat önce (yakınsa 15 dk önce) yerel hatırlatma. */
    fun scheduleReminder(context: Context, start: Long, businessName: String, serviceName: String) {
      val now = System.currentTimeMillis()
      val at = if (start - 2 * 3_600_000L > now) start - 2 * 3_600_000L else start - 15 * 60_000L
      if (at <= now) return
      val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
        .setInitialDelay(at - now, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf("start" to start, "business" to businessName, "service" to serviceName))
        .build()
      WorkManager.getInstance(context).enqueueUniqueWork("hatirlatma_$start", ExistingWorkPolicy.REPLACE, request)
    }

    fun notify(context: Context, id: Int, title: String, lines: List<String>) {
      if (!DueReminderWorker.canNotify(context)) return
      if (Build.VERSION.SDK_INT >= 26) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
          nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Randevular", NotificationManager.IMPORTANCE_HIGH))
        }
      }
      val pending = PendingIntent.getActivity(
        context, id,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val style = NotificationCompat.InboxStyle().also { s -> lines.forEach { s.addLine(it) } }
      val n = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setContentTitle(title)
        .setContentText(lines.firstOrNull() ?: "")
        .setStyle(style)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
      try { NotificationManagerCompat.from(context).notify(id, n) } catch (e: SecurityException) { }
    }
  }
}

class AppointmentReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    val start = inputData.getLong("start", 0L)
    if (start < System.currentTimeMillis()) return Result.success()
    AppointmentWorker.notify(
      applicationContext, 5400 + (start / 60_000L % 100).toInt(),
      "Randevu hatırlatması",
      listOf("${inputData.getString("business") ?: ""} • ${inputData.getString("service") ?: ""} • ${Formatters.formatDateTime(start)}")
    )
    return Result.success()
  }
}
