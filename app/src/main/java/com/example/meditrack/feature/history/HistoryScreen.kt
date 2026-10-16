@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.data.DoseStatus
import com.example.meditrack.core.data.HistoryItem
import com.example.meditrack.core.data.StockOutageHistoryItem
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.softScrollEdges
import com.example.meditrack.core.ui.components.EmptyState
import com.example.meditrack.core.ui.components.MedicationAvatar
import com.example.meditrack.core.ui.components.StatusPill
import com.example.meditrack.core.ui.components.CategoryDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MediTrackViewModel, embedded: Boolean = false) {
    val history by viewModel.history.collectAsState()
    val outages by viewModel.outageHistory.collectAsState()
    var filter by remember { mutableStateOf("ALL") }
    val supportedHistory = history.filter { it.log.status != DoseStatus.SKIPPED.name }
    val visible = when (filter) {
        "TAKEN" -> supportedHistory.filter { it.log.status == DoseStatus.TAKEN.name }
        "POSTPONED" -> supportedHistory.filter { it.log.status == DoseStatus.POSTPONED.name }
        "MISSED" -> supportedHistory.filter { it.log.status == DoseStatus.MISSED.name }
        "OUTAGES" -> emptyList()
        else -> supportedHistory
    }
    val visibleOutages = if (filter == "ALL" || filter == "OUTAGES") outages else emptyList()
    val missedCount = supportedHistory.count { it.log.status == DoseStatus.MISSED.name }

    LazyColumn(
            modifier = Modifier.fillMaxSize().softScrollEdges(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!embedded) {
                item { Text("Historial", style = MaterialTheme.typography.headlineMedium) }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Tomas no realizadas", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$missedCount ${if (missedCount == 1) "toma no fue realizada" else "tomas no fueron realizadas"}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("Todas") })
                    FilterChip(selected = filter == "TAKEN", onClick = { filter = "TAKEN" }, label = { Text("Tomadas") })
                    FilterChip(selected = filter == "POSTPONED", onClick = { filter = "POSTPONED" }, label = { Text("Pospuestas") })
                    FilterChip(selected = filter == "MISSED", onClick = { filter = "MISSED" }, label = { Text("No tomadas") })
                    FilterChip(selected = filter == "OUTAGES", onClick = { filter = "OUTAGES" }, label = { Text("Sin stock") })
                }
            }
            if (visible.isEmpty() && visibleOutages.isEmpty()) {
                item { EmptyState("Sin registros", "Cuando confirmes, omitas o pospongas una toma aparecerá aquí.") }
            } else {
                if (visible.isNotEmpty()) item { CategoryDivider("Registro de tomas") }
                items(visible, key = { it.log.id }) { HistoryCard(it) }
                if (visibleOutages.isNotEmpty()) item { CategoryDivider("Períodos sin existencias") }
                items(visibleOutages, key = { "outage-${it.outage.id}" }) { StockOutageCard(it) }
            }
    }
}

@Composable
private fun StockOutageCard(item: StockOutageHistoryItem) {
    val formatter = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "NI"))
    val end = item.outage.endedAt
    val elapsed = (end ?: System.currentTimeMillis()) - item.outage.startedAt
    val duration = when {
        elapsed < 60 * 60_000L -> "${(elapsed / 60_000L).coerceAtLeast(1)} min"
        elapsed < 24 * 60 * 60_000L -> "${elapsed / (60 * 60_000L)} h"
        else -> "${elapsed / (24 * 60 * 60_000L)} días"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(item.medicationName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Inicio: ${formatter.format(Date(item.outage.startedAt))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    end?.let { "Fin: ${formatter.format(Date(it))}" } ?: "Fin: pendiente de reposición",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Tiempo sin stock: $duration", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem) {
    val status = runCatching { DoseStatus.valueOf(item.log.status) }.getOrDefault(DoseStatus.PENDING)
    val date = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es", "NI")).format(Date(item.log.scheduledAt))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MedicationAvatar(item.medicationName.hashCode())
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(item.medicationName, style = MaterialTheme.typography.titleMedium)
                Text("${item.dose} · $date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(status)
        }
    }
}
