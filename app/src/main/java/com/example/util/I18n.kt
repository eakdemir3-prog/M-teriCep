package com.example.util

import java.util.Locale

/**
 * Basit arayüz çevirisi. Türkçe metin anahtardır; cihaz dili Türkçe değilse İngilizcesi
 * gösterilir (sözlükte yoksa Türkçe kalır). {0}, {1} ... yer tutucuları sırayla doldurulur.
 */
object I18n {
  /** Cihaz dili Türkçe değilse İngilizce arayüz. */
  val isEnglish: Boolean get() = Locale.getDefault().language != "tr"

  fun format(text: String, args: Array<out Any?>): String {
    if (args.isEmpty()) return text
    var out = text
    args.forEachIndexed { i, a -> out = out.replace("{$i}", a?.toString() ?: "") }
    return out
  }

  private val en: Map<String, String> by lazy { EnStrings.map }

  fun translate(key: String): String = if (isEnglish) en[key] ?: key else key
}

fun tr(key: String, vararg args: Any?): String = I18n.format(I18n.translate(key), args)
