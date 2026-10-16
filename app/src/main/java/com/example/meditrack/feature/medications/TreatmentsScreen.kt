@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.medications

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.data.MedicationRepository
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.EmptyState
import com.example.meditrack.core.ui.components.MedicationAvatar
import com.example.meditrack.core.ui.components.CategoryDivider
import com.example.meditrack.core.ui.components.softScrollEdges

@Composable
fun TreatmentsScreen(
    viewModel: MediTrackViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onAlerts: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val alertCount = medications.count { it.hasUnacknowledgedStockAlert }
    var pendingDelete by remember { mutableStateOf<MedicationEntity?>(null) }
    var showActive by remember { mutableStateOf(true) }
    val visibleMedications = medications.filter { it.active == showActive }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Nuevo") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).softScrollEdges(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tratamientos", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onAlerts) {
                        BadgedBox(badge = {
                            if (alertCount > 0) Badge { Text(if (alertCount > 99) "99+" else alertCount.toString()) }
                        }) {
                            Icon(Icons.Default.Notifications, "Ver alertas")
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (showActive) {
                        Button(onClick = { showActive = true }, modifier = Modifier.weight(1f)) {
                            Text("Activos (${medications.count { it.active }})")
                        }
                        OutlinedButton(onClick = { showActive = false }, modifier = Modifier.weight(1f)) {
                            Text("Inactivos (${medications.count { !it.active }})")
                        }
                    } else {
                        OutlinedButton(onClick = { showActive = true }, modifier = Modifier.weight(1f)) {
                            Text("Activos (${medications.count { it.active }})")
                        }
                        Button(onClick = { showActive = false }, modifier = Modifier.weight(1f)) {
                            Text("Inactivos (${medications.count { !it.active }})")
                        }
                    }
                }
            }
            if (medications.isEmpty()) {
                item { EmptyState("Aún no hay tratamientos", "Registra el nombre, cantidad, horarios e inventario de tu medicamento.") }
            } else if (visibleMedications.isEmpty()) {
                item {
                    EmptyState(
                        if (showActive) "No hay tratamientos activos" else "No hay tratamientos inactivos",
                        if (showActive) "Puedes reactivar uno desde la sección Inactivos o crear un tratamiento nuevo."
                        else "Los tratamientos que canceles aparecerán aquí con su historial conservado."
                    )
                }
            } else {
                item {
                    Text(
                        if (showActive) "${visibleMedications.size} activos" else "${visibleMedications.size} inactivos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                listOf("MEDICINE" to "Medicinas", "SUPPLEMENT" to "Suplementos").forEach { (category, title) ->
                    val grouped = visibleMedications.filter { it.category == category }
                    if (grouped.isNotEmpty()) {
                        item(key = "header-$category") { CategoryDivider(title) }
                        items(grouped, key = { it.id }) { medication ->
                            MedicationCard(
                                medication = medication,
                                onEdit = { onEdit(medication.id) },
                                onDelete = { pendingDelete = medication }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar ${medication.name}") },
            text = { Text("También se eliminará su historial. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMedication(medication); pendingDelete = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MedicationCard(
    medication: MedicationEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedicationAvatar(medication.colorIndex, size = 54)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(medication.name, style = MaterialTheme.typography.titleLarge)
                        if (!medication.active) {
                            Spacer(Modifier.width(8.dp))
                            Text("Inactivo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(medication.displayDose, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(medication.scheduleDescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    medication.finiteTreatmentPeriodLabel?.let { period ->
                        Text(period, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (!medication.active && medication.deactivatedAt > 0) {
                        Text(
                            "Cancelado el ${MedicationRepository.formatDate(medication.deactivatedAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
