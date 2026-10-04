@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.data.DoseStatus
import com.example.meditrack.core.data.MedicationRepository
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.softScrollEdges
import com.example.meditrack.core.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.rememberDateRangePickerState

@Composable
fun DashboardScreen(viewModel: MediTrackViewModel, embedded: Boolean = false) {
    val history by viewModel.history.collectAsState()
    val medications by viewModel.medications.collectAsState()
    var selectedMedicationId by rememberSaveable { mutableLongStateOf(-1L) }
    var selectedStartDate by rememberSaveable { mutableLongStateOf(-1L) }
    var selectedEndDate by rememberSaveable { mutableLongStateOf(-1L) }
    var showTreatmentMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val hasDateRange = selectedStartDate > 0 && selectedEndDate > 0
    val periodStart = if (hasDateRange) MedicationRepository.startOfDay(selectedStartDate) else monthStart
    val periodEnd = if (hasDateRange) {
        MedicationRepository.startOfDay(selectedEndDate) + MedicationRepository.DAY_MS
    } else Long.MAX_VALUE
    val monthLogs = history.filter { item ->
        item.log.scheduledAt in periodStart until periodEnd &&
            (selectedMedicationId < 0 || item.log.medicationId == selectedMedicationId)
    }
    val taken = monthLogs.count { it.log.status == DoseStatus.TAKEN.name }
    val postponed = monthLogs.count { it.log.status == DoseStatus.POSTPONED.name }
    val completed = taken + postponed
    val adherence = if (completed == 0) 0 else (taken * 100 / completed)
    val lowStock = medications.count { it.active && it.stock <= it.lowStockThreshold }
    val periodName = if (hasDateRange) {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "NI"))
        "${formatter.format(Date(selectedStartDate))} - ${formatter.format(Date(selectedEndDate))}"
    } else {
        SimpleDateFormat("MMMM yyyy", Locale("es", "NI")).format(Date())
    }.replaceFirstChar { it.uppercase() }
    val selectedTreatment = medications.firstOrNull { it.id == selectedMedicationId }

    BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 780.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize().softScrollEdges(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!embedded) {
                    item { Text("Dashboard", style = MaterialTheme.typography.headlineMedium) }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(periodName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        @Composable fun TreatmentFilter(modifier: Modifier = Modifier) {
                            Box(modifier) {
                                OutlinedButton(
                                    onClick = { showTreatmentMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedTreatment?.name ?: "Todos los tratamientos")
                                }
                                DropdownMenu(expanded = showTreatmentMenu, onDismissRequest = { showTreatmentMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Todos los tratamientos") },
                                        onClick = { selectedMedicationId = -1L; showTreatmentMenu = false }
                                    )
                                    medications.forEach { medication ->
                                        DropdownMenuItem(
                                            text = { Text(medication.name) },
                                            onClick = { selectedMedicationId = medication.id; showTreatmentMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                        if (wide) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TreatmentFilter(Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasDateRange) "Cambiar rango" else "Entre fechas")
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TreatmentFilter(Modifier.fillMaxWidth())
                                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (hasDateRange) "Cambiar rango" else "Entre fechas")
                                }
                            }
                        }
                        if (hasDateRange) {
                            TextButton(onClick = { selectedStartDate = -1L; selectedEndDate = -1L }) {
                                Text("Volver al mes actual")
                            }
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, null, tint = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (hasDateRange) "Cumplimiento del período" else "Cumplimiento mensual",
                                    color = Color.White.copy(alpha = .86f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("$adherence%", color = Color.White, style = MaterialTheme.typography.displaySmall)
                            Text("$taken dosis tomadas de $completed registradas", color = Color.White.copy(alpha = .85f))
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { adherence / 100f },
                                modifier = Modifier.fillMaxWidth().height(9.dp),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = .24f)
                            )
                        }
                    }
                }
                item {
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            MetricCard("Tomadas", taken, Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                            MetricCard("Pospuestas", postponed, Icons.Default.MoreTime, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard("Tomadas", taken, Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())
                            MetricCard("Pospuestas", postponed, Icons.Default.MoreTime, MaterialTheme.colorScheme.secondary, Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    Text("Estado del tratamiento", style = MaterialTheme.typography.titleLarge)
                }
                item {
                    if (monthLogs.isEmpty()) {
                        EmptyState(
                            if (hasDateRange) "Sin actividad en este período" else "Sin actividad este mes",
                            "Registra tus tomas para visualizar aquí tu progreso."
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardRow("Tratamientos activos", medications.count { it.active }.toString())
                                DashboardRow("Dosis registradas", monthLogs.size.toString())
                                DashboardRow("Medicamentos con stock bajo", lowStock.toString(), lowStock > 0)
                            }
                        }
                    }
                }
            }
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = selectedStartDate.takeIf { it > 0 },
            initialSelectedEndDateMillis = selectedEndDate.takeIf { it > 0 }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        selectedStartDate = start
                        selectedEndDate = end
                        showDatePicker = false
                    }
                }, enabled = pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null) {
                    Text("Aplicar")
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DateRangePicker(state = pickerState, modifier = Modifier.height(520.dp)) }
    }
}

@Composable
private fun MetricCard(label: String, value: Int, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(color.copy(alpha = .13f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                Icon(icon, null, tint = color)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashboardRow(label: String, value: String, alert: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = if (alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}
