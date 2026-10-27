package com.example.cloud

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppPalette
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * İşletme logosu: Firebase Storage (ücretli plan) gerektirmesin diye küçük bir JPEG olarak
 * (256 px, ~20 KB) işletme belgesinin içinde base64 metin olarak saklanır.
 */
object BusinessLogo {
  private const val SIZE = 256
  private val cache = LruCache<Int, ImageBitmap>(40)

  /** Seçilen görseli kare kırpıp küçültür. Okunamazsa null. */
  suspend fun encode(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
      var sample = 1
      while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
      val opts = BitmapFactory.Options().apply { inSampleSize = sample }
      val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: return@runCatching null
      val side = minOf(src.width, src.height)
      val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
      val scaled = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
      // Saydam PNG'ler JPEG'de siyah olmasın diye beyaz zemine çiz.
      val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
      android.graphics.Canvas(out).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(scaled, 0f, 0f, null) }
      val bytes = ByteArrayOutputStream().use { os ->
        out.compress(Bitmap.CompressFormat.JPEG, 82, os); os.toByteArray()
      }
      Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()
  }

  fun decode(data: String): ImageBitmap? {
    if (data.isBlank()) return null
    val key = data.hashCode()
    cache.get(key)?.let { return it }
    return runCatching {
      val bytes = Base64.decode(data, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()?.also { cache.put(key, it) }
  }
}

/** Logo varsa logoyu, yoksa mağaza simgesini gösterir. */
@Composable
fun BusinessLogoImage(logo: String, size: Dp = 44.dp, corner: Dp = 12.dp) {
  val palette = LocalAppPalette.current
  val bmp = remember(logo) { BusinessLogo.decode(logo) }
  val shape = RoundedCornerShape(corner)
  if (bmp != null) {
    Image(
      bitmap = bmp, contentDescription = "İşletme logosu", contentScale = ContentScale.Crop,
      modifier = Modifier.size(size).clip(shape)
    )
  } else {
    Box(
      modifier = Modifier.size(size).clip(shape).background(palette.accentSoft),
      contentAlignment = Alignment.Center
    ) {
      Icon(Icons.Default.Storefront, contentDescription = null, tint = palette.ink, modifier = Modifier.size(size * 0.5f))
    }
  }
}

object BusinessLocation {
  /** Telefonun şu anki konumu (izin verilmiş olmalı). Alınamazsa null. */
  @SuppressLint("MissingPermission")
  suspend fun current(context: Context): Pair<Double, Double>? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val token = CancellationTokenSource()
    val loc = withTimeoutOrNull(20_000) {
      runCatching { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await() }.getOrNull()
    } ?: run {
      token.cancel()
      runCatching { client.lastLocation.await() }.getOrNull()
    }
    return loc?.let { it.latitude to it.longitude }
  }

  fun format(lat: Double, lng: Double): String = String.format(Locale.US, "%.5f, %.5f", lat, lng)

  /** Google Haritalar'da yol tarifi (uygulama yoksa tarayıcıda açılır). */
  fun directionsIntent(lat: Double?, lng: Double?, fallbackQuery: String): Intent {
    val dest = if (lat != null && lng != null) String.format(Locale.US, "%.6f,%.6f", lat, lng) else fallbackQuery
    return Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(dest)))
  }

  /** Konumu haritada iğneyle gösterir. */
  fun showIntent(lat: Double, lng: Double, label: String): Intent {
    val q = String.format(Locale.US, "%.6f,%.6f", lat, lng)
    return Intent(Intent.ACTION_VIEW, Uri.parse("geo:$q?q=" + Uri.encode("$q($label)")))
  }

  fun addressText(b: BusinessProfile): String =
    listOf(b.address, b.district, b.city).filter { it.isNotBlank() }.joinToString(", ")
}
