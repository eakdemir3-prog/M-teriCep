package com.example.ui.screens

import com.example.model.allows
import androidx.compose.material.icons.filled.Lock

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppCard
import com.example.ui.components.BarChart
import com.example.ui.components.IconBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.moneyStyle
import com.example.util.CsvHelper
import com.example.util.FileShare
import com.example.util.Formatters
import com.example.util.Money
import com.example.viewmodel.MusteriCepViewModel
import java.util.Calendar

/** Rapor dönemi. */
private enum class ReportRange(val label: String) {
  TODAY("Bugün"),
  WEEK("Bu hafta"),
  MONTH("Bu ay"),
  LAST_MONTH("Geçen ay"),
  QUARTER("Son 3 ay"),
  YEAR("Bu yıl"),
  ALL("Tümü")
}

private fun rangeBounds(r: ReportRange, now: Long): Pair<Long, Long> {
  val todayStart = Formatters.startOfDay(now)
  val tomorrow = Calendar.getInstance().apply { timeInMillis = todayStart; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
  return when (r) {
    ReportRange.TODAY -> todayStart to tomorrow
    ReportRange.WEEK -> Formatters.startOfWeek(now) to tomorrow
    ReportRange.MONTH -> Formatters.startOfMonth(now) to tomorrow
    ReportRange.LAST_MONTH -> {
      val thisMonth = Formatters.startOfMonth(now)
      Formatters.startOfMonth(thisMonth - 1) to thisMonth
    }
    ReportRange.QUARTER -> Calendar.getInstance().apply { timeInMillis = Formatters.startOfMonth(now); add(Calendar.MONTH, -2) }.timeInMillis to tomorrow
    ReportRange.YEAR -> Calendar.getInstance().apply { timeInMillis = todayStart; set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis to tomorrow
    ReportRange.ALL -> Long.MIN_VALUE to Long.MAX_VALUE
  }
}

private data class HistoryRow(val key: String, val name: String, val isCollection: Boolean, val label: String, val date: Long, val amount: Double)
private data class CustomerReport(val name: String, val collected: Double, val newDebt: Double, val balance: Double, val overdue: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndExportScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val summary by viewModel.dashboardSummary.collectAsState()
  val ledger by viewModel.ledger.collectAsState()
  val palette = LocalAppPalette.current
  var range by rememberSaveable { mutableStateOf(ReportRange.MONTH) }
  val profileTier = viewModel.profile.collectAsState().value.activeTier
  var importCsvText by remember { mutableStateOf("") }
  var showPaste by remember { mutableStateOf(false) }
  var showAllHistory by remember { mutableStateOf(false) }
  val now = System.currentTimeMillis()
  val today = Formatters.formatDate(now).replace('.', '-')
  val realCount = ledger.customers.count { !it.customer.isDemo }

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

  // Seçili döneme göre gerçek verilerden hesaplanan rapor
  val (start, end) = rangeBounds(range, now)
  val names = remember(ledger) { ledger.customers.associate { it.customer.id to it.customer.name } }
  val collIn = ledger.collections.filter { it.date in start until end }
  val debtIn = ledger.debtStatuses.filter { it.debt.date in start until end }
  val collectedTotal = Money.sum(collIn.map { it.amount })
  val debtTotal = Money.sum(debtIn.map { it.debt.amount })

  val chart = remember(ledger, range) {
    val (s, e) = rangeBounds(range, now)
    buildRangeChart(ledger.collections.map { it.date to it.amount }, s, e, now, ledger.collections.minOfOrNull { it.date })
  }

  val customerReports = remember(ledger, range) {
    val (s, e) = rangeBounds(range, now)
    ledger.customers.map { c ->
      CustomerReport(
        name = c.customer.name,
        collected = Money.sum(ledger.collections.filter { it.customerId == c.customer.id && it.date in s until e }.map { it.amount }),
        newDebt = Money.sum(ledger.debtStatuses.filter { it.debt.customerId == c.customer.id && it.debt.date in s until e }.map { it.debt.amount }),
        balance = c.remainingBalance,
        overdue = c.overdueAmount
      )
    }.filter { it.collected > 0 || it.newDebt > 0 || it.balance > 0 }
      .sortedWith(compareByDescending<CustomerReport> { it.balance }.thenByDescending { it.collected })
  }

  val history = remember(ledger, range) {
    val (s, e) = rangeBounds(range, now)
    val rows = mutableListOf<HistoryRow>()
    ledger.collections.filter { it.date in s until e }.forEach {
      rows += HistoryRow("c${it.id}", names[it.customerId] ?: "Bilinmeyen", true, it.paymentMethod.label, it.date, it.amount)
    }
    ledger.debtStatuses.filter { it.debt.date in s until e }.forEach {
      rows += HistoryRow("d${it.debt.id}", names[it.debt.customerId] ?: "Bilinmeyen", false, it.debt.category, it.debt.date, it.debt.amount)
    }
    rows.sortedByDescending { it.date }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("Raporlar", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Dönem özeti (sabit)
      item {
        AppCard {
          Text("Genel durum", style = MaterialTheme.typography.titleSmall)
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth()) {
            Stat("Toplam alacak", Formatters.formatCurrencyShort(summary.totalReceivable), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            Stat("Gecikmiş", Formatters.formatCurrencyShort(summary.overdueAmount), if (summary.overdueAmount > 0) palette.danger else palette.muted, Modifier.weight(1f))
          }
          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = palette.cardBorder)
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth()) {
            Stat("Bugün", Formatters.formatCurrencyShort(summary.todayCollected), palette.success, Modifier.weight(1f))
            Stat("Bu hafta", Formatters.formatCurrencyShort(summary.thisWeekCollected), palette.success, Modifier.weight(1f))
            Stat("Bu ay", Formatters.formatCurrencyShort(summary.thisMonthCollected), palette.success, Modifier.weight(1f))
          }
        }
      }

      // Tarih filtresi
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(ReportRange.entries.toList()) { r ->
            FilterChip(
              selected = range == r,
              onClick = {
                val free = r == ReportRange.TODAY || r == ReportRange.WEEK || r == ReportRange.MONTH
                if (free || viewModel.requireFeature(com.example.model.Feature.ALL_REPORTS)) { range = r; showAllHistory = false }
              },
              label = {
                val locked = !(r == ReportRange.TODAY || r == ReportRange.WEEK || r == ReportRange.MONTH) &&
                  !profileTier.allows(com.example.model.Feature.ALL_REPORTS)
                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (locked) {
                    Icon(Icons.Default.Lock, contentDescription = "Paket gerekli", modifier = Modifier.size(14.dp), tint = palette.muted)
                    Spacer(modifier = Modifier.width(4.dp))
                  }
                  Text(r.label, maxLines = 1, softWrap = false)
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.accentSoft,
                selectedLabelColor = palette.ink,
                containerColor = MaterialTheme.colorScheme.surface
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true, selected = range == r,
                borderColor = palette.cardBorder, selectedBorderColor = palette.ink.copy(alpha = 0.4f)
              )
            )
          }
        }
      }

      // Dönem raporu + grafik
      item {
        AppCard {
          Text(rangeTitle(range, start, end), style = MaterialTheme.typography.labelLarge, color = palette.muted)
          Spacer(modifier = Modifier.height(8.dp))
          Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Tahsil edilen", fontSize = 12.sp, color = palette.muted)
              Text(Formatters.formatCurrency(collectedTotal), style = moneyStyle(20, FontWeight.Bold), color = palette.success, maxLines = 1)
              Text("${collIn.size} tahsilat", fontSize = 12.sp, color = palette.muted)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
              Text("Yeni borç", fontSize = 12.sp, color = palette.muted)
              Text(Formatters.formatCurrency(debtTotal), style = moneyStyle(20, FontWeight.Bold), maxLines = 1)
              Text("${debtIn.size} kayıt", fontSize = 12.sp, color = palette.muted)
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          if (chart.values.all { it <= 0.0 }) {
            Text("Bu dönemde tahsilat yok.", style = MaterialTheme.typography.bodySmall, color = palette.muted, modifier = Modifier.padding(vertical = 12.dp))
          } else {
            BarChart(values = chart.values, labels = chart.labels, highlightIndex = chart.highlight, height = 110.dp)
          }
        }
      }

      // Müşteri bazlı rapor
      item { SectionHeader("Müşteri bazlı") }
      if (customerReports.isEmpty()) {
        item { Text("Bu dönemde hareketi olan müşteri yok.", style = MaterialTheme.typography.bodySmall, color = palette.muted) }
      } else {
        item {
          AppCard(padding = 0.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
              Text("Müşteri", fontSize = 11.sp, color = palette.muted, modifier = Modifier.weight(1.4f))
              Text("Tahsil", fontSize = 11.sp, color = palette.muted, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
              Text("Bakiye", fontSize = 11.sp, color = palette.muted, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            customerReports.take(10).forEach { r ->
              HorizontalDivider(color = palette.cardBorder)
              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.4f)) {
                  Text(r.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  if (r.overdue > 0) Text("Gecikmiş ${Formatters.formatCurrencyShort(r.overdue)}", fontSize = 11.sp, color = palette.danger, maxLines = 1)
                }
                Text(Formatters.formatCurrencyShort(r.collected), style = moneyStyle(13), color = if (r.collected > 0) palette.success else palette.muted, maxLines = 1,
                  modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text(Formatters.formatCurrencyShort(r.balance), style = moneyStyle(13), maxLines = 1,
                  modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
              }
            }
          }
        }
      }

      // İşlem geçmişi
      item { SectionHeader("İşlem geçmişi (${history.size})") }
      if (history.isEmpty()) {
        item { Text("Bu dönemde işlem yok.", style = MaterialTheme.typography.bodySmall, color = palette.muted) }
      } else {
        item {
          AppCard(padding = 0.dp) {
            val shown = if (showAllHistory) history else history.take(10)
            shown.forEachIndexed { i, h ->
              if (i > 0) HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(start = 64.dp))
              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                  if (h.isCollection) Icons.Default.SouthWest else Icons.Default.NorthEast,
                  if (h.isCollection) palette.success else palette.danger,
                  if (h.isCollection) palette.accentSoft else palette.dangerSoft,
                  size = 36.dp, iconSize = 18.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(h.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text("${if (h.isCollection) "Tahsilat" else "Borç"} • ${h.label} • ${Formatters.formatDate(h.date)}", fontSize = 12.sp, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  (if (h.isCollection) "+" else "") + Formatters.formatCurrency(h.amount),
                  style = moneyStyle(14),
                  color = if (h.isCollection) palette.success else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1
                )
              }
            }
            if (history.size > 10) {
              HorizontalDivider(color = palette.cardBorder)
              TextButton(onClick = { showAllHistory = !showAllHistory }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showAllHistory) "Daha az göster" else "Tümünü göster (${history.size})", color = palette.ink)
              }
            }
          }
        }
      }

      // Excel'e aktar
      item { SectionHeader("Excel (CSV)") }
      item {
        AppCard {
          Text(
            "Dosya Excel'de doğrudan açılır; Türkçe karakterler ve tutarlar doğru görünür. Örnek veriler dosyaya eklenmez.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted
          )
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = {
              if (!viewModel.requireFeature(com.example.model.Feature.CSV_EXPORT)) Unit
              else if (realCount == 0) viewModel.showMessage("Dışa aktarılacak müşteri yok.")
              else FileShare.shareText(context, "MusteriCep_Musteriler_$today.csv", "text/csv", viewModel.exportCustomersCsv(), "Müşteri listesini gönder")
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.onInk),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_export_customers_csv")
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Müşteri bakiyeleri ($realCount)", maxLines = 1)
          }
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = {
              if (!viewModel.requireFeature(com.example.model.Feature.CSV_EXPORT)) Unit
              else if (realCount == 0) viewModel.showMessage("Dışa aktarılacak kayıt yok.")
              else FileShare.shareText(context, "MusteriCep_Hareketler_$today.csv", "text/csv", viewModel.exportTransactionsCsv(), "Hareket dökümünü gönder")
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tüm hareketler", maxLines = 1)
          }
        }
      }

      // Excel'den müşteri ekle
      item {
        AppCard {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FileUpload, contentDescription = null, tint = palette.ink)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Excel'den müşteri ekle", style = MaterialTheme.typography.titleSmall)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Sütun sırası: Ad Soyad; Telefon; E-posta; Adres; Not. Önce önizleme gösterilir; hatalı ve zaten kayıtlı satırlar atlanır.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = { if (viewModel.requireFeature(com.example.model.Feature.CSV_IMPORT)) csvOpener.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*")) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_pick_csv")
          ) { Text("CSV dosyası seç") }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = {
              FileShare.shareText(context, "MusteriCep_Ornek_Sablon.csv", "text/csv", CsvHelper.sampleTemplate(), "Örnek şablonu gönder")
            }) { Text("Örnek şablon", color = palette.ink) }
            TextButton(onClick = { showPaste = !showPaste }) {
              Text(if (showPaste) "Kapat" else "Metin yapıştır", color = palette.ink)
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
                if (!viewModel.requireFeature(com.example.model.Feature.CSV_IMPORT)) Unit
                else if (importCsvText.isNotBlank()) viewModel.parseAndPreviewCsv(importCsvText)
                else viewModel.showMessage("Önce CSV metnini yapıştırın.")
              },
              colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.onInk),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_preview_csv")
            ) { Text("Önizle") }
          }
        }
      }
    }
  }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
  Column(modifier = modifier) {
    Text(label, fontSize = 12.sp, color = LocalAppPalette.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(value, style = moneyStyle(15), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

private fun rangeTitle(r: ReportRange, start: Long, end: Long): String = when (r) {
  ReportRange.ALL -> "Tüm zamanlar"
  ReportRange.TODAY -> "Bugün • ${Formatters.formatDate(start)}"
  else -> "${r.label} • ${Formatters.formatShortDate(start)} – ${Formatters.formatShortDate(end - 1)}"
}

private data class RangeChart(val values: List<Double>, val labels: List<String>, val highlight: Int)

/** Kısa dönemlerde gün gün, uzun dönemlerde ay ay tahsilat. */
private fun buildRangeChart(items: List<Pair<Long, Double>>, startIn: Long, endIn: Long, now: Long, firstDate: Long?): RangeChart {
  val start = if (startIn == Long.MIN_VALUE) Formatters.startOfMonth(firstDate ?: now) else startIn
  val end = if (endIn == Long.MAX_VALUE) now + 1 else minOf(endIn, now + 86_400_000L)
  val days = ((end - start) / 86_400_000L).toInt().coerceAtLeast(1)
  val months = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
  return if (days <= 1) {
    // Bugün: 4 saatlik dilimler
    val labels = listOf("00", "04", "08", "12", "16", "20")
    val values = (0 until 6).map { i ->
      val s = start + i * 4 * 3_600_000L
      Money.sum(items.filter { it.first in s until s + 4 * 3_600_000L }.map { it.second })
    }
    RangeChart(values, labels, ((now - start) / (4 * 3_600_000L)).toInt().coerceIn(0, 5))
  } else if (days <= 31) {
    val values = ArrayList<Double>()
    val labels = ArrayList<String>()
    var cur = start
    var idx = 0
    var hi = -1
    while (cur < end) {
      val next = Calendar.getInstance().apply { timeInMillis = cur; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
      values += Money.sum(items.filter { it.first in cur until next }.map { it.second })
      val d = Calendar.getInstance().apply { timeInMillis = cur }.get(Calendar.DAY_OF_MONTH)
      // Çok gün varsa etiketleri seyrelt
      labels += if (days <= 10 || d == 1 || d % 5 == 0) d.toString() else ""
      if (now in cur until next) hi = idx
      cur = next; idx++
    }
    RangeChart(values, labels, hi)
  } else {
    val values = ArrayList<Double>()
    val labels = ArrayList<String>()
    var cur = Formatters.startOfMonth(start)
    var idx = 0
    var hi = -1
    while (cur < end && idx < 24) {
      val next = Calendar.getInstance().apply { timeInMillis = cur; add(Calendar.MONTH, 1) }.timeInMillis
      values += Money.sum(items.filter { it.first in cur until next }.map { it.second })
      labels += months[Calendar.getInstance().apply { timeInMillis = cur }.get(Calendar.MONTH)]
      if (now in cur until next) hi = idx
      cur = next; idx++
    }
    // En fazla son 12 ay gösterilir
    val drop = (values.size - 12).coerceAtLeast(0)
    RangeChart(values.drop(drop), labels.drop(drop), if (hi >= 0) hi - drop else -1)
  }
}
