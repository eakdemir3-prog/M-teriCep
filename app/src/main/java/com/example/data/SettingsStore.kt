package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import com.example.model.SubscriptionTier
import com.example.model.UserBusinessProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mustericep_settings")

/** İşletme profili ve uygulama tercihleri; uygulama kapansa da kaybolmaz. */
class SettingsStore(private val context: Context) {

  private object Keys {
    val BUSINESS_NAME = stringPreferencesKey("business_name")
    val OWNER_NAME = stringPreferencesKey("owner_name")
    val PHONE = stringPreferencesKey("phone")
    val CITY = stringPreferencesKey("city")
    val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    val REMINDER_DAYS = intPreferencesKey("reminder_days_before")
    val TIER = stringPreferencesKey("active_tier")
    val THEME = stringPreferencesKey("theme_mode")
    val LOCAL_ONLY = booleanPreferencesKey("local_only")
    val PENDING_ROLE = stringPreferencesKey("pending_role")
  }

  /** Kullanıcı "giriş yapmadan sadece defter" modunu seçti mi? */
  val localOnly: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.LOCAL_ONLY] ?: false }

  suspend fun setLocalOnly(value: Boolean) {
    context.settingsDataStore.edit { it[Keys.LOCAL_ONLY] = value }
  }

  /** Giriş öncesi seçilen rol (Google girişi dönünce kaydedilir). */
  val pendingRole: Flow<String?> = context.settingsDataStore.data.map { it[Keys.PENDING_ROLE] }

  suspend fun setPendingRole(role: String?) {
    context.settingsDataStore.edit { if (role == null) it.remove(Keys.PENDING_ROLE) else it[Keys.PENDING_ROLE] = role }
  }

  val profile: Flow<UserBusinessProfile> = context.settingsDataStore.data.map { p ->
    UserBusinessProfile(
      businessName = p[Keys.BUSINESS_NAME] ?: "",
      ownerName = p[Keys.OWNER_NAME] ?: "",
      phone = p[Keys.PHONE] ?: "",
      city = p[Keys.CITY] ?: "",
      notificationsEnabled = p[Keys.NOTIFICATIONS] ?: false,
      reminderDaysBefore = p[Keys.REMINDER_DAYS] ?: 1,
      activeTier = p[Keys.TIER]?.let { runCatching { SubscriptionTier.valueOf(it) }.getOrNull() } ?: SubscriptionTier.FREE,
      themeMode = p[Keys.THEME]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.DEFTER
    )
  }

  suspend fun current(): UserBusinessProfile = profile.first()

  suspend fun updateBusiness(name: String, owner: String, phone: String, city: String) {
    context.settingsDataStore.edit {
      it[Keys.BUSINESS_NAME] = name.trim()
      it[Keys.OWNER_NAME] = owner.trim()
      it[Keys.PHONE] = phone.trim()
      it[Keys.CITY] = city.trim()
    }
  }

  suspend fun setNotifications(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
  }

  suspend fun setReminderDays(days: Int) {
    context.settingsDataStore.edit { it[Keys.REMINDER_DAYS] = days.coerceIn(0, 7) }
  }

  suspend fun setTheme(mode: AppThemeMode) {
    context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
  }

  /** Yalnızca doğrulanmış bir Google Play satın alımından sonra çağrılmalı. */
  suspend fun setTier(tier: SubscriptionTier) {
    context.settingsDataStore.edit { it[Keys.TIER] = tier.name }
  }

  suspend fun restore(profile: UserBusinessProfile) {
    context.settingsDataStore.edit {
      it[Keys.BUSINESS_NAME] = profile.businessName
      it[Keys.OWNER_NAME] = profile.ownerName
      it[Keys.PHONE] = profile.phone
      it[Keys.CITY] = profile.city
      it[Keys.REMINDER_DAYS] = profile.reminderDaysBefore
      it[Keys.THEME] = profile.themeMode.name
    }
  }
}
