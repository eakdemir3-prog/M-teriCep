package com.example.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Uygulama kilidi: açılışta ve arka plandan 30 sn'den uzun sonra dönüşte telefonun kendi
 * kilidi (parmak izi, yüz veya ekran kilidi şifresi) istenir. Ayar, açılışta beklemeden
 * okunabilsin diye küçük bir SharedPreferences'ta tutulur.
 */
object AppLock {
  private const val PREFS = "app_lock"
  private const val KEY = "enabled"
  private const val GRACE_MS = 30_000L

  var locked by mutableStateOf(false)
    private set
  private var authenticating = false
  private var backgroundAt = 0L

  fun isEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

  fun setEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, enabled).apply()
    if (!enabled) locked = false
  }

  /** Telefonda ekran kilidi (PIN, desen, şifre) kurulu mu? Kilit için gerekli. */
  fun deviceSecure(context: Context): Boolean =
    (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

  fun onCreate(activity: Activity, firstLaunch: Boolean) {
    if (firstLaunch && isEnabled(activity) && deviceSecure(activity)) locked = true
  }

  fun onStop() {
    if (!authenticating) backgroundAt = System.currentTimeMillis()
  }

  fun onStart(activity: Activity) {
    if (authenticating || !isEnabled(activity) || !deviceSecure(activity)) return
    if (backgroundAt > 0 && System.currentTimeMillis() - backgroundAt > GRACE_MS) locked = true
    backgroundAt = 0L
    if (locked) authenticate(activity)
  }

  @Suppress("DEPRECATION")
  fun authenticate(activity: Activity) {
    if (authenticating) return
    if (!deviceSecure(activity)) { locked = false; return }
    authenticating = true
    if (Build.VERSION.SDK_INT >= 29) {
      val builder = BiometricPrompt.Builder(activity)
        .setTitle(tr("MüşteriCep kilitli"))
        .setSubtitle(tr("Devam etmek için kimliğinizi doğrulayın"))
      if (Build.VERSION.SDK_INT >= 30) {
        builder.setAllowedAuthenticators(
          BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
      } else {
        builder.setDeviceCredentialAllowed(true)
      }
      builder.build().authenticate(
        CancellationSignal(),
        activity.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            authenticating = false; locked = false
          }
          override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            authenticating = false
          }
        }
      )
    } else {
      val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
      val intent = km.createConfirmDeviceCredentialIntent(tr("MüşteriCep kilitli"), tr("Devam etmek için kimliğinizi doğrulayın"))
      if (intent == null) { authenticating = false; locked = false; return }
      activity.startActivityForResult(intent, REQUEST)
    }
  }

  const val REQUEST = 4711

  /** Android 9 ve altı: ekran kilidi ekranından dönüş. */
  fun onActivityResult(requestCode: Int, resultCode: Int) {
    if (requestCode != REQUEST) return
    authenticating = false
    if (resultCode == Activity.RESULT_OK) locked = false
  }
}
