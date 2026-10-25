package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate500
import com.example.ui.theme.moneyStyle
import com.example.util.CsvHelper
import com.example.util.FileShare
import com.example.util.Formatters
import com.example.viewmodel.MusteriCepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndExportScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val summary by viewModel.dashboardSummary.collectAsState()
  val customers by viewModel.allCustomersWithBalance.collectAsState()
  val palette = LocalAppPalette.current
  val realCount = customers.count { !it.customer.isDemo }

  var importCsvText by remember { mutableStateOf("") }
  var showPaste by remember { mutableStateOf(false) }
  val today = Formatters.formatDate(System.currentTimeMillis()).replace('.', '-')

  val csvOpener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      try {
        viewModel.parseAndPreviewCsv(FileShare.readFromUri(context, uri, maxBytes = 5 * 1024 * 1024))
      } catch (e: Exception) {
        viewModel.showMessage("Dosya okunamadı: ${e.message}")
      }
    }
  }

  BackHandler { viewModel.navigateBack() }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Raporlar ve Excel", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
      // 1. Özet
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.cardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PieChart, contentDescription = null, tint = NavyPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Genel durum", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            ReportRow("Toplam alacak", Formatters.formatCurrency(summary.totalReceivable), RoseError)
            ReportDivider()
            ReportRow("Vadesi geçen alacak", Formatters.formatCurrency(summary.overdueAmount), RoseError)
            ReportDivider()
            ReportRow("7 gün içinde vadesi gelen", Formatters.formatCurrency(summary.upcomingAmount), MaterialTheme.colorScheme.onSurface)
            ReportDivider()
            ReportRow("Bu ay tahsilat", Formatters.formatCurrency(summary.thisMonthCollected), EmeraldPrimary)
            ReportDivider()
            ReportRow("Geçen ay tahsilat", Formatters.formatCurrency(summary.lastMonthCollected), EmeraldPrimary)
            ReportDivider()
            ReportRow("Toplam tahsilat (tüm zamanlar)", Formatters.formatCurrency(summary.totalCollected), EmeraldPrimary)
            ReportDivider()
            ReportRow("Aktif müşteri / borçlu", "${summary.activeCustomerCount} / ${summary.debtorCount}", MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      // 2. Dışa aktarma
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.cardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Excel'e aktar (CSV)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Dosya Excel'de doğrudan açılır; Türkçe karakterler ve tutarlar doğru görünür. Örnek veriler dosyaya eklenmez.",
              style = MaterialTheme.typography.bodySmall,
              color = Slate500
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = {
                if (realCount == 0) {
                  viewModel.showMessage("Dışa aktarılacak müşteri yok.")
                } else {
                  FileShare.shareText(context, "MusteriCep_Musteriler_$today.csv", "text/csv", viewModel.exportCustomersCsv(), "Müşteri listesini gönder")
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_export_customers_csv")
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Müşteri bakiyeleri ($realCount)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = {
                if (realCount == 0) {
                  viewModel.showMessage("Dışa aktarılacak kayıt yok.")
                } else {
                  FileShare.shareText(context, "MusteriCep_Hareketler_$today.csv", "text/csv", viewModel.exportTransactionsCsv(), "Hareket dökümünü gönder")
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Tüm borç ve tahsilat hareketleri")
            }
          }
        }
      }

      // 3. İçe aktarma
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.cardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.FileUpload, contentDescription = null, tint = NavyPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Excel'den müşteri ekle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Sütun sırası: Ad Soyad; Telefon; E-posta; Adres; Not. Excel'de \"Farklı kaydet → CSV (noktalı virgülle ayrılmış)\" seçin. " +
                "Önce önizleme gösterilir; hatalı ve zaten kayıtlı satırlar atlanır.",
              style = MaterialTheme.typography.bodySmall,
              color = Slate500
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { csvOpener.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*")) },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_pick_csv")
            ) {
              Text("CSV dosyası seç")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              TextButton(onClick = {
                FileShare.shareText(context, "MusteriCep_Ornek_Sablon.csv", "text/csv", CsvHelper.sampleTemplate(), "Örnek şablonu gönder")
              }) { Text("Örnek şablon", fontSize = 13.sp) }
              TextButton(onClick = { showPaste = !showPaste }) {
                Text(if (showPaste) "Yapıştırmayı kapat" else "Metin yapıştır", fontSize = 13.sp)
              }
            }
            if (showPaste) {
              OutlinedTextField(
                value = importCsvText,
                onValueChange = { importCsvText = it },
                placeholder = { Text("Ali Koç;05321112233;ali@mail.com;Kadıköy;Cuma öder") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_csv_paste")
              )
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  if (importCsvText.isNotBlank()) viewModel.parseAndPreviewCsv(importCsvText)
                  else viewModel.showMessage("Önce CSV metnini yapıştırın.")
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_preview_csv")
              ) {
                Text("Önizle")
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(40.dp)) }
    }
  }
}

@Composable
private fun ReportDivider() {
  HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LocalAppPalette.current.cardBorder)
}

@Composable
private fun ReportRow(title: String, value: String, valueColor: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(title, style = MaterialTheme.typography.bodyMedium, color = Slate500, modifier = Modifier.weight(1f))
    Text(value, style = moneyStyle(15), color = valueColor)
  }
}
