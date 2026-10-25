package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whatsapp_templates")
data class WhatsAppTemplate(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val key: String,
  val title: String,
  val templateText: String,
  val isCustom: Boolean = false
)

object DefaultTemplates {
  fun getDefaults(): List<WhatsAppTemplate> = listOf(
    WhatsAppTemplate(
      key = "PAYMENT_REMINDER",
      title = "Ödeme Hatırlatması",
      templateText = "Sayın {musteri_adi}, {isletme_adi} nezdindeki cari hesabınızda {kalan_bakiye} tutarında gecikmiş/vadesi gelmiş bakiyeniz bulunmaktadır. Ödemenizi en kısa sürede rica eder, iyi çalışmalar dileriz."
    ),
    WhatsAppTemplate(
      key = "UPCOMING_DUE",
      title = "Vadesi Yaklaşan Ödeme",
      templateText = "Merhaba Sayın {musteri_adi}, {vade_tarihi} vadeli {kalan_bakiye} tutarındaki ödemenizi hatırlatmak isteriz. Saygılarımızla, {isletme_adi}."
    ),
    WhatsAppTemplate(
      key = "PAYMENT_RECEIVED",
      title = "Ödeme Alındı Bildirimi",
      templateText = "Sayın {musteri_adi}, {son_tutar} tutarındaki tahsilatınız hesabınıza işlenmiştir. Kalan güncel bakiyeniz: {kalan_bakiye}. Teşekkür eder, bereketli günler dileriz. - {isletme_adi}"
    ),
    WhatsAppTemplate(
      key = "THANK_YOU",
      title = "Teşekkür Mesajı",
      templateText = "Sayın {musteri_adi}, ödemeniz için teşekkür ederiz. Hesabınızda herhangi bir borç bakiyesi bulunmamaktadır. İş birliğiniz için minnettarız. {isletme_adi}"
    ),
    WhatsAppTemplate(
      key = "GENERAL_INFO",
      title = "Genel Bakiye Bilgilendirmesi",
      templateText = "Sayın {musteri_adi}, {isletme_adi} cari hesap ekstreniz: Toplam İşlem: {toplam_borc}, Yapılan Ödeme: {toplam_tahsilat}, Güncel Kalan Bakiye: {kalan_bakiye}. Bilgilerinize sunarız."
    )
  )
}
