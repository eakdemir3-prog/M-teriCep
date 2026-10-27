package com.example.cloud

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class SlotTakenException : Exception("Bu saat az önce doldu. Lütfen başka bir saat seçin.")

/**
 * Bulut (Firestore) işlemleri. Veri yapısı:
 *  users/{uid}                          rol, ad, telefon, favoriler
 *  businesses/{ownerUid}                işletme vitrini (herkes okuyabilir)
 *  businesses/{id}/services/{sid}       hizmetler
 *  businesses/{id}/locks/{blok}         dolu 15 dk'lık bloklar (çift randevuyu engeller)
 *  appointments/{id}                    randevular (yalnız müşteri ve işletme görür)
 */
class CloudRepository {

  private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

  private fun users() = db.collection("users")
  private fun businesses() = db.collection("businesses")
  private fun appointments() = db.collection("appointments")

  // ---------------- Kullanıcı ----------------

  suspend fun getUser(uid: String): UserProfile? {
    val doc = users().document(uid).get().await()
    return if (doc.exists()) CloudMappers.user(doc) else null
  }

  fun userFlow(uid: String): Flow<UserProfile?> = docFlow(users().document(uid)) { if (it.exists()) CloudMappers.user(it) else null }

  /**
   * Hesap silinirken buluttaki kişisel verileri kaldırır: kullanıcı profili ve (işletmeyse)
   * işletme vitrini, logo, konum ve hizmetler.
   */
  suspend fun deleteUserData(uid: String) {
    runCatching {
      val services = businesses().document(uid).collection("services").get().await()
      services.documents.forEach { it.reference.delete().await() }
    }
    runCatching { businesses().document(uid).delete().await() }
    users().document(uid).delete().await()
  }

  suspend fun saveUser(user: UserProfile) {
    users().document(user.uid).set(CloudMappers.userToMap(user), SetOptions.merge()).await()
  }

  suspend fun setFavorite(uid: String, businessId: String, favorite: Boolean) {
    users().document(uid).update(
      "favoriteBusinessIds",
      if (favorite) FieldValue.arrayUnion(businessId) else FieldValue.arrayRemove(businessId)
    ).await()
  }

  // ---------------- İşletme ----------------

  suspend fun getBusiness(id: String): BusinessProfile? {
    val doc = businesses().document(id).get().await()
    return if (doc.exists()) CloudMappers.business(doc) else null
  }

  fun businessFlow(id: String): Flow<BusinessProfile?> =
    docFlow(businesses().document(id)) { if (it.exists()) CloudMappers.business(it) else null }

  suspend fun saveBusiness(b: BusinessProfile): BusinessProfile {
    val withCode = if (b.joinCode.isBlank()) b.copy(joinCode = newUniqueCode()) else b
    businesses().document(withCode.id).set(CloudMappers.businessToMap(withCode), SetOptions.merge()).await()
    return withCode
  }

  suspend fun findByCode(code: String): BusinessProfile? {
    val clean = code.trim().uppercase()
    if (clean.length < 4) return null
    val snap = businesses().whereEqualTo("joinCode", clean).limit(1).get().await()
    return snap.documents.firstOrNull()?.let(CloudMappers::business)
  }

  /** Herkese açık işletmeler. Şehir verilirse o şehirdekiler. */
  suspend fun listBusinesses(city: String?): List<BusinessProfile> {
    var q: Query = businesses().whereEqualTo("isListed", true)
    if (!city.isNullOrBlank()) q = q.whereEqualTo("cityLower", city.trim().lowercase(java.util.Locale("tr", "TR")))
    return q.limit(200).get().await().documents.map(CloudMappers::business)
  }

  suspend fun getBusinesses(ids: List<String>): List<BusinessProfile> =
    ids.distinct().take(30).mapNotNull { runCatching { getBusiness(it) }.getOrNull() }

  private suspend fun newUniqueCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // karışan 0/O, 1/I yok
    val rnd = SecureRandom()
    repeat(10) {
      val code = (1..6).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
      if (findByCode(code) == null) return code
    }
    return (1..8).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
  }

  // ---------------- Hizmetler ----------------

  fun servicesFlow(businessId: String): Flow<List<ServiceItem>> =
    queryFlow(businesses().document(businessId).collection("services")) { docs ->
      docs.map(CloudMappers::service).sortedBy { it.name.lowercase() }
    }

  suspend fun getServices(businessId: String): List<ServiceItem> =
    businesses().document(businessId).collection("services").get().await().documents.map(CloudMappers::service)

  suspend fun saveService(businessId: String, s: ServiceItem) {
    val col = businesses().document(businessId).collection("services")
    val ref = if (s.id.isBlank()) col.document() else col.document(s.id)
    ref.set(CloudMappers.serviceToMap(s)).await()
  }

  suspend fun deleteService(businessId: String, serviceId: String) {
    businesses().document(businessId).collection("services").document(serviceId).delete().await()
  }

  // ---------------- Müsaitlik ve randevu ----------------

  /** O gün dolu olan 15 dakikalık bloklar. */
  suspend fun takenBlocks(businessId: String, dayKey: String): Set<String> =
    businesses().document(businessId).collection("locks")
      .whereEqualTo("day", dayKey).get().await()
      .documents.map { it.id }.toSet()

  /**
   * Randevu oluşturur. Kapladığı her blok için kilit kaydı tek bir işlem (transaction)
   * içinde yazılır; blok doluysa [SlotTakenException] fırlatır.
   */
  suspend fun book(appointment: Appointment): String {
    val apptRef = appointments().document()
    val bizRef = businesses().document(appointment.businessId)
    val keys = Slots.blockKeys(appointment.start, appointment.durationMinutes)
    val lockRefs = keys.map { bizRef.collection("locks").document(it) }
    db.runTransaction { tx ->
      val existing = lockRefs.map { tx.get(it) }
      if (existing.any { it.exists() }) throw SlotTakenException()
      tx.set(apptRef, CloudMappers.appointmentToMap(appointment))
      lockRefs.forEach { ref ->
        tx.set(
          ref,
          mapOf(
            "day" to appointment.dayKey,
            "appointmentId" to apptRef.id,
            "businessId" to appointment.businessId,
            "customerUid" to appointment.customerUid
          )
        )
      }
      null
    }.await()
    return apptRef.id
  }

  /** Durum değişikliği. İptal/ret durumunda saat tekrar boşa çıkar. */
  suspend fun setStatus(appt: Appointment, status: AppointmentStatus, byUid: String) {
    val batch = db.batch()
    val updates = mutableMapOf<String, Any>(
      "status" to status.name,
      "updatedAt" to System.currentTimeMillis()
    )
    if (status == AppointmentStatus.CANCELLED || status == AppointmentStatus.REJECTED) {
      updates["cancelledBy"] = byUid
      Slots.blockKeys(appt.start, appt.durationMinutes).forEach { key ->
        batch.delete(businesses().document(appt.businessId).collection("locks").document(key))
      }
    }
    batch.update(appointments().document(appt.id), updates)
    batch.commit().await()
  }

  suspend fun markLedgerPosted(appointmentId: String) {
    appointments().document(appointmentId)
      .update(mapOf("ledgerPosted" to true, "updatedAt" to System.currentTimeMillis())).await()
  }

  /** İşletmenin belirli bir günü (sıralama cihazda yapılır; bileşik indeks gerekmez). */
  fun businessDayFlow(businessId: String, dayKey: String): Flow<List<Appointment>> =
    queryFlow(appointments().whereEqualTo("businessId", businessId).whereEqualTo("dayKey", dayKey)) { docs ->
      docs.map(CloudMappers::appointment).sortedBy { it.start }
    }

  fun businessPendingFlow(businessId: String): Flow<List<Appointment>> =
    queryFlow(appointments().whereEqualTo("businessId", businessId).whereEqualTo("status", AppointmentStatus.PENDING.name)) { docs ->
      docs.map(CloudMappers::appointment).sortedBy { it.start }
    }

  suspend fun businessPendingOnce(businessId: String): List<Appointment> =
    appointments().whereEqualTo("businessId", businessId).whereEqualTo("status", AppointmentStatus.PENDING.name)
      .get().await().documents.map(CloudMappers::appointment)

  fun customerAppointmentsFlow(uid: String): Flow<List<Appointment>> =
    queryFlow(appointments().whereEqualTo("customerUid", uid)) { docs ->
      docs.map(CloudMappers::appointment).sortedByDescending { it.start }
    }

  suspend fun customerAppointmentsOnce(uid: String): List<Appointment> =
    appointments().whereEqualTo("customerUid", uid).get().await().documents.map(CloudMappers::appointment)

  // ---------------- Yardımcılar ----------------

  private fun <T> docFlow(
    ref: com.google.firebase.firestore.DocumentReference,
    map: (com.google.firebase.firestore.DocumentSnapshot) -> T
  ): Flow<T> = callbackFlow {
    val reg: ListenerRegistration = ref.addSnapshotListener { snap, err ->
      if (err != null) { close(err); return@addSnapshotListener }
      if (snap != null) trySend(map(snap))
    }
    awaitClose { reg.remove() }
  }

  private fun <T> queryFlow(
    query: Query,
    map: (List<com.google.firebase.firestore.DocumentSnapshot>) -> T
  ): Flow<T> = callbackFlow {
    val reg: ListenerRegistration = query.addSnapshotListener { snap, err ->
      if (err != null) { close(err); return@addSnapshotListener }
      if (snap != null) trySend(map(snap.documents))
    }
    awaitClose { reg.remove() }
  }
}
