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
import com.example.meditrack.core.data.MedicationRepository
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.softScrollEdges
import com.example.meditrack.core.ui.components.EmptyState
import com.example.meditrack.core.ui.components.MedicationAvatar
import com.example.meditrack.core.ui.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MediTrackViewModel, embedded: Boolean = false) {
    val history by viewModel.history.collectAsState()
    var filter by remember { mutableStateOf<DoseStatus?>(null) }
    val supportedHistory = history.filter { it.log.status != DoseStatus.SKIPPED.name }
    val visible = filter?.let { status -> supportedHistory.filter { it.log.status == status.name } } ?: supportedHistory

    LazyColumn(
            modifier = Modifier.fillMaxSize().softScrollEdges(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!embedded) {
                item { Text("Historial", style = MaterialTheme.typography.headlineMedium) }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todas") })
                    listOf(DoseStatus.TAKEN, DoseStatus.POSTPONED).forEach { status ->
                        val label = when (status) {
                            DoseStatus.TAKEN -> "Tomadas"
                            DoseStatus.POSTPONED -> "Pospuestas"
                            else -> "Pendientes"
                        }
                        FilterChip(selected = filter == status, onClick = { filter = status }, label = { Text(label) })
                    }
                }
            }
            if (visible.isEmpty()) {
                item { EmptyState("Sin registros", "Cuando confirmes, omitas o pospongas una toma aparecerá aquí.") }
            } else {
                items(visible, key = { it.log.id }) { HistoryCard(it) }
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
