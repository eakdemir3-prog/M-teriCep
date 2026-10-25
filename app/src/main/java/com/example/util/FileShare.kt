package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileShare {
  /** Metni geçici bir dosyaya yazar ve paylaşım menüsünü (WhatsApp, Drive, e-posta...) açar. */
  fun shareText(context: Context, fileName: String, mimeType: String, content: String, chooserTitle: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    // Eski dışa aktarımları temizle (kişisel veri cihazda gereksiz yere kalmasın)
    dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 86_400_000L) it.delete() }
    val file = File(dir, fileName)
    file.writeText(content, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, fileName)
      clipData = ClipData.newRawUri(fileName, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  /** Kullanıcının seçtiği konuma (Dosyalar, Drive...) yazar. */
  fun writeToUri(context: Context, uri: Uri, content: String) {
    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
      ?: throw IllegalStateException("Dosya yazılamadı.")
  }

  fun readFromUri(context: Context, uri: Uri, maxBytes: Int = 20 * 1024 * 1024): String {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
      val out = java.io.ByteArrayOutputStream()
      val buf = ByteArray(8192)
      var total = 0
      while (true) {
        val n = input.read(buf)
        if (n < 0) break
        total += n
        if (total > maxBytes) throw IllegalArgumentException("Dosya çok büyük.")
        out.write(buf, 0, n)
      }
      out.toByteArray()
    } ?: throw IllegalStateException("Dosya açılamadı.")
    return decode(bytes)
  }

  /** UTF-8 değilse (Excel'in eski Windows-1254 kaydı) Türkçe karakterleri kurtarmak için 1254 dener. */
  private fun decode(bytes: ByteArray): String {
    val utf8 = String(bytes, Charsets.UTF_8)
    return if (utf8.contains('�')) {
      runCatching { String(bytes, charset("windows-1254")) }.getOrDefault(utf8)
    } else utf8
  }
}
