package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppHelper {

  fun buildMessage(
    templateText: String,
    customerName: String,
    businessName: String,
    totalDebt: Double,
    totalPaid: Double,
    remainingBalance: Double,
    dueDate: Long?,
    lastPaymentAmount: Double? = null
  ): String {
    var result = templateText
      .replace("{musteri_adi}", customerName)
      .replace("{isletme_adi}", businessName)
      .replace("{toplam_borc}", Formatters.formatCurrency(totalDebt))
      .replace("{toplam_tahsilat}", Formatters.formatCurrency(totalPaid))
      .replace("{kalan_bakiye}", Formatters.formatCurrency(remainingBalance))
      .replace("{vade_tarihi}", Formatters.formatDate(dueDate))
      .replace("{son_tutar}", Formatters.formatCurrency(lastPaymentAmount ?: 0.0))
      .replace("{tarih}", Formatters.formatDate(System.currentTimeMillis()))
    return result
  }

  fun openWhatsAppChat(
    context: Context,
    rawPhone: String,
    message: String,
    onError: (String) -> Unit
  ) {
    val cleanPhone = Formatters.cleanPhoneNumber(rawPhone)
    if (cleanPhone.length < 10) {
      onError("Geçersiz telefon numarası. Lütfen 10 haneli geçerli bir telefon numarası girin.")
      return
    }

    try {
      val encodedMsg = URLEncoder.encode(message, "UTF-8")
      val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
      val intent = Intent(Intent.ACTION_VIEW, uri)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

      context.startActivity(intent)
    } catch (e: Exception) {
      // WhatsApp might not be installed or intent could not resolve
      onError("WhatsApp uygulaması açılamadı. Mesaj panoya kopyalanabilir.")
    }
  }

  fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
  }
}
