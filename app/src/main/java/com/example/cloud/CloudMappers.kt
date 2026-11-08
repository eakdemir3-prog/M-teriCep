package com.example.cloud

import com.google.firebase.firestore.DocumentSnapshot

/** Firestore <-> model dönüşümleri (yansıma kullanmadan; küçültme/proguard güvenli). */
internal object CloudMappers {

  fun userToMap(u: UserProfile): Map<String, Any?> = mapOf(
    "role" to u.role?.name,
    "displayName" to u.displayName,
    "email" to u.email,
    "phone" to u.phone,
    "favoriteBusinessIds" to u.favoriteBusinessIds,
    "reviewedAppointmentIds" to u.reviewedAppointmentIds,
    "updatedAt" to System.currentTimeMillis()
  )

  @Suppress("UNCHECKED_CAST")
  fun user(doc: DocumentSnapshot): UserProfile = UserProfile(
    uid = doc.id,
    role = doc.getString("role")?.let { runCatching { UserRole.valueOf(it) }.getOrNull() },
    displayName = doc.getString("displayName") ?: "",
    email = doc.getString("email") ?: "",
    phone = doc.getString("phone") ?: "",
    favoriteBusinessIds = (doc.get("favoriteBusinessIds") as? List<String>) ?: emptyList(),
    reviewedAppointmentIds = (doc.get("reviewedAppointmentIds") as? List<String>) ?: emptyList()
  )

  fun staffToMap(s: Staff): Map<String, Any?> = mapOf(
    "name" to s.name.trim(),
    "isActive" to s.isActive,
    "serviceIds" to s.serviceIds
  )

  @Suppress("UNCHECKED_CAST")
  fun staff(doc: DocumentSnapshot): Staff = Staff(
    id = doc.id,
    name = doc.getString("name") ?: "",
    isActive = doc.getBoolean("isActive") ?: true,
    serviceIds = (doc.get("serviceIds") as? List<String>) ?: emptyList()
  )

  fun reviewToMap(r: Review): Map<String, Any?> = mapOf(
    "businessId" to r.businessId,
    "customerUid" to r.customerUid,
    "customerName" to r.customerName,
    "rating" to r.rating,
    "comment" to r.comment.trim(),
    "serviceName" to r.serviceName,
    "createdAt" to r.createdAt
  )

  fun review(doc: DocumentSnapshot): Review = Review(
    id = doc.id,
    businessId = doc.getString("businessId") ?: "",
    customerUid = doc.getString("customerUid") ?: "",
    customerName = doc.getString("customerName") ?: "",
    rating = doc.getLong("rating")?.toInt() ?: 0,
    comment = doc.getString("comment") ?: "",
    serviceName = doc.getString("serviceName") ?: "",
    createdAt = doc.getLong("createdAt") ?: 0L
  )

  fun dayToMap(d: WorkingDay): Map<String, Any?> = mapOf(
    "dayOfWeek" to d.dayOfWeek,
    "isOpen" to d.isOpen,
    "openMinute" to d.openMinute,
    "closeMinute" to d.closeMinute,
    "breakStart" to d.breakStart,
    "breakEnd" to d.breakEnd
  )

  private fun day(m: Map<*, *>): WorkingDay = WorkingDay(
    dayOfWeek = (m["dayOfWeek"] as? Number)?.toInt() ?: 1,
    isOpen = m["isOpen"] as? Boolean ?: false,
    openMinute = (m["openMinute"] as? Number)?.toInt() ?: 540,
    closeMinute = (m["closeMinute"] as? Number)?.toInt() ?: 1080,
    breakStart = (m["breakStart"] as? Number)?.toInt(),
    breakEnd = (m["breakEnd"] as? Number)?.toInt()
  )

  fun businessToMap(b: BusinessProfile): Map<String, Any?> = mapOf(
    "ownerUid" to b.ownerUid,
    "name" to b.name.trim(),
    "nameLower" to b.name.trim().lowercase(java.util.Locale("tr", "TR")),
    "category" to b.category,
    "city" to b.city.trim(),
    "cityLower" to b.city.trim().lowercase(java.util.Locale("tr", "TR")),
    "district" to b.district.trim(),
    "address" to b.address.trim(),
    "phone" to b.phone.trim(),
    "description" to b.description.trim(),
    "isListed" to b.isListed,
    "autoApprove" to b.autoApprove,
    "joinCode" to b.joinCode,
    "workingHours" to b.workingHours.map { dayToMap(it) },
    "bookingWindowDays" to b.bookingWindowDays,
    "minNoticeMinutes" to b.minNoticeMinutes,
    "logo" to b.logo,
    "lat" to b.latitude,
    "lng" to b.longitude,
    "tierRank" to b.tierRank,
    "ratingAvg" to b.ratingAvg,
    "ratingCount" to b.ratingCount,
    "bookingFull" to b.bookingFull,
    "bookingFullMonth" to if (b.bookingFull) currentMonthKey() else "",
    "updatedAt" to System.currentTimeMillis()
  )

  fun business(doc: DocumentSnapshot): BusinessProfile = BusinessProfile(
    id = doc.id,
    ownerUid = doc.getString("ownerUid") ?: doc.id,
    name = doc.getString("name") ?: "",
    category = doc.getString("category") ?: "",
    city = doc.getString("city") ?: "",
    district = doc.getString("district") ?: "",
    address = doc.getString("address") ?: "",
    phone = doc.getString("phone") ?: "",
    description = doc.getString("description") ?: "",
    isListed = doc.getBoolean("isListed") ?: false,
    autoApprove = doc.getBoolean("autoApprove") ?: false,
    joinCode = doc.getString("joinCode") ?: "",
    workingHours = (doc.get("workingHours") as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.let(::day) }
      ?.takeIf { it.isNotEmpty() } ?: WorkingDay.defaultWeek(),
    bookingWindowDays = doc.getLong("bookingWindowDays")?.toInt() ?: 30,
    minNoticeMinutes = doc.getLong("minNoticeMinutes")?.toInt() ?: 60,
    logo = doc.getString("logo") ?: "",
    latitude = doc.getDouble("lat"),
    longitude = doc.getDouble("lng"),
    tierRank = doc.getLong("tierRank")?.toInt() ?: 0,
    ratingAvg = doc.getDouble("ratingAvg") ?: 0.0,
    ratingCount = doc.getLong("ratingCount")?.toInt() ?: 0,
    // Sınır yalnızca dolduğu ay için geçerli: yeni ayda işletme uygulamayı açmasa da randevu açılır.
    bookingFull = (doc.getBoolean("bookingFull") ?: false) && doc.getString("bookingFullMonth") == currentMonthKey()
  )

  fun currentMonthKey(now: Long = System.currentTimeMillis()): String {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = now }
    return "%04d-%02d".format(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1)
  }

  fun serviceToMap(s: ServiceItem): Map<String, Any?> = mapOf(
    "name" to s.name.trim(),
    "durationMinutes" to s.durationMinutes,
    "price" to s.price,
    "isActive" to s.isActive
  )

  fun service(doc: DocumentSnapshot): ServiceItem = ServiceItem(
    id = doc.id,
    name = doc.getString("name") ?: "",
    durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 30,
    price = doc.getDouble("price") ?: 0.0,
    isActive = doc.getBoolean("isActive") ?: true
  )

  fun appointmentToMap(a: Appointment): Map<String, Any?> = mapOf(
    "businessId" to a.businessId,
    "businessName" to a.businessName,
    "customerUid" to a.customerUid,
    "customerName" to a.customerName,
    "customerPhone" to a.customerPhone,
    "serviceId" to a.serviceId,
    "serviceName" to a.serviceName,
    "durationMinutes" to a.durationMinutes,
    "price" to a.price,
    "start" to a.start,
    "dayKey" to a.dayKey,
    "status" to a.status.name,
    "note" to a.note,
    "cancelledBy" to a.cancelledBy,
    "createdAt" to a.createdAt,
    "updatedAt" to a.updatedAt,
    "ledgerPosted" to a.ledgerPosted,
    "businessAddress" to a.businessAddress,
    "businessLat" to a.businessLat,
    "businessLng" to a.businessLng,
    "staffId" to a.staffId,
    "staffName" to a.staffName
  )

  fun appointment(doc: DocumentSnapshot): Appointment = Appointment(
    id = doc.id,
    businessId = doc.getString("businessId") ?: "",
    businessName = doc.getString("businessName") ?: "",
    customerUid = doc.getString("customerUid") ?: "",
    customerName = doc.getString("customerName") ?: "",
    customerPhone = doc.getString("customerPhone") ?: "",
    serviceId = doc.getString("serviceId") ?: "",
    serviceName = doc.getString("serviceName") ?: "",
    durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 30,
    price = doc.getDouble("price") ?: 0.0,
    start = doc.getLong("start") ?: 0L,
    status = doc.getString("status")?.let { runCatching { AppointmentStatus.valueOf(it) }.getOrNull() } ?: AppointmentStatus.PENDING,
    note = doc.getString("note") ?: "",
    cancelledBy = doc.getString("cancelledBy") ?: "",
    dayKey = doc.getString("dayKey") ?: "",
    createdAt = doc.getLong("createdAt") ?: 0L,
    updatedAt = doc.getLong("updatedAt") ?: 0L,
    ledgerPosted = doc.getBoolean("ledgerPosted") ?: false,
    businessAddress = doc.getString("businessAddress") ?: "",
    businessLat = doc.getDouble("businessLat"),
    businessLng = doc.getDouble("businessLng"),
    staffId = doc.getString("staffId") ?: "",
    staffName = doc.getString("staffName") ?: ""
  )
}
