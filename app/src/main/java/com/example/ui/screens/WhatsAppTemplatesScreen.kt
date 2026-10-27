package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.Slate500
import com.example.viewmodel.DialogState
import com.example.viewmodel.MusteriCepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppTemplatesScreen(
  viewModel: MusteriCepViewModel,
  modifier: Modifier = Modifier
) {
  val templates by viewModel.allTemplates.collectAsState(initial = emptyList())

  BackHandler {
    viewModel.navigateBack()
  }

  Scaffold(
    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("Mesaj şablonları", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
          }
        },
        actions = {
          TextButton(onClick = {
            viewModel.openDialog(
              com.example.viewmodel.DialogState.ConfirmAction(
                title = "Varsayılana döndür",
                message = "Tüm şablonlar ilk hâline dönecek; yaptığınız değişiklikler silinecek.",
                confirmLabel = "Döndür",
                onConfirm = { viewModel.resetTemplates() }
              )
            )
          }) { Text("Sıfırla") }
        },
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
      )
    }
  ) { innerPadding ->
    val palette = com.example.ui.theme.LocalAppPalette.current
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        com.example.ui.components.AppCard(padding = 14.dp) {
          Row(verticalAlignment = Alignment.Top) {
            com.example.ui.components.IconBadge(
              androidx.compose.material.icons.Icons.AutoMirrored.Filled.Chat,
              com.example.ui.theme.WhatsAppGreen,
              palette.accentSoft,
              size = 36.dp,
              iconSize = 18.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Akıllı alanlar", style = MaterialTheme.typography.titleSmall)
              Text(
                "{musteri_adi}, {kalan_bakiye}, {vade_tarihi} gibi alanlar gönderirken müşterinin güncel bilgileriyle doldurulur. Mesaj otomatik gitmez; WhatsApp'ta siz gönderirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
              )
            }
          }
        }
      }

      items(templates, key = { it.id }) { template ->
        com.example.ui.components.AppCard(padding = 0.dp) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = template.title,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.TextButton(onClick = { viewModel.openDialog(DialogState.EditTemplate(template)) }) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.ink)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Düzenle", color = palette.ink, maxLines = 1, softWrap = false)
            }
          }
          Text(
            text = template.templateText,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            maxLines = 5,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
          )
        }
      }
    }
  }
}
