package com.example.cloud

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Firebase yapılandırılmamışsa (google-services.json yoksa) bulut özellikleri kapalı kalır
 * ve uygulama yalnızca yerel defter olarak çalışır; çökmez.
 */
object CloudGate {
  fun isConfigured(context: Context): Boolean =
    runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

  /** google-services eklentisinin ürettiği Google giriş istemci kimliği. */
  fun webClientId(context: Context): String? {
    val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    return if (id == 0) null else context.getString(id)
  }
}

class CloudAuth(private val context: Context) {

  private val auth: FirebaseAuth? get() = if (CloudGate.isConfigured(context)) FirebaseAuth.getInstance() else null

  val currentUser: FirebaseUser? get() = auth?.currentUser

  val userFlow: Flow<FirebaseUser?> = run {
    val a = auth ?: return@run flowOf(null)
    callbackFlow {
      val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
      a.addAuthStateListener(listener)
      awaitClose { a.removeAuthStateListener(listener) }
    }
  }

  /** Google hesabıyla giriş. Hata olursa kullanıcıya gösterilecek Türkçe mesajla istisna fırlatır. */
  suspend fun signInWithGoogle(activity: Activity): FirebaseUser {
    val a = auth ?: throw IllegalStateException("Bulut bağlantısı henüz kurulmadı.")
    val clientId = CloudGate.webClientId(context)
      ?: throw IllegalStateException("Google giriş ayarı eksik (Firebase'de Google girişini açın).")
    val manager = CredentialManager.create(activity)

    suspend fun attempt(option: androidx.credentials.CredentialOption) =
      manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())

    // 1. deneme: hesap seçici alt pencere (en uyumlu yöntem)
    // 2. deneme: "Google ile oturum aç" tam ekran akışı
    val result = try {
      attempt(
        GetGoogleIdOption.Builder()
          .setServerClientId(clientId)
          .setFilterByAuthorizedAccounts(false)
          .setAutoSelectEnabled(false)
          .build()
      )
    } catch (first: GetCredentialException) {
      try {
        attempt(GetSignInWithGoogleOption.Builder(clientId).build())
      } catch (e: GetCredentialCancellationException) {
        throw IllegalStateException(explain(e, first))
      } catch (e: NoCredentialException) {
        throw IllegalStateException("Telefonda Google hesabı bulunamadı. Ayarlar > Hesaplar'dan Google hesabı ekleyin.")
      } catch (e: GetCredentialException) {
        throw IllegalStateException(explain(e, first))
      }
    }
    val credential = result.credential
    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      throw IllegalStateException("Google girişi tamamlanamadı.")
    }
    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
    val authResult = a.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    return authResult.user ?: throw IllegalStateException("Giriş yapılamadı.")
  }

  /** Kullanıcının bastığı iptal ile yapılandırma hatasını ayırt eder (hata kodu ekranda görünür). */
  private fun explain(e: GetCredentialException, first: GetCredentialException): String {
    val detail = listOfNotNull(first.message, e.message).joinToString(" | ").take(160)
    val configProblem = detail.contains("16") || detail.contains("10") || detail.contains("reauth", true) ||
      detail.contains("developer", true) || detail.contains("DEVELOPER_ERROR")
    return if (configProblem || detail.isNotBlank() && !detail.contains("cancel", true)) {
      "Google girişi şu an yapılamıyor (uygulama imzası doğrulanamadı). Hata: $detail"
    } else {
      "Giriş iptal edildi. ($detail)"
    }
  }

  suspend fun signOut() {
    auth?.signOut()
    runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
  }

  /** Hesabı Firebase'den kalıcı olarak siler (Play politikası gereği uygulama içinden yapılabilmeli). */
  suspend fun deleteAccount() {
    currentUser?.delete()?.await()
    signOut()
  }
}
