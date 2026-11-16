package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.model.CustomerWithBalance
import com.example.model.TransactionItem
import java.io.File

/**
 * Müşteri hesap ekstresi (PDF): işletme logosu ve adı, müşteri bilgisi, özet ve
 * tarih sırasıyla tüm borç/tahsilat hareketleri, her satırda kalan bakiye.
 * A4, gerekirse birden çok sayfa.
 */
object StatementPdf {
  private const val W = 595
  private const val H = 842
  private const val M = 40f
  private val INK = 0xFF0E0F12.toInt()
  private val MUTED = 0xFF6B7280.toInt()
  private val LINE = 0xFFE6E8E3.toInt()
  private val LIME = 0xFFC6F432.toInt()
  private val SOFT = 0xFFF6F7F4.toInt()
  private val RED = 0xFFDC2626.toInt()
  private val GREEN = 0xFF4D7C0F.toInt()

  fun create(
    context: Context,
    businessName: String,
    businessPhone: String,
    logoBase64: String,
    item: CustomerWithBalance,
    transactions: List<TransactionItem>,
    now: Long = System.currentTimeMillis()
  ): File {
    val doc = PdfDocument()
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; textSize = 10f }
    val bold = Paint(text).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    val rows = transactions.sortedWith(
      compareBy<TransactionItem> { it.date }.thenBy { if (it is TransactionItem.DebtItem) 0 else 1 }.thenBy { it.id }
    )
    var running = 0L
    val lines = rows.map { t ->
      val isDebt = t is TransactionItem.DebtItem
      running += if (isDebt) Money.toCents(t.amount) else -Money.toCents(t.amount)
      val desc = when (t) {
        is TransactionItem.DebtItem -> listOf(tr(t.category), t.notesOrDesc).filter { it.isNotBlank() }.joinToString(" • ")
          .ifBlank { tr("Borç") }
        is TransactionItem.CollectionItem -> listOf(tr("Tahsilat") + " (" + tr(t.paymentMethod.shortLabel) + ")", t.notesOrDesc)
          .filter { it.isNotBlank() }.joinToString(" • ")
      }
      Row(Formatters.formatDate(t.date), desc, if (isDebt) t.amount else null, if (!isDebt) t.amount else null, Money.fromCents(running))
    }

    val logo: Bitmap? = logoBase64.takeIf { it.isNotBlank() }?.let {
      runCatching { Base64.decode(it, Base64.DEFAULT).let { b -> BitmapFactory.decodeByteArray(b, 0, b.size) } }.getOrNull()
    }

    val colX = floatArrayOf(M, M + 70f, W - M - 225f, W - M - 150f, W - M - 75f)
    val rowH = 20f
    var pageNo = 0
    var page: PdfDocument.Page? = null
    var y = 0f
    val pages = mutableListOf<PdfDocument.Page>()

    fun footer(c: Canvas, n: Int) {
      text.color = MUTED; text.textSize = 8f
      c.drawText(tr("MüşteriCep ile oluşturuldu • {0}", Formatters.formatDateTime(now)), M, H - 24f, text)
      val p = tr("Sayfa {0}", n)
      c.drawText(p, W - M - text.measureText(p), H - 24f, text)
      text.color = INK; text.textSize = 10f
    }

    fun tableHeader(c: Canvas) {
      fill.color = INK
      c.drawRoundRect(RectF(M, y, W - M, y + 22f), 6f, 6f, fill)
      bold.color = 0xFFFFFFFF.toInt(); bold.textSize = 9f
      c.drawText(tr("Tarih"), colX[0] + 6f, y + 15f, bold)
      c.drawText(tr("Açıklama"), colX[1], y + 15f, bold)
      rightText(c, tr("Borç"), colX[3] - 8f, y + 15f, bold)
      rightText(c, tr("Tahsilat"), colX[4] - 8f, y + 15f, bold)
      rightText(c, tr("Bakiye"), W - M - 6f, y + 15f, bold)
      bold.color = INK; bold.textSize = 10f
      y += 26f
    }

    fun newPage(): Canvas {
      page?.let { footer(it.canvas, pageNo); doc.finishPage(it) }
      pageNo++
      val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
      page = p
      y = M
      return p.canvas
    }

    var c = newPage()

    // Başlık: logo + işletme
    var x = M
    if (logo != null) {
      val r = RectF(M, y, M + 52f, y + 52f)
      c.save()
      val path = android.graphics.Path().apply { addRoundRect(r, 10f, 10f, android.graphics.Path.Direction.CW) }
      c.clipPath(path)
      c.drawBitmap(logo, null, r, Paint(Paint.FILTER_BITMAP_FLAG))
      c.restore()
      x = M + 64f
    } else {
      fill.color = LIME
      c.drawRoundRect(RectF(M, y + 6f, M + 6f, y + 46f), 3f, 3f, fill)
      x = M + 16f
    }
    bold.textSize = 18f
    c.drawText(ellipsize(businessName, bold, W - M - x - 150f), x, y + 24f, bold)
    text.color = MUTED
    if (businessPhone.isNotBlank()) c.drawText(Formatters.formatPhoneForDisplay(businessPhone), x, y + 42f, text)
    text.color = INK
    bold.textSize = 14f
    rightText(c, tr("Hesap Ekstresi"), W - M, y + 24f, bold)
    text.color = MUTED
    rightText(c, Formatters.formatDate(now), W - M, y + 42f, text)
    text.color = INK
    bold.textSize = 10f
    y += 70f

    // Müşteri ve özet
    fill.color = SOFT
    c.drawRoundRect(RectF(M, y, W - M, y + 92f), 10f, 10f, fill)
    text.color = MUTED; text.textSize = 9f
    c.drawText(tr("Müşteri"), M + 14f, y + 20f, text)
    text.color = INK; text.textSize = 10f
    bold.textSize = 14f
    c.drawText(ellipsize(item.customer.name, bold, 240f), M + 14f, y + 38f, bold)
    bold.textSize = 10f
    if (item.customer.phone.isNotBlank()) {
      text.color = MUTED
      c.drawText(Formatters.formatPhoneForDisplay(item.customer.phone), M + 14f, y + 54f, text)
      text.color = INK
    }
    val sx = M + 290f
    fun stat(label: String, value: Double, color: Int, yy: Float) {
      text.color = MUTED; text.textSize = 9f
      c.drawText(label, sx, yy, text)
      bold.color = color
      rightText(c, Formatters.formatCurrency(value), W - M - 14f, yy, bold)
      bold.color = INK; text.color = INK; text.textSize = 10f
    }
    stat(tr("Toplam borç"), item.totalDebt, INK, y + 22f)
    stat(tr("Toplam tahsilat"), item.totalPaid, GREEN, y + 40f)
    if (item.overdueAmount > 0) stat(tr("Vadesi geçen"), item.overdueAmount, RED, y + 58f)
    fill.color = LINE
    c.drawRect(sx, y + 66f, W - M - 14f, y + 67f, fill)
    text.textSize = 10f
    bold.textSize = 12f
    c.drawText(tr("Kalan bakiye"), sx, y + 83f, bold)
    bold.color = if (item.remainingBalance > 0) RED else GREEN
    rightText(c, Formatters.formatCurrency(item.remainingBalance), W - M - 14f, y + 83f, bold)
    bold.color = INK; bold.textSize = 10f
    y += 108f

    tableHeader(c)
    if (lines.isEmpty()) {
      text.color = MUTED
      c.drawText(tr("Henüz işlem yok."), M + 6f, y + 14f, text)
      text.color = INK
      y += rowH
    }
    lines.forEachIndexed { i, r ->
      if (y + rowH > H - 50f) { c = newPage(); tableHeader(c) }
      if (i % 2 == 1) { fill.color = SOFT; c.drawRect(M, y - 2f, W - M, y + rowH - 4f, fill) }
      val base = y + 12f
      text.textSize = 9f
      c.drawText(r.date, colX[0] + 6f, base, text)
      c.drawText(ellipsize(r.desc, text, colX[2] - colX[1] + 30f), colX[1], base, text)
      r.debt?.let { text.color = INK; rightText(c, Formatters.formatCurrency(it), colX[3] - 8f, base, text) }
      r.paid?.let { text.color = GREEN; rightText(c, Formatters.formatCurrency(it), colX[4] - 8f, base, text) }
      text.color = INK
      bold.textSize = 9f
      rightText(c, Formatters.formatCurrency(r.balance), W - M - 6f, base, bold)
      bold.textSize = 10f
      text.textSize = 10f
      y += rowH
    }

    // Kapanış
    if (y + 40f > H - 50f) c = newPage()
    fill.color = LINE
    c.drawRect(M, y + 4f, W - M, y + 5f, fill)
    bold.textSize = 11f
    c.drawText(tr("Kalan bakiye"), M + 6f, y + 24f, bold)
    bold.color = if (item.remainingBalance > 0) RED else GREEN
    rightText(c, Formatters.formatCurrency(item.remainingBalance), W - M - 6f, y + 24f, bold)
    bold.color = INK

    page?.let { footer(it.canvas, pageNo); doc.finishPage(it) }

    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 86_400_000L) it.delete() }
    val safe = item.customer.name.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').ifBlank { "musteri" }
    val file = File(dir, "Ekstre_${safe}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
  }

  /** PDF'i paylaşım menüsüyle (WhatsApp, e-posta, Drive...) gönderir. */
  fun share(context: Context, file: File, message: String) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_TEXT, message)
      putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension.replace('_', ' '))
      clipData = ClipData.newRawUri(file.name, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, tr("Ekstreyi gönder")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  private data class Row(val date: String, val desc: String, val debt: Double?, val paid: Double?, val balance: Double)

  private fun rightText(c: Canvas, s: String, right: Float, y: Float, p: Paint) = c.drawText(s, right - p.measureText(s), y, p)

  private fun ellipsize(s: String, p: Paint, max: Float): String {
    if (p.measureText(s) <= max) return s
    var t = s
    while (t.isNotEmpty() && p.measureText("$t…") > max) t = t.dropLast(1)
    return "$t…"
  }
}
