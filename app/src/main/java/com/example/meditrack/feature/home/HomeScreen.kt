@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.example.meditrack.core.data.DailyDose
import com.example.meditrack.core.data.DoseStatus
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.EmptyState
import com.example.meditrack.core.ui.components.MedicationAvatar
import com.example.meditrack.core.ui.components.SectionHeader
import com.example.meditrack.core.ui.components.StatusPill
import com.example.meditrack.core.ui.components.softScrollEdges
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@Composable
fun HomeScreen(viewModel: MediTrackViewModel, onSettings: () -> Unit) {
    val doses by viewModel.todayDoses.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val taken = doses.count { it.status == DoseStatus.TAKEN }
    val lowStock = medications.count { it.active && it.stock <= it.lowStockThreshold }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 820.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize().softScrollEdges(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$greeting${profile.name.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty()}",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, "Abrir configuración")
                        }
                    }
                }
                item {
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AdherenceCard(taken, doses.size, Modifier.weight(1f))
                            NextDoseCard(doses.firstOrNull { it.status == DoseStatus.PENDING }, Modifier.weight(1f))
                            InventoryCard(lowStock, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdherenceCard(taken, doses.size, Modifier.fillMaxWidth())
                            NextDoseCard(doses.firstOrNull { it.status == DoseStatus.PENDING }, Modifier.fillMaxWidth())
                            if (lowStock > 0) InventoryCard(lowStock, Modifier.fillMaxWidth())
                        }
                    }
                }
                item { SectionHeader("Plan de hoy") }
                if (doses.isEmpty()) {
                    item { EmptyState("Tu día está libre", "Ve a Tratamientos para agregar tu primer medicamento o suplemento.") }
                } else {
                    items(doses, key = { "${it.medication.id}-${it.scheduledAt}" }) { dose ->
                        DoseCard(dose = dose, onStatus = { viewModel.setDoseStatus(dose, it) })
                    }
                }
            }
    }
}

@Composable
private fun AdherenceCard(taken: Int, total: Int, modifier: Modifier) {
    val progress = if (total == 0) 0f else taken.toFloat() / total
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(Modifier.padding(20.dp)) {
            Text("Cumplimiento de hoy", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Text("$taken de $total dosis", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = .24f)
            )
        }
    }
}

@Composable
private fun NextDoseCard(dose: DailyDose?, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text("Próxima dosis", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(dose?.time ?: "Sin pendientes", style = MaterialTheme.typography.headlineMedium)
            Text(dose?.let { "${it.medication.name} · ${it.medication.displayDose}" } ?: "Todo al día", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InventoryCard(lowStock: Int, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text("Inventario", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            Text(if (lowStock == 0) "Todo bien" else "$lowStock con stock bajo", style = MaterialTheme.typography.titleLarge)
            Text(if (lowStock == 0) "Sin alertas" else "Revisa tus existencias", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DoseCard(dose: DailyDose, onStatus: (DoseStatus) -> Unit) {
    val largeText = LocalDensity.current.fontScale >= 1.2f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (largeText) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Horario", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(dose.time, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    StatusPill(dose.status)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Horario", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(dose.time, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    StatusPill(dose.status)
                }
            }
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedicationAvatar(dose.medication.colorIndex, size = 56)
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(dose.medication.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dose.medication.displayDose,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (dose.medication.instructions.isNotBlank()) {
                    Text(
                        dose.medication.instructions,
                        modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(14.dp)
                        ).padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (dose.status == DoseStatus.PENDING || dose.status == DoseStatus.POSTPONED) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = { onStatus(DoseStatus.TAKEN) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Marcar como tomada")
                    }
                    FilledTonalButton(
                        onClick = { onStatus(DoseStatus.POSTPONED) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Default.MoreTime, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Recordar en 5 minutos")
                    }
                }
            }
        }
    }
}
