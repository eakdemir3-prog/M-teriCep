package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.model.CustomerWithBalance
import java.net.URLEncoder

object WhatsAppHelper {

  private const val WHATSAPP = "com.whatsapp"
  private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

  /** Şablondaki {değişken}leri müşterinin güncel kayıtlarıyla doldurur. */
  fun buildMessage(
    templateText: String,
    customerName: String,
    businessName: String,
    totalDebt: Double,
    totalPaid: Double,
    remainingBalance: Double,
    dueDate: Long?,
    lastPaymentAmount: Double? = null,
    overdueAmount: Double = 0.0
  ): String {
    return templateText
      .replace("{musteri_adi}", customerName)
      .replace("{isletme_adi}", businessName)
      .replace("{toplam_borc}", Formatters.formatCurrency(totalDebt))
      .replace("{toplam_tahsilat}", Formatters.formatCurrency(totalPaid))
      .replace("{kalan_bakiye}", Formatters.formatCurrency(maxOf(0.0, remainingBalance)))
      .replace("{gecikmis_tutar}", Formatters.formatCurrency(overdueAmount))
      .replace("{vade_tarihi}", if (dueDate != null) Formatters.formatDate(dueDate) else "belirtilmemiş")
      .replace("{son_tutar}", Formatters.formatCurrency(lastPaymentAmount ?: 0.0))
      .replace("{tarih}", Formatters.formatDate(System.currentTimeMillis()))
  }

  fun buildMessage(templateText: String, item: CustomerWithBalance, businessName: String): String =
    buildMessage(
      templateText = templateText,
      customerName = item.customer.name,
      businessName = businessName,
      totalDebt = item.totalDebt,
      totalPaid = item.totalPaid,
      remainingBalance = item.remainingBalance,
      dueDate = item.nextDueDate,
      lastPaymentAmount = item.lastPaymentAmount,
      overdueAmount = item.overdueAmount
    )

  private fun isInstalled(context: Context, pkg: String): Boolean = try {
    if (Build.VERSION.SDK_INT >= 33) {
      context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      context.packageManager.getPackageInfo(pkg, 0)
    }
    true
  } catch (e: Exception) {
    false
  }

  /**
   * WhatsApp sohbetini mesaj hazır olarak açar; mesajı kullanıcı kendisi gönderir.
   * WhatsApp Business yüklüyse o tercih edilir (esnaf çoğunlukla onu kullanır).
   */
  fun openWhatsAppChat(
    context: Context,
    rawPhone: String,
    message: String,
    onError: (String) -> Unit
  ) {
    val cleanPhone = Formatters.cleanPhoneNumber(rawPhone)
    if (cleanPhone.length < 10) {
      onError("Müşterinin telefon numarası eksik veya hatalı. Önce müşteri bilgilerinden düzeltin.")
      return
    }
    val pkg = when {
      isInstalled(context, WHATSAPP_BUSINESS) -> WHATSAPP_BUSINESS
      isInstalled(context, WHATSAPP) -> WHATSAPP
      else -> null
    }
    if (pkg == null) {
      onError("Bu telefonda WhatsApp yüklü değil. Mesajı kopyalayıp başka bir uygulamadan gönderebilirsiniz.")
      return
    }
    try {
      val uri = Uri.parse("https://wa.me/$cleanPhone?text=" + URLEncoder.encode(message, "UTF-8").replace("+", "%20"))
      val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage(pkg)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      onError("WhatsApp açılamadı. Mesajı kopyalayıp WhatsApp'a yapıştırabilirsiniz.")
    }
  }

  fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
  }
}
