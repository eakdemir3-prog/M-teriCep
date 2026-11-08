package com.example.cloud

import android.content.Context
import com.example.reminder.AppointmentWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Anlık bildirimler (Firebase Cloud Messaging). Bildirimleri sunucudaki Cloud Function gönderir:
 * işletmeye yeni randevu isteği, müşteriye onay / ret / iptal.
 */
class PushService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    saveToken(this, token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    val title = message.data["title"] ?: message.notification?.title ?: return
    val body = message.data["body"] ?: message.notification?.body ?: ""
    val id = (message.data["appointmentId"] ?: message.messageId ?: title).hashCode()
    AppointmentWorker.notify(this, id, title, listOf(body))
  }

  companion object {
    /** Giriş yapılmışsa cihazın bildirim anahtarını kullanıcı kaydına ekler. */
    fun register(context: Context) {
      if (FirebaseAuth.getInstance().currentUser == null) return
      FirebaseMessaging.getInstance().token.addOnSuccessListener { saveToken(context, it) }
    }

    fun saveToken(@Suppress("UNUSED_PARAMETER") context: Context, token: String) {
      val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
      FirebaseFirestore.getInstance().collection("users").document(uid)
        .update("fcmTokens", FieldValue.arrayUnion(token))
    }

    /** Çıkışta bu cihaza bildirim gitmesin. */
    fun unregister() {
      val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
      FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
        FirebaseFirestore.getInstance().collection("users").document(uid)
          .update("fcmTokens", FieldValue.arrayRemove(t))
      }
    }
  }
}
