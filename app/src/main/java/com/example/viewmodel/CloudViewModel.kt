package com.example.viewmodel

import com.example.util.tr

import com.example.model.allows

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloud.Appointment
import com.example.cloud.AppointmentStatus
import com.example.cloud.BusinessProfile
import com.example.cloud.CloudAuth
import com.example.cloud.CloudGate
import com.example.cloud.CloudRepository
import com.example.cloud.ServiceItem
import com.example.cloud.SlotTakenException
import com.example.cloud.Slots
import com.example.cloud.UserProfile
import com.example.cloud.UserRole
import com.example.cloud.dayKeyOf
import com.example.data.AppDatabase
import com.example.data.MusteriCepRepository
import com.example.data.SettingsStore
import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.Debt
import com.example.model.PaymentMethod
import com.example.reminder.AppointmentWorker
import com.example.util.Formatters
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Uygulamanın hangi yüzünün gösterileceği. */
enum class AppMode { LOADING, WELCOME, BUSINESS, CUSTOMER }

/** Müşteri tarafındaki ekranlar. */
sealed class CustomerScreen {
  object Discover : CustomerScreen()
  object MyAppointments : CustomerScreen()
  object Profile : CustomerScreen()
  data class BusinessDetail(val businessId: String) : CustomerScreen()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CloudViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application
  val isConfigured: Boolean = CloudGate.isConfigured(application)
  private val auth = CloudAuth(application)
  private val cloud = CloudRepository()
  private val settings = SettingsStore(application)
  private val ledger = MusteriCepRepository(AppDatabase.getDatabase(application))

  private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
  val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()
  private fun say(text: String) { _messages.tryEmit(UiMessage(text)) }
  fun notify(text: String) = say(text)

  private val _busy = MutableStateFlow(false)
  val busy: StateFlow<Boolean> = _busy.asStateFlow()

  // ---------------- Oturum ----------------

  val firebaseUser: StateFlow<FirebaseUser?> = auth.userFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser)

  /** null = henüz yüklenmedi; profil yoksa rolü null olan boş profil. */
  val userProfile: StateFlow<UserProfile?> = firebaseUser.flatMapLatest { u ->
    if (u == null) flowOf(null)
    else cloud.userFlow(u.uid)
      .map { it ?: UserProfile(uid = u.uid, role = null, displayName = u.displayName ?: "", email = u.email ?: "") }
      .catch { emit(UserProfile(uid = u.uid, role = null, displayName = u.displayName ?: "", email = u.email ?: "")) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val localOnly: StateFlow<Boolean?> = settings.localOnly
    .map<Boolean, Boolean?> { it }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val mode: StateFlow<AppMode> = combine(firebaseUser, userProfile, localOnly) { user, profile, local ->
    when {
      local == null -> AppMode.LOADING
      user == null -> if (local) AppMode.BUSINESS else AppMode.WELCOME
      profile == null -> AppMode.LOADING
      profile.role == UserRole.CUSTOMER -> AppMode.CUSTOMER
      profile.role == UserRole.BUSINESS -> AppMode.BUSINESS
      else -> AppMode.WELCOME
    }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, AppMode.LOADING)

  val isSignedIn: Boolean get() = firebaseUser.value != null

  init {
    // Giriş yapılmışsa ve rol seçilmişse arka plan randevu kontrolünü kur.
    viewModelScope.launch {
      userProfile.collect { p -> if (p?.role != null) AppointmentWorker.schedule(app) }
    }
    // Rol seçip Google girişine gidildiyse, dönüşte rolü kaydet.
    viewModelScope.launch {
      combine(firebaseUser, userProfile) { u, p -> u to p }.collect { (u, p) ->
        if (u != null && p != null && p.role == null) {
          val pending = settings.pendingRole.first()
          if (pending != null) {
            runCatching { UserRole.valueOf(pending) }.getOrNull()?.let { saveRole(u, it) }
          }
        }
      }
    }
  }

  /**
   * Aynı Google hesabıyla işletme ve müşteri modları arasında geçiş. Son kullanılan mod
   * kullanıcı profilinde saklanır; uygulama bir sonraki açılışta o modda başlar.
   */
  fun switchMode(activity: Activity?, to: UserRole) {
    val u = firebaseUser.value
    if (u == null) {
      if (activity != null) signIn(activity, to) else say(tr("Önce Google ile giriş yapın."))
      return
    }
    val p = userProfile.value ?: UserProfile(uid = u.uid, role = null, displayName = u.displayName ?: "", email = u.email ?: "")
    viewModelScope.launch {
      _busy.value = true
      try {
        kotlinx.coroutines.withTimeoutOrNull(15_000) { cloud.saveUser(p.copy(role = to)) }
        if (to == UserRole.BUSINESS) settings.setLocalOnly(false)
        if (to == UserRole.CUSTOMER) _customerStack.value = listOf(CustomerScreen.Discover)
        say(if (to == UserRole.CUSTOMER) tr("Müşteri modundasınız. İşletmenize üstteki şeritten dönebilirsiniz.") else tr("İşletme moduna geçildi."))
      } catch (e: Exception) {
        say(tr("Mod değiştirilemedi: {0}", e.message))
      } finally {
        _busy.value = false
      }
    }
  }

  fun continueLocalOnly() {
    viewModelScope.launch { settings.setLocalOnly(true) }
  }

  fun signIn(activity: Activity, role: UserRole) {
    if (!isConfigured) {
      say(tr("Bulut bağlantısı henüz kurulmadı. Şimdilik giriş yapmadan defteri kullanabilirsiniz."))
      return
    }
    viewModelScope.launch {
      _busy.value = true
      try {
        settings.setPendingRole(role.name)
        val user = auth.signInWithGoogle(activity)
        val existing = runCatching { kotlinx.coroutines.withTimeout(15_000) { cloud.getUser(user.uid) } }.getOrNull()
        // Aynı hesap iki modu da kullanabilir: seçilen moda geçilir.
        if (existing?.role != role) saveRole(user, role)
        settings.setPendingRole(null)
        if (role == UserRole.BUSINESS) settings.setLocalOnly(false)
      } catch (e: Exception) {
        say(e.message ?: tr("Giriş yapılamadı."))
      } finally {
        _busy.value = false
      }
    }
  }

  private suspend fun saveRole(user: FirebaseUser, role: UserRole) {
    try {
      // Firestore yazımı yerelde hemen geçerli olur; sunucu onayı gecikirse arayüzü bekletme.
      kotlinx.coroutines.withTimeoutOrNull(15_000) {
        cloud.saveUser(
          UserProfile(uid = user.uid, role = role, displayName = user.displayName ?: "", email = user.email ?: "")
        )
      }
      settings.setPendingRole(null)
    } catch (e: Exception) {
      say(tr("Hesap kaydedilemedi: {0}", e.message))
    }
  }

  fun signOut() {
    viewModelScope.launch {
      AppointmentWorker.cancel(app)
      runCatching { com.example.cloud.PushService.unregister() }
      kotlinx.coroutines.delay(300)
      auth.signOut()
      settings.setLocalOnly(false)
      _customerStack.value = listOf(CustomerScreen.Discover)
      say(tr("Çıkış yapıldı."))
    }
  }

  /** Hesabı ve buluttaki profil/işletme bilgilerini siler. Telefondaki defter kayıtları silinmez. */
  fun deleteAccount() {
    val user = firebaseUser.value ?: return
    viewModelScope.launch {
      _busy.value = true
      try {
        // Önce buluttaki veriler (giriş açıkken kurallar izin verir), sonra hesabın kendisi.
        kotlinx.coroutines.withTimeoutOrNull(20_000) { runCatching { cloud.deleteUserData(user.uid) } }
        auth.deleteAccount()
        settings.setLocalOnly(false)
        say(tr("Hesabınız silindi."))
      } catch (e: FirebaseAuthRecentLoginRequiredException) {
        say(tr("Güvenlik için çıkış yapıp tekrar giriş yaptıktan sonra hesabı silin."))
      } catch (e: Exception) {
        say(tr("Hesap silinemedi: {0}", e.message))
      } finally {
        _busy.value = false
      }
    }
  }

  fun saveCustomerProfile(name: String, phone: String) {
    val p = userProfile.value ?: return
    if (name.isBlank()) { say(tr("Adınızı girin.")); return }
    if (phone.filter { it.isDigit() }.length < 10) { say(tr("Telefon numarası eksik (en az 10 hane).")); return }
    viewModelScope.launch {
      try {
        cloud.saveUser(p.copy(displayName = name.trim(), phone = phone.trim()))
        say(tr("Bilgileriniz kaydedildi."))
      } catch (e: Exception) {
        say(tr("Kaydedilemedi: {0}", e.message))
      }
    }
  }

  // =============== İŞLETME TARAFI ===============

  val myBusiness: StateFlow<BusinessProfile?> = firebaseUser.flatMapLatest { u ->
    if (u == null) flowOf(null) else cloud.businessFlow(u.uid).catch { emit(null) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val myServices: StateFlow<List<ServiceItem>> = firebaseUser.flatMapLatest { u ->
    if (u == null) flowOf(emptyList()) else cloud.servicesFlow(u.uid).catch { emit(emptyList()) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  /** İşletme sahibinin personel listesi. */
  val myStaff: StateFlow<List<com.example.cloud.Staff>> = firebaseUser.flatMapLatest { u ->
    if (u == null) flowOf(emptyList()) else cloud.staffFlow(u.uid).catch { emit(emptyList()) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  fun saveStaff(s: com.example.cloud.Staff) {
    val u = firebaseUser.value ?: return
    if (s.name.isBlank()) { say(tr("Personel adını girin.")); return }
    val t = tier.value
    if (!t.allows(com.example.model.Feature.STAFF)) {
      say(tr("Personel ve ayrı takvimler İşletme paketinde. Ayarlar > Paketler'den geçebilirsiniz."))
      return
    }
    if (s.id.isBlank() && myStaff.value.size >= t.maxStaff) {
      say(tr("{0} paketinde en fazla {1} personel eklenebilir.", t.title, t.maxStaff))
      return
    }
    viewModelScope.launch {
      try { cloud.saveStaff(u.uid, s); say(tr("Personel kaydedildi.")) } catch (e: Exception) { say(tr("Kaydedilemedi: {0}", e.message)) }
    }
  }

  fun deleteStaff(s: com.example.cloud.Staff) {
    val u = firebaseUser.value ?: return
    viewModelScope.launch {
      try { cloud.deleteStaff(u.uid, s.id); say(tr("Personel silindi.")) } catch (e: Exception) { say(tr("Silinemedi: {0}", e.message)) }
    }
  }

  private val _selectedDay = MutableStateFlow(Formatters.startOfDay(System.currentTimeMillis()))
  val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()
  fun selectDay(dayStart: Long) { _selectedDay.value = Formatters.startOfDay(dayStart) }

  val dayAppointments: StateFlow<List<Appointment>> = combine(firebaseUser, _selectedDay) { u, d -> u to d }
    .flatMapLatest { (u, d) ->
      if (u == null) flowOf(emptyList()) else cloud.businessDayFlow(u.uid, dayKeyOf(d)).catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val todayAppointments: StateFlow<List<Appointment>> = firebaseUser.flatMapLatest { u ->
    if (u == null) flowOf(emptyList())
    else cloud.businessDayFlow(u.uid, dayKeyOf(System.currentTimeMillis())).catch { emit(emptyList()) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val pendingAppointments: StateFlow<List<Appointment>> = firebaseUser.flatMapLatest { u ->
    if (u == null) flowOf(emptyList()) else cloud.businessPendingFlow(u.uid).catch { emit(emptyList()) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // ---------- Paket sınırları ----------
  val tier: StateFlow<com.example.model.SubscriptionTier> = settings.profile
    .map { it.activeTier }
    .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.model.SubscriptionTier.FREE)

  private val _monthCount = MutableStateFlow<Long?>(null)
  /** Bu ayki randevu sayısı (bilinmiyorsa null). */
  val monthAppointmentCount: StateFlow<Long?> = _monthCount.asStateFlow()

  init {
    viewModelScope.launch {
      combine(myBusiness, tier, todayAppointments, pendingAppointments) { b, t, today, p -> Triple(b, t, today.size + p.size) }
        .collectLatest { (b, t, _) ->
          if (b == null) return@collectLatest
          kotlinx.coroutines.delay(800)
          refreshUsage(b, t)
        }
    }
  }

  init {
    viewModelScope.launch {
      firebaseUser.collect { u ->
        if (u != null) runCatching { com.example.cloud.PushService.register(app) }
        runCatching { com.example.billing.BillingManager.identify(u?.uid) }
      }
    }
  }

  private var lastRatingSync = 0L

  private suspend fun refreshUsage(b: BusinessProfile, t: com.example.model.SubscriptionTier) {
    if (System.currentTimeMillis() - lastRatingSync > 3_600_000L) {
      lastRatingSync = System.currentTimeMillis()
      runCatching { cloud.syncRating(b.id) }
    }
    val limited = com.example.model.Billing.ENABLED && t.monthlyAppointments != com.example.model.UNLIMITED
    val count = runCatching { cloud.monthAppointmentCount(b.id) }.getOrNull()
    _monthCount.value = count
    val full = limited && count != null && count >= t.monthlyAppointments
    val rank = if (com.example.model.Billing.ENABLED) t.rank else 0
    if (count != null && (b.bookingFull != full || b.tierRank != rank)) {
      runCatching { cloud.setBusinessUsage(b.id, rank, full) }
    }
  }

  fun saveBusiness(b: BusinessProfile) {
    val u = firebaseUser.value ?: return
    if (b.name.isBlank()) { say(tr("İşletme adı zorunludur.")); return }
    viewModelScope.launch {
      _busy.value = true
      try {
        cloud.saveBusiness(b.copy(id = u.uid, ownerUid = u.uid))
        say(tr("İşletme bilgileri kaydedildi."))
      } catch (e: Exception) {
        say(tr("Kaydedilemedi: {0}", e.message))
      } finally {
        _busy.value = false
      }
    }
  }

  fun saveService(s: ServiceItem) {
    val u = firebaseUser.value ?: return
    if (s.name.isBlank()) { say(tr("Hizmet adı girin.")); return }
    if (s.durationMinutes < 5) { say(tr("Süre en az 5 dakika olmalı.")); return }
    val t = tier.value
    if (com.example.model.Billing.ENABLED && s.id.isBlank() && myServices.value.size >= t.maxServices) {
      say(tr("{0} paketinde en fazla {1} hizmet eklenebilir. Ayarlar > Paketler'den paketinizi yükseltebilirsiniz.", t.title, t.maxServices))
      return
    }
    viewModelScope.launch {
      try { cloud.saveService(u.uid, s); say(tr("Hizmet kaydedildi.")) } catch (e: Exception) { say(tr("Kaydedilemedi: {0}", e.message)) }
    }
  }

  fun deleteService(id: String) {
    val u = firebaseUser.value ?: return
    viewModelScope.launch {
      try { cloud.deleteService(u.uid, id); say(tr("Hizmet silindi.")) } catch (e: Exception) { say(tr("Silinemedi: {0}", e.message)) }
    }
  }

  fun setAppointmentStatus(appt: Appointment, status: AppointmentStatus) {
    val u = firebaseUser.value ?: return
    viewModelScope.launch {
      try {
        cloud.setStatus(appt, status, u.uid)
        say(tr("Randevu: {0}", status.label))
      } catch (e: Exception) {
        say(tr("İşlem yapılamadı: {0}", e.message))
      }
    }
  }

  /** İşletmenin telefonla/yüz yüze aldığı randevuyu takvime eklemesi. */
  fun addManualAppointment(
    customerName: String,
    phone: String,
    service: ServiceItem,
    start: Long,
    note: String,
    staffChoice: List<com.example.cloud.Staff> = emptyList()
  ) {
    val chosen = if (staffChoice.isEmpty()) null else slotStaff[start] ?: staffChoice.first()
    val u = firebaseUser.value ?: return
    val b = myBusiness.value ?: run { say(tr("Önce işletme bilgilerini kaydedin.")); return }
    if (customerName.isBlank()) { say(tr("Müşteri adını girin.")); return }
    if (b.bookingFull) {
      say(tr("{0} paketinin aylık {1} randevu sınırı doldu. Ayarlar > Paketler'den paketinizi yükseltebilirsiniz.", tier.value.title, tier.value.monthlyAppointments))
      return
    }
    viewModelScope.launch {
      _busy.value = true
      try {
        cloud.book(
          Appointment(
            businessId = u.uid, businessName = b.name, customerName = customerName.trim(),
            customerPhone = phone.trim(), serviceId = service.id, serviceName = service.name,
            durationMinutes = service.durationMinutes, price = service.price, start = start,
            status = AppointmentStatus.CONFIRMED, note = note.trim(),
            staffId = chosen?.id ?: "", staffName = chosen?.name ?: ""
          )
        )
        say(tr("Randevu eklendi: {0}", Formatters.formatDateTime(start)))
      } catch (e: Exception) {
        say(if (e.isSlotTaken()) tr("Bu saat dolu, başka bir saat seçin.") else tr("Eklenemedi: {0}", e.message))
      } finally {
        _busy.value = false
      }
    }
  }

  /**
   * Tamamlanan randevuyu borç/tahsilat defterine işler. Müşteri telefonla eşleşirse var olan
   * kayda, yoksa yeni müşteri olarak eklenir. [paid] true ise tahsilat da girilir.
   */
  fun postToLedger(appt: Appointment, paid: Boolean, method: PaymentMethod) {
    viewModelScope.launch {
      try {
        val existing = if (appt.customerPhone.isNotBlank()) ledger.findCustomerByPhone(appt.customerPhone) else null
        val customerId = existing?.id ?: ledger.insertCustomer(
          Customer(name = appt.customerName.ifBlank { tr("Randevu müşterisi") }, phone = appt.customerPhone, notes = tr("Randevudan eklendi"))
        )
        if (appt.price > 0) {
          ledger.insertDebt(
            Debt(customerId = customerId, amount = appt.price, date = appt.start, dueDate = if (paid) null else appt.start,
              description = tr("Randevu: {0}", appt.serviceName), category = tr("Hizmet & İşçilik"))
          )
          if (paid) {
            ledger.insertCollection(
              CollectionRecord(customerId = customerId, amount = appt.price, date = System.currentTimeMillis(),
                paymentMethod = method, notes = tr("Randevu ödemesi: {0}", appt.serviceName)),
              force = true
            )
          }
        }
        if (appt.status != AppointmentStatus.COMPLETED) runCatching { cloud.setStatus(appt, AppointmentStatus.COMPLETED, appt.businessId) }
        runCatching { cloud.markLedgerPosted(appt.id) }
        say(if (paid) tr("Randevu tamamlandı, tahsilat deftere işlendi.") else tr("Randevu tamamlandı, borç olarak deftere yazıldı."))
      } catch (e: Exception) {
        say(tr("Deftere işlenemedi: {0}", e.message))
      }
    }
  }

  // =============== MÜŞTERİ TARAFI ===============

  private val _customerStack = MutableStateFlow<List<CustomerScreen>>(listOf(CustomerScreen.Discover))
  val customerScreen: StateFlow<CustomerScreen> = _customerStack.map { it.last() }
    .stateIn(viewModelScope, SharingStarted.Eagerly, CustomerScreen.Discover)

  fun openCustomerTab(screen: CustomerScreen) { _customerStack.value = listOf(screen) }
  fun openBusiness(id: String) { _customerStack.value = _customerStack.value + CustomerScreen.BusinessDetail(id) }
  fun customerBack(): Boolean {
    val s = _customerStack.value
    return when {
      s.size > 1 -> { _customerStack.value = s.dropLast(1); true }
      s.last() !is CustomerScreen.Discover -> { _customerStack.value = listOf(CustomerScreen.Discover); true }
      else -> false
    }
  }

  val myAppointments: StateFlow<List<Appointment>> = firebaseUser.flatMapLatest { u ->
    if (u == null) flowOf(emptyList()) else cloud.customerAppointmentsFlow(u.uid).catch { emit(emptyList()) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _listed = MutableStateFlow<List<BusinessProfile>>(emptyList())
  val listedBusinesses: StateFlow<List<BusinessProfile>> = _listed.asStateFlow()
  private val _favorites = MutableStateFlow<List<BusinessProfile>>(emptyList())
  val favoriteBusinesses: StateFlow<List<BusinessProfile>> = _favorites.asStateFlow()

  fun loadListed(city: String?) {
    if (!isSignedIn) return
    viewModelScope.launch {
      try {
        val me = firebaseUser.value?.uid
        _listed.value = cloud.listBusinesses(city)
          .filter { it.id != me }
          .sortedWith(compareByDescending<BusinessProfile> { it.tierRank >= 2 }.thenBy { it.name.lowercase() })
      } catch (e: Exception) { say(tr("İşletmeler yüklenemedi: {0}", e.message)) }
    }
  }

  fun loadFavorites() {
    val ids = userProfile.value?.favoriteBusinessIds.orEmpty()
    viewModelScope.launch { _favorites.value = runCatching { cloud.getBusinesses(ids) }.getOrDefault(emptyList()) }
  }

  fun findByCode(code: String) {
    viewModelScope.launch {
      _busy.value = true
      try {
        val b = cloud.findByCode(code)
        if (b == null) say(tr("Bu koda ait işletme bulunamadı."))
        else {
          firebaseUser.value?.let { u -> runCatching { cloud.setFavorite(u.uid, b.id, true) } }
          openBusiness(b.id)
        }
      } catch (e: Exception) {
        say(tr("Aranamadı: {0}", e.message))
      } finally {
        _busy.value = false
      }
    }
  }

  fun toggleFavorite(businessId: String) {
    val u = firebaseUser.value ?: return
    val isFav = userProfile.value?.favoriteBusinessIds?.contains(businessId) == true
    viewModelScope.launch {
      runCatching { cloud.setFavorite(u.uid, businessId, !isFav) }
        .onSuccess { say(if (isFav) tr("Favorilerden çıkarıldı.") else tr("Favorilere eklendi.")) }
    }
  }

  // Seçili işletme (randevu ekranı)
  private val _viewedBusinessId = MutableStateFlow<String?>(null)
  fun viewBusiness(id: String) {
    _viewedBusinessId.value = id
    _viewedReviews.value = emptyList()
    viewModelScope.launch { _viewedReviews.value = runCatching { cloud.latestReviews(id) }.getOrDefault(emptyList()) }
  }

  private val _viewedReviews = MutableStateFlow<List<com.example.cloud.Review>>(emptyList())
  val viewedReviews: StateFlow<List<com.example.cloud.Review>> = _viewedReviews.asStateFlow()

  private val _myReviews = MutableStateFlow<List<com.example.cloud.Review>>(emptyList())
  /** İşletme sahibinin gördüğü son yorumlar. */
  val myBusinessReviews: StateFlow<List<com.example.cloud.Review>> = _myReviews.asStateFlow()

  fun loadMyBusinessReviews() {
    val b = myBusiness.value ?: return
    viewModelScope.launch {
      _myReviews.value = runCatching { cloud.latestReviews(b.id, 50) }.getOrDefault(emptyList())
      runCatching { cloud.syncRating(b.id) }
    }
  }

  /** Müşteri: geçmiş randevu için puan ve yorum. */
  fun submitReview(a: Appointment, rating: Int, comment: String) {
    val u = firebaseUser.value ?: return
    if (rating !in 1..5) { say(tr("Lütfen 1 ile 5 arasında yıldız seçin.")); return }
    if (comment.length > 300) { say(tr("Yorum en fazla 300 karakter olabilir.")); return }
    val name = (userProfile.value?.displayName ?: u.displayName ?: "").trim().split(Regex("\\s+")).let { parts ->
      when {
        parts.isEmpty() || parts[0].isBlank() -> tr("Müşteri")
        parts.size == 1 -> parts[0]
        else -> parts[0] + " " + parts.last().take(1) + "."
      }
    }
    viewModelScope.launch {
      try {
        cloud.submitReview(
          com.example.cloud.Review(
            id = a.id, businessId = a.businessId, customerUid = u.uid, customerName = name,
            rating = rating, comment = comment.trim(), serviceName = a.serviceName
          )
        )
        say(tr("Teşekkürler! Değerlendirmeniz kaydedildi."))
      } catch (e: Exception) {
        say(tr("Değerlendirme kaydedilemedi: {0}", e.message))
      }
    }
  }

  val viewedBusiness: StateFlow<BusinessProfile?> = _viewedBusinessId.flatMapLatest { id ->
    if (id == null) flowOf(null) else cloud.businessFlow(id).catch { emit(null) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  /** Müşterinin baktığı işletmenin etkin personeli (boşsa personelsiz işletme). */
  val viewedStaff: StateFlow<List<com.example.cloud.Staff>> = _viewedBusinessId.flatMapLatest { id ->
    if (id == null) flowOf(emptyList()) else cloud.staffFlow(id).map { l -> l.filter { it.isActive } }.catch { emit(emptyList()) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val viewedServices: StateFlow<List<ServiceItem>> = _viewedBusinessId.flatMapLatest { id ->
    if (id == null) flowOf(emptyList()) else cloud.servicesFlow(id).map { l -> l.filter { it.isActive } }.catch { emit(emptyList()) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _slots = MutableStateFlow<List<Long>?>(null)
  /** null = yükleniyor */
  val availableSlots: StateFlow<List<Long>?> = _slots.asStateFlow()

  /** Seçilen gün ve hizmet için boş saatleri getirir (işletme ve müşteri tarafında ortak). */
  /** Saat → o saatte boş olan personel (personelli işletmede "Fark etmez" seçimi için). */
  private var slotStaff: Map<Long, com.example.cloud.Staff> = emptyMap()

  /**
   * [staff] boşsa işletme geneli saatler. Doluysa seçilen personelin boş saatleri; birden fazla
   * personel verilirse ("Fark etmez") en az birinin boş olduğu saatler.
   */
  fun loadSlots(
    business: BusinessProfile,
    dayStart: Long,
    durationMinutes: Int,
    applyNotice: Boolean = true,
    staff: List<com.example.cloud.Staff> = emptyList()
  ) {
    _slots.value = null
    viewModelScope.launch {
      try {
        val taken = cloud.takenBlocks(business.id, dayKeyOf(dayStart))
        fun avail(staffId: String) = Slots.available(
          day = business.dayFor(dayStart),
          dayStart = dayStart,
          durationMinutes = durationMinutes,
          takenBlocks = taken,
          now = System.currentTimeMillis(),
          minNoticeMinutes = if (applyNotice) business.minNoticeMinutes else 0,
          staffId = staffId
        )
        if (staff.isEmpty()) {
          slotStaff = emptyMap()
          _slots.value = avail("")
        } else {
          val map = linkedMapOf<Long, com.example.cloud.Staff>()
          staff.forEach { st -> avail(st.id).forEach { t -> if (t !in map) map[t] = st } }
          slotStaff = map
          _slots.value = map.keys.sorted()
        }
      } catch (e: Exception) {
        _slots.value = emptyList()
        say(tr("Saatler yüklenemedi: {0}", e.message))
      }
    }
  }

  fun book(
    business: BusinessProfile,
    service: ServiceItem,
    start: Long,
    note: String,
    staffChoice: List<com.example.cloud.Staff> = emptyList(),
    onDone: () -> Unit
  ) {
    val chosen = if (staffChoice.isEmpty()) null else slotStaff[start] ?: staffChoice.first()
    val u = firebaseUser.value ?: return
    val p = userProfile.value ?: return
    if (p.phone.filter { it.isDigit() }.length < 10) {
      say(tr("Randevu için önce Profil'den telefon numaranızı girin."))
      openCustomerTab(CustomerScreen.Profile)
      return
    }
    if (business.id == u.uid) {
      say(tr("Kendi işletmenizden randevu alamazsınız. Takviminize randevu eklemek için işletme moduna dönün."))
      return
    }
    if (business.bookingFull) {
      say(tr("Bu işletme bu ay online randevu kapasitesine ulaştı. Telefonla arayarak randevu alabilirsiniz."))
      return
    }
    viewModelScope.launch {
      _busy.value = true
      try {
        val status = if (business.autoApprove) AppointmentStatus.CONFIRMED else AppointmentStatus.PENDING
        cloud.book(
          Appointment(
            businessId = business.id, businessName = business.name, customerUid = u.uid,
            customerName = p.displayName.ifBlank { u.displayName ?: tr("Müşteri") }, customerPhone = p.phone,
            serviceId = service.id, serviceName = service.name, durationMinutes = service.durationMinutes,
            price = service.price, start = start, status = status, note = note.trim(),
            businessAddress = com.example.cloud.BusinessLocation.addressText(business),
            businessLat = business.latitude, businessLng = business.longitude,
            staffId = chosen?.id ?: "", staffName = chosen?.name ?: ""
          )
        )
        runCatching { cloud.setFavorite(u.uid, business.id, true) }
        AppointmentWorker.scheduleReminder(app, start, business.name, service.name)
        say(if (status == AppointmentStatus.CONFIRMED) tr("Randevunuz alındı: {0}", Formatters.formatDateTime(start)) else tr("Randevu isteğiniz işletmeye iletildi. Onaylanınca bildirim alacaksınız."))
        onDone()
      } catch (e: Exception) {
        say(if (e.isSlotTaken()) tr("Bu saat az önce doldu, lütfen başka bir saat seçin.") else tr("Randevu alınamadı: {0}", e.message))
        loadSlots(business, Formatters.startOfDay(start), service.durationMinutes, staff = staffChoice)
      } finally {
        _busy.value = false
      }
    }
  }

  fun cancelMyAppointment(appt: Appointment) {
    val u = firebaseUser.value ?: return
    viewModelScope.launch {
      try {
        cloud.setStatus(appt, AppointmentStatus.CANCELLED, u.uid)
        androidx.work.WorkManager.getInstance(app).cancelUniqueWork("hatirlatma_${appt.start}")
        say(tr("Randevu iptal edildi."))
      } catch (e: Exception) {
        say(tr("İptal edilemedi: {0}", e.message))
      }
    }
  }

  private fun Throwable.isSlotTaken(): Boolean {
    var t: Throwable? = this
    while (t != null) {
      if (t is SlotTakenException) return true
      t = t.cause
    }
    return false
  }
}
