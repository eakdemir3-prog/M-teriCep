package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val IntroBackground = Color(0xFFFFFFFF)
private val IntroInk = Color(0xFF172033)
private val IntroGreen = Color(0xFF0F766E)
private val IntroAccent = Color(0xFF10B981)
private val IntroMuted = Color(0xFF64748B)

/**
 * Açılış animasyonu (~1,3 sn): simge yaylanarak büyür, arkasında yeşil halka yayılır,
 * ardından uygulama adı ve alt yazı belirir; sonra tüm katman sönerek uygulamayı açar.
 * Uygulama içeriği bu sırada arkada yüklenir, yani açılışı geciktirmez.
 * Dokunulursa hemen kapanır; sistemde animasyonlar kapalıysa hiç oynamaz.
 */
@Composable
fun IntroAnimation(onFinished: () -> Unit) {
  val context = LocalContext.current
  val animationsOff = remember {
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
      .getOrDefault(false)
  }
  if (animationsOff) {
    LaunchedEffect(Unit) { onFinished() }
    return
  }

  val iconScale = remember { Animatable(0.72f) }
  val iconAlpha = remember { Animatable(0f) }
  val ring = remember { Animatable(0f) }
  val titleAlpha = remember { Animatable(0f) }
  val titleShift = remember { Animatable(1f) }
  val subAlpha = remember { Animatable(0f) }
  val layerAlpha = remember { Animatable(1f) }
  val density = LocalDensity.current

  LaunchedEffect(Unit) {
    coroutineScope {
      launch { iconAlpha.animateTo(1f, tween(220)) }
      launch { iconScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)) }
      launch { delay(120); ring.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
      launch {
        delay(330)
        launch { titleAlpha.animateTo(1f, tween(380)) }
        titleShift.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
      }
      launch { delay(560); subAlpha.animateTo(1f, tween(360)) }
    }
    delay(380)
    layerAlpha.animateTo(0f, tween(280))
    onFinished()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer { alpha = layerAlpha.value }
      .background(IntroBackground)
      .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFinished() },
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        // Yayılan halka
        Canvas(modifier = Modifier.fillMaxSize()) {
          val r = ring.value
          if (r > 0f) {
            val base = size.minDimension / 2f
            drawCircle(color = IntroAccent.copy(alpha = 0.10f * (1f - r)), radius = base * (0.55f + 0.45f * r))
            drawCircle(
              color = IntroGreen.copy(alpha = 0.35f * (1f - r)),
              radius = base * (0.5f + 0.5f * r),
              style = Stroke(width = 2.dp.toPx())
            )
          }
        }
        Image(
          painter = painterResource(R.drawable.ic_launcher_foreground),
          contentDescription = null,
          modifier = Modifier
            .size(168.dp)
            .graphicsLayer {
              scaleX = iconScale.value
              scaleY = iconScale.value
              alpha = iconAlpha.value
            }
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        "MüşteriCep",
        color = IntroInk,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        modifier = Modifier.graphicsLayer {
          alpha = titleAlpha.value
          translationY = with(density) { 14.dp.toPx() } * titleShift.value
        }
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        "Tahsilat • Randevu • Müşteri",
        color = IntroMuted,
        fontSize = 14.sp,
        modifier = Modifier.graphicsLayer { alpha = subAlpha.value }
      )
    }
    Text(
      "EA Vision Labs",
      color = IntroMuted.copy(alpha = 0.8f),
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = 1.sp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 28.dp)
        .graphicsLayer { alpha = subAlpha.value }
    )
  }
}
