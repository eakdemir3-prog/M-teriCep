package com.example

import com.example.model.allows

import com.example.model.Customer
import com.example.model.CustomerWithBalance
import com.example.model.SubscriptionTier
import com.example.util.CsvHelper
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testCurrencyFormatting() {
    val formatted = Formatters.formatCurrency(1250.50)
    assertTrue(formatted.contains("1.250,50") || formatted.contains("1250,50"))
    assertTrue(formatted.contains("₺"))
  }

  @Test
  fun testPhoneCleaning() {
    assertEquals("905321234567", Formatters.cleanPhoneNumber("0532 123 45 67"))
    assertEquals("905321234567", Formatters.cleanPhoneNumber("5321234567"))
    assertEquals("905321234567", Formatters.cleanPhoneNumber("+90 532 123 45 67"))
  }

  @Test
  fun testWhatsAppTemplateInterpolation() {
    val template = "Sayın {musteri_adi}, {isletme_adi} nezdinde kalan borcunuz: {kalan_bakiye}."
    val built = WhatsAppHelper.buildMessage(
      templateText = template,
      customerName = "Ahmet Yılmaz",
      businessName = "Bereket Market",
      totalDebt = 1000.0,
      totalPaid = 300.0,
      remainingBalance = 700.0,
      dueDate = null
    )
    assertTrue(built.contains("Ahmet Yılmaz"))
    assertTrue(built.contains("Bereket Market"))
    assertTrue(built.contains("700"))
  }

  @Test
  fun testCsvParsingAndValidation() {
    val validCsv = """
      Ad Soyad;Telefon;E-posta;Adres;Notlar
      Mehmet Demir;05431112233;demir@oto.com;Sanayi Sit;Haftalık ödeme
      Hatalı Satır;123;;;
    """.trimIndent()

    val result = CsvHelper.parseCustomerCsv(validCsv)
    assertEquals(1, result.validCustomers.size)
    assertEquals("Mehmet Demir", result.validCustomers[0].name)
    assertEquals("905431112233", result.validCustomers[0].phone)
    assertEquals(1, result.errors.size)
    assertTrue(result.errors[0].contains("geçersiz"))
  }

  @Test
  fun testSubscriptionTierLimits() {
    assertEquals(25, SubscriptionTier.FREE.maxCustomerLimit)
    assertEquals(150, SubscriptionTier.STARTER.maxCustomerLimit)
    assertEquals(com.example.model.UNLIMITED, SubscriptionTier.PRO.maxCustomerLimit)
    assertFalse(SubscriptionTier.FREE.allows(com.example.model.Feature.CSV_EXPORT))
    assertTrue(SubscriptionTier.STARTER.allows(com.example.model.Feature.CSV_EXPORT))
    assertFalse(SubscriptionTier.STARTER.allows(com.example.model.Feature.CSV_IMPORT))
    assertTrue(SubscriptionTier.PRO.allows(com.example.model.Feature.CSV_IMPORT))
    assertEquals(SubscriptionTier.PRO, SubscriptionTier.fromProductId("mustericep_pro"))
  }
}
