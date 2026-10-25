package com.example.util

import com.example.model.AppThemeMode
import com.example.model.CollectionRecord
import com.example.model.Customer
import com.example.model.Debt
import com.example.model.PaymentMethod
import com.example.model.UserBusinessProfile
import com.example.model.WhatsAppTemplate
import org.json.JSONArray
import org.json.JSONObject

data class BackupSnapshot(
  val customers: List<Customer>,
  val debts: List<Debt>,
  val collections: List<CollectionRecord>,
  val templates: List<WhatsAppTemplate>,
  val profile: UserBusinessProfile? = null,
  val createdAt: Long = System.currentTimeMillis()
)

/**
 * Tam yedek: tüm müşteri, borç, tahsilat, şablon ve işletme bilgileri tek bir JSON
 * dosyasında. Kullanıcı bu dosyayı Drive'a, WhatsApp'a veya telefona kaydedebilir;
 * yeni telefonda "Yedekten geri yükle" ile her şey geri gelir.
 */
object BackupManager {
  private const val FORMAT = "mustericep-backup"
  private const val VERSION = 1

  fun toJson(s: BackupSnapshot): String {
    val root = JSONObject()
    root.put("format", FORMAT)
    root.put("version", VERSION)
    root.put("createdAt", s.createdAt)

    s.profile?.let { p ->
      root.put("profile", JSONObject().apply {
        put("businessName", p.businessName)
        put("ownerName", p.ownerName)
        put("phone", p.phone)
        put("city", p.city)
        put("reminderDaysBefore", p.reminderDaysBefore)
        put("themeMode", p.themeMode.name)
      })
    }

    root.put("customers", JSONArray().apply {
      s.customers.forEach { c ->
        put(JSONObject().apply {
          put("id", c.id); put("name", c.name); put("phone", c.phone); put("email", c.email)
          put("address", c.address); put("notes", c.notes); put("isArchived", c.isArchived)
          put("createdAt", c.createdAt)
        })
      }
    })
    root.put("debts", JSONArray().apply {
      s.debts.forEach { d ->
        put(JSONObject().apply {
          put("id", d.id); put("customerId", d.customerId); put("amount", d.amount); put("date", d.date)
          put("dueDate", d.dueDate ?: JSONObject.NULL); put("description", d.description)
          put("category", d.category); put("createdAt", d.createdAt); put("updatedAt", d.updatedAt)
          put("isDeleted", d.isDeleted)
        })
      }
    })
    root.put("collections", JSONArray().apply {
      s.collections.forEach { c ->
        put(JSONObject().apply {
          put("id", c.id); put("customerId", c.customerId); put("debtId", c.debtId ?: JSONObject.NULL)
          put("amount", c.amount); put("date", c.date); put("paymentMethod", c.paymentMethod.name)
          put("notes", c.notes); put("createdAt", c.createdAt); put("updatedAt", c.updatedAt)
          put("isDeleted", c.isDeleted)
        })
      }
    })
    root.put("templates", JSONArray().apply {
      s.templates.forEach { t ->
        put(JSONObject().apply {
          put("id", t.id); put("key", t.key); put("title", t.title)
          put("templateText", t.templateText); put("isCustom", t.isCustom)
        })
      }
    })
    return root.toString(2)
  }

  /** Dosyayı okur; MüşteriCep yedeği değilse veya bozuksa anlaşılır bir hata fırlatır. */
  fun fromJson(json: String): BackupSnapshot {
    val root = try {
      JSONObject(json)
    } catch (e: Exception) {
      throw IllegalArgumentException("Dosya okunamadı. Bu bir MüşteriCep yedek dosyası değil.")
    }
    if (root.optString("format") != FORMAT) {
      throw IllegalArgumentException("Bu dosya bir MüşteriCep yedeği değil.")
    }

    fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)

    val customers = root.optJSONArray("customers").objects().map { o ->
      Customer(
        id = o.getLong("id"), name = o.getString("name"), phone = o.optString("phone"),
        email = o.optString("email"), address = o.optString("address"), notes = o.optString("notes"),
        isArchived = o.optBoolean("isArchived"), createdAt = o.optLong("createdAt", System.currentTimeMillis())
      )
    }
    val debts = root.optJSONArray("debts").objects().map { o ->
      Debt(
        id = o.getLong("id"), customerId = o.getLong("customerId"), amount = o.getDouble("amount"),
        date = o.getLong("date"), dueDate = o.optLongOrNull("dueDate"), description = o.optString("description"),
        category = o.optString("category", "Genel Satış"), createdAt = o.optLong("createdAt"),
        updatedAt = o.optLong("updatedAt"), isDeleted = o.optBoolean("isDeleted")
      )
    }
    val collections = root.optJSONArray("collections").objects().map { o ->
      CollectionRecord(
        id = o.getLong("id"), customerId = o.getLong("customerId"), debtId = o.optLongOrNull("debtId"),
        amount = o.getDouble("amount"), date = o.getLong("date"),
        paymentMethod = runCatching { PaymentMethod.valueOf(o.getString("paymentMethod")) }.getOrDefault(PaymentMethod.OTHER),
        notes = o.optString("notes"), createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
        isDeleted = o.optBoolean("isDeleted")
      )
    }
    val templates = root.optJSONArray("templates").objects().map { o ->
      WhatsAppTemplate(
        id = o.getLong("id"), key = o.optString("key"), title = o.optString("title"),
        templateText = o.optString("templateText"), isCustom = o.optBoolean("isCustom")
      )
    }
    val profile = root.optJSONObject("profile")?.let { p ->
      UserBusinessProfile(
        businessName = p.optString("businessName"), ownerName = p.optString("ownerName"),
        phone = p.optString("phone"), city = p.optString("city"),
        reminderDaysBefore = p.optInt("reminderDaysBefore", 1),
        themeMode = runCatching { AppThemeMode.valueOf(p.optString("themeMode")) }.getOrDefault(AppThemeMode.DEFTER)
      )
    }
    return BackupSnapshot(customers, debts, collections, templates, profile, root.optLong("createdAt"))
  }

  private fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optJSONObject(it) }
  }
}
