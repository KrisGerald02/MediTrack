package com.example.meditrack.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.meditrack.core.data.DoseStatus

@Composable
fun medicationColors(): List<Color> = MaterialTheme.colorScheme.run {
    listOf(primary, secondary, tertiary, primary.copy(alpha = .72f), secondary.copy(alpha = .72f), tertiary.copy(alpha = .72f))
}

/** Suaviza la entrada y salida del contenido desplazable para evitar cortes bruscos. */
fun Modifier.softScrollEdges(color: Color, edgeHeight: Dp = 18.dp): Modifier = drawWithContent {
    drawContent()
    val edge = edgeHeight.toPx().coerceAtMost(size.height / 3f)
    drawRect(
        brush = Brush.verticalGradient(listOf(color, Color.Transparent), 0f, edge),
        size = Size(size.width, edge)
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, color),
            size.height - edge,
            size.height
        ),
        topLeft = Offset(0f, size.height - edge),
        size = Size(size.width, edge)
    )
}

@Composable
fun MedicationAvatar(colorIndex: Int, modifier: Modifier = Modifier, size: Int = 48) {
    val colors = medicationColors()
    val color = colors[Math.floorMod(colorIndex, colors.size)]
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color.copy(alpha = .14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Medication,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((size * .5f).dp)
        )
    }
}

@Composable
fun StatusPill(status: DoseStatus) {
    val (label, color) = when (status) {
        DoseStatus.PENDING -> "Pendiente" to MaterialTheme.colorScheme.tertiary
        DoseStatus.TAKEN -> "Tomada" to MaterialTheme.colorScheme.primary
        DoseStatus.SKIPPED -> "Pospuesta" to MaterialTheme.colorScheme.secondary
        DoseStatus.POSTPONED -> "Pospuesta" to MaterialTheme.colorScheme.secondary
    }
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(color.copy(alpha = .12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MedicationAvatar(0, size = 64)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        action?.invoke()
    }
}

@Composable
fun CategoryDivider(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(12.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = .25f))
    }
}

@Composable
fun AddInventoryDialog(
    medicationName: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    val validAmount = amount.toIntOrNull()?.let { it > 0 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar al inventario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Indica cuántas píldoras deseas agregar a $medicationName. La cantidad se sumará al inventario actual.")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = { Text("Cantidad de píldoras") },
                    placeholder = { Text("Ej. 30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = validAmount,
                onClick = { amount.toIntOrNull()?.let(onConfirm) }
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
