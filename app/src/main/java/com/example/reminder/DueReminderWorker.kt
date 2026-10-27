package com.example.reminder

import com.example.model.allows
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.SettingsStore
import com.example.model.DebtStatus
import com.example.util.Formatters
import com.example.util.Ledger
import com.example.util.Money
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Günde bir kez çalışır: vadesi geçmiş ve vadesi yaklaşan alacakları sayar ve tek bir
 * bildirimle hatırlatır. Kimseye mesaj göndermez; sadece işletme sahibini uyarır.
 */
class DueReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    val ctx = applicationContext
    val profile = SettingsStore(ctx).current()
    if (!profile.notificationsEnabled) return Result.success()
    if (!profile.activeTier.allows(com.example.model.Feature.DUE_REMINDERS)) return Result.success()
    if (!canNotify(ctx)) return Result.success()

    val db = AppDatabase.getDatabase(ctx)
    val customers = db.customerDao().getAllCustomersOnce().filter { !it.isArchived }
    val (_, statuses) = Ledger.buildAll(
      customers,
      db.debtDao().getAllDebtsOnce(),
      db.collectionDao().getAllCollectionsOnce()
    )

    val now = System.currentTimeMillis()
    val limit = Formatters.startOfDay(now) + (profile.reminderDaysBefore + 1) * 86_400_000L
    val overdue = statuses.filter { it.status == DebtStatus.OVERDUE }
    val upcoming = statuses.filter {
      it.status != DebtStatus.PAID && it.status != DebtStatus.OVERDUE &&
        it.debt.dueDate != null && it.debt.dueDate < limit
    }
    if (overdue.isEmpty() && upcoming.isEmpty()) return Result.success()

    val overdueSum = Money.fromCents(overdue.sumOf { Money.toCents(it.remainingAmount) })
    val upcomingSum = Money.fromCents(upcoming.sumOf { Money.toCents(it.remainingAmount) })
    val overdueCustomers = overdue.map { it.debt.customerId }.distinct().size
    val upcomingCustomers = upcoming.map { it.debt.customerId }.distinct().size

    val lines = buildList {
      if (overdue.isNotEmpty()) add("$overdueCustomers müşteride vadesi geçmiş ${Formatters.formatCurrency(overdueSum)}")
      if (upcoming.isNotEmpty()) {
        val whenText = if (profile.reminderDaysBefore == 0) "bugün" else "${profile.reminderDaysBefore} gün içinde"
        add("$upcomingCustomers müşteride $whenText vadesi gelen ${Formatters.formatCurrency(upcomingSum)}")
      }
    }
    show(ctx, title = "Tahsilat hatırlatması", lines = lines)
    return Result.success()
  }

  companion object {
    private const val CHANNEL_ID = "vade_hatirlatma"
    private const val WORK_NAME = "mustericep_vade_hatirlatici"
    private const val NOTIFICATION_ID = 4101

    fun canNotify(context: Context): Boolean {
      if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
      ) return false
      return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Her sabah yaklaşık 09:00'da çalışacak şekilde planlar. */
    fun schedule(context: Context) {
      val now = Calendar.getInstance()
      val next = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
      }
      val delay = next.timeInMillis - now.timeInMillis
      val request = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
      WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= 26) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
          nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Vade hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT).apply {
              description = "Vadesi gelen ve geciken alacaklar için günlük özet"
            }
          )
        }
      }
    }

    fun show(context: Context, title: String, lines: List<String>) {
      if (!canNotify(context)) return
      ensureChannel(context)
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pending = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val style = NotificationCompat.InboxStyle()
      lines.forEach { style.addLine(it) }
      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setContentTitle(title)
        .setContentText(lines.firstOrNull() ?: "")
        .setStyle(style)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
      try {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      } catch (e: SecurityException) {
        // İzin geri alınmış olabilir; sessizce geç.
      }
    }
  }
}
