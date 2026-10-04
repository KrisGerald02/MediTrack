@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.inventory

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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.EmptyState
import com.example.meditrack.core.ui.components.MedicationAvatar
import com.example.meditrack.core.ui.components.AddInventoryDialog
import com.example.meditrack.core.ui.components.CategoryDivider
import com.example.meditrack.core.ui.components.softScrollEdges

@Composable
fun InventoryScreen(viewModel: MediTrackViewModel) {
    val medications by viewModel.medications.collectAsState()
    val lowStock = medications.count { it.stock <= it.lowStockThreshold }
    var pendingInventory by remember { mutableStateOf<MedicationEntity?>(null) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).softScrollEdges(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Text("Inventario", style = MaterialTheme.typography.headlineMedium) }
            item {
                InventorySummary(lowStock = lowStock, total = medications.size)
            }
            if (medications.isEmpty()) {
                item { EmptyState("Inventario vacío", "Agrega un tratamiento para comenzar a controlar tus existencias.") }
            } else {
                listOf("MEDICINE" to "Medicinas", "SUPPLEMENT" to "Suplementos").forEach { (category, title) ->
                    val grouped = medications.filter { it.category == category }
                    if (grouped.isNotEmpty()) {
                        item(key = "inventory-header-$category") { CategoryDivider(title) }
                        items(grouped, key = { it.id }) { medication ->
                            InventoryItem(medication) { pendingInventory = medication }
                        }
                    }
                }
            }
        }
    }

    pendingInventory?.let { medication ->
        AddInventoryDialog(
            medicationName = medication.name,
            onDismiss = { pendingInventory = null },
            onConfirm = { amount ->
                viewModel.refill(medication.id, amount)
                pendingInventory = null
            }
        )
    }
}

@Composable
private fun InventorySummary(lowStock: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Inventory2,
                null,
                tint = if (lowStock > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("$lowStock de $total", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tratamientos con stock bajo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InventoryItem(medication: MedicationEntity, onRefill: () -> Unit) {
    val isLow = medication.stock <= medication.lowStockThreshold
    val levelColor = inventoryLevelColor(medication)
    val visualCapacity = maxOf(medication.stock, medication.lowStockThreshold * 3, 1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedicationAvatar(medication.colorIndex)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(medication.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        medication.displayDose,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${medication.stock}", style = MaterialTheme.typography.headlineMedium, color = levelColor)
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { (medication.stock.toFloat() / visualCapacity).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = levelColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onRefill) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar")
                }
            }
        }
    }
}

private fun inventoryLevelColor(medication: MedicationEntity): Color = when {
    medication.stock <= medication.lowStockThreshold -> Color(0xFFE53935)
    medication.stock <= medication.lowStockThreshold * 2 -> Color(0xFFF9A825)
    else -> Color(0xFF2EAD62)
}
