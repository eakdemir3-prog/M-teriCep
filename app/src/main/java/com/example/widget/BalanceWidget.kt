package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.model.DebtStatus
import com.example.util.Formatters
import com.example.util.Ledger
import com.example.util.Money
import com.example.util.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ana ekran widget'ı: toplam alacak, vadesi geçen tutar ve borçlu müşteri sayısı.
 * Uygulamadan çıkıldığında ve sistemin belirlediği aralıklarla güncellenir; dokununca uygulama açılır.
 * Uygulama kilidi açıksa tutarlar gizlenir.
 */
class BalanceWidget : AppWidgetProvider() {

  override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
    val pending = goAsync()
    scope.launch {
      try { render(context, manager, ids) } finally { pending.finish() }
    }
  }

  companion object {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Tüm widget'ları yeniden çizer (uygulamadan çıkarken çağrılır). */
    fun updateAll(context: Context) {
      val app = context.applicationContext
      val manager = AppWidgetManager.getInstance(app)
      val ids = manager.getAppWidgetIds(ComponentName(app, BalanceWidget::class.java))
      if (ids.isEmpty()) return
      scope.launch { runCatching { render(app, manager, ids) } }
    }

    private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
      val db = AppDatabase.getDatabase(context)
      val customers = db.customerDao().getAllCustomersOnce().filter { !it.isArchived }
      val (balances, statuses) = Ledger.buildAll(customers, db.debtDao().getAllDebtsOnce(), db.collectionDao().getAllCollectionsOnce())
      val total = Money.fromCents(balances.sumOf { maxOf(0L, Money.toCents(it.remainingBalance)) })
      val debtors = balances.count { it.remainingBalance > 0.004 }
      val overdue = Money.fromCents(statuses.filter { it.status == DebtStatus.OVERDUE }.sumOf { Money.toCents(it.remainingAmount) })
      val hidden = com.example.util.AppLock.isEnabled(context)

      val open = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      for (id in ids) {
        val v = RemoteViews(context.packageName, R.layout.widget_balance)
        v.setTextViewText(R.id.w_title, tr("Toplam alacak"))
        v.setTextViewText(R.id.w_total, if (hidden) "••••" else Formatters.formatCurrency(total))
        v.setTextViewText(R.id.w_debtors, tr("{0} borçlu müşteri", debtors))
        v.setTextViewText(R.id.w_overdue_label, tr("Gecikmiş"))
        v.setTextViewText(R.id.w_overdue, if (hidden) "••••" else Formatters.formatCurrency(overdue))
        v.setOnClickPendingIntent(R.id.w_root, open)
        manager.updateAppWidget(id, v)
      }
    }
  }
}
