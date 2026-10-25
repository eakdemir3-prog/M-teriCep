package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.util.CsvHelper
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import com.example.viewmodel.MusteriCepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndExportScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val summary by viewModel.dashboardSummary.collectAsState()
  val customers by viewModel.allCustomersWithBalance.collectAsState(initial = emptyList())
  val debts by viewModel.allDebts.collectAsState(initial = emptyList())
  val collections by viewModel.allCollections.collectAsState(initial = emptyList())

  var importCsvText by remember { mutableStateOf("") }

  BackHandler {
    viewModel.navigateBack()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Raporlar & Excel Dışa Aktar", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Genel Finansal Durum Rapor Kartı
      item {
        Spacer(modifier = Modifier.height(4.dp))
        ElevatedCard(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PieChart, contentDescription = null, tint = NavyPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Finansal Bilanço Özeti", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            ReportRow("Toplam Alacak Tutarı", Formatters.formatCurrency(summary.totalReceivable), RoseError)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ReportRow("Toplam Yapılan Tahsilat", Formatters.formatCurrency(summary.totalCollected), EmeraldPrimary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ReportRow("Gecikmiş Vadesi Geçen Alacak", Formatters.formatCurrency(summary.overdueAmount), RoseError)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ReportRow("Aktif Kayıtlı Müşteri Sayısı", "${summary.activeCustomerCount}", MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      // 2. Excel (CSV) Dışa Aktarma
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Excel (CSV) Dışa Aktar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Verilerinizi Türkçe karakter destekli (UTF-8 BOM) ve Excel uyumlu noktalı virgüllü CSV dosyası olarak dışa aktarın.",
              style = MaterialTheme.typography.bodySmall,
              color = Slate500
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Export Customers CSV
            Button(
              onClick = {
                val csv = CsvHelper.exportCustomersToCsv(customers)
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, csv)
                  putExtra(Intent.EXTRA_TITLE, "MusteriCep_Musteri_Listesi.csv")
                  type = "text/csv"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Müşteri Listesi CSV Paylaş"))
              },
              colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_export_customers_csv")
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Müşteri Bakiyelerini Dışa Aktar (${customers.size} Müşteri)")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Export All Transactions CSV
            OutlinedButton(
              onClick = {
                val customerMap = customers.associate { it.customer.id to it.customer }
                val csv = CsvHelper.exportAllTransactionsToCsv(customerMap, debts, collections)
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, csv)
                  putExtra(Intent.EXTRA_TITLE, "MusteriCep_Tum_Finansal_Hareketler.csv")
                  type = "text/csv"
                }
                context.startActivity(Intent.createChooser(sendIntent, "İşlem Geçmişi CSV Paylaş"))
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Tüm Borç ve Tahsilat Geçmişini Dışa Aktar")
            }
          }
        }
      }

      // 3. CSV Dosyasından Müşteri İçe Aktarma (Önizlemeli & Hata Bildirimli)
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.FileUpload, contentDescription = null, tint = NavyPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("CSV ile Müşteri İçe Aktar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Format: Ad Soyad;Telefon;E-posta;Adres;Notlar (Her satıra bir müşteri gelecek şekilde metni yapıştırın).",
              style = MaterialTheme.typography.bodySmall,
              color = Slate500
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Paste input
            OutlinedTextField(
              value = importCsvText,
              onValueChange = { importCsvText = it },
              placeholder = {
                Text(
                  "Örnek:\nAli Koç;05321112233;ali@mail.com;Kadıköy;Düzenli alıcı\nFatma Şen;05423334455;;Beşiktaş;"
                )
              },
              maxLines = 5,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_csv_text")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              TextButton(
                onClick = {
                  importCsvText = "Ad Soyad;Telefon;E-posta;Adres;Notlar\nKemal Sunal (Güneş Ticaret);05329998877;kemal@gunes.com;Fatih/İstanbul;Toptan gıda\nZeynep Yılmaz;05448887766;;Şişli;Kuaför malzemesi\nHatalı Satır Deneme;123;;;"
                }
              ) {
                Text("Örnek Veri Doldur", fontSize = 12.sp)
              }

              Button(
                onClick = {
                  if (importCsvText.isNotBlank()) {
                    viewModel.parseAndPreviewCsv(importCsvText)
                  } else {
                    viewModel.showMessage("Lütfen CSV metnini yapıştırınız.")
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("btn_preview_csv")
              ) {
                Text("Önizle & Doğrula")
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }
}

@Composable
private fun ReportRow(title: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(title, style = MaterialTheme.typography.bodyMedium, color = Slate500)
    Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = valueColor)
  }
}
