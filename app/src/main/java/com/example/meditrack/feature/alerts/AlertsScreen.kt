@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.data.StockLevel
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.CategoryDivider
import com.example.meditrack.core.ui.components.EmptyState
import com.example.meditrack.core.ui.components.softScrollEdges

@Composable
fun AlertsScreen(viewModel: MediTrackViewModel, onBack: () -> Unit) {
    val medications by viewModel.medications.collectAsState()
    val stockAlerts = medications.filter { it.hasUnacknowledgedStockAlert }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alertas", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .softScrollEdges(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AlertSummary(stockAlerts.size)
            }
            if (stockAlerts.isEmpty()) {
                item {
                    EmptyState(
                        "Todo está al día",
                        "No tienes alertas nuevas de inventario medio, bajo o agotado."
                    )
                }
            }
            if (stockAlerts.isNotEmpty()) {
                item { CategoryDivider("Inventario por revisar") }
                items(stockAlerts, key = { "stock-${it.id}-${it.stockLevel}" }) { medication ->
                    StockAlert(medication) { viewModel.acknowledgeStockAlert(medication) }
                }
            }
        }
    }
}

@Composable
private fun AlertSummary(lowStock: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Default.NotificationsActive,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp).size(28.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Centro de alertas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "$lowStock alertas de inventario sin confirmar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StockAlert(medication: MedicationEntity, onReceived: () -> Unit) {
    val (title, message) = when (medication.stockLevel) {
        StockLevel.OUT -> "${medication.name} · Sin existencias" to
            "El inventario llegó a 0. Sus alarmas quedan pausadas hasta agregar existencias."
        StockLevel.LOW -> "${medication.name} · Stock bajo" to
            "Quedan ${medication.stock} pastillas · Umbral bajo: ${medication.lowStockThreshold}"
        StockLevel.MEDIUM -> "${medication.name} · Stock medio" to
            "Quedan ${medication.stock} pastillas. Conviene preparar una reposición."
        StockLevel.GOOD -> medication.name to "Inventario suficiente"
    }
    AlertCard(
        icon = { Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.error) },
        title = title,
        message = message,
        onReceived = onReceived
    )
}

@Composable
private fun AlertCard(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    onReceived: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) { icon() }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onReceived, modifier = Modifier.padding(top = 10.dp)) {
                    Text("Recibido")
                }
            }
        }
    }
}
