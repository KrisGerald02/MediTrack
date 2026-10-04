@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.medications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.data.TreatmentCategory
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.softScrollEdges
import com.example.meditrack.core.ui.components.medicationColors
import java.util.Calendar
import java.util.Locale

@Composable
fun MedicationFormScreen(
    medicationId: Long,
    viewModel: MediTrackViewModel,
    onBack: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val availableUnits by viewModel.units.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val availableMedicationColors = medicationColors()
    var initialized by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("mg") }
    var pillsPerDose by rememberSaveable { mutableStateOf("1") }
    var category by rememberSaveable {
        mutableStateOf(
            if (profile.role == "ATHLETE") TreatmentCategory.SUPPLEMENT.name
            else TreatmentCategory.MEDICINE.name
        )
    }
    var instructions by rememberSaveable { mutableStateOf("") }
    var timesCsv by rememberSaveable { mutableStateOf("08:00,20:00") }
    var stock by rememberSaveable { mutableStateOf("30") }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var active by rememberSaveable { mutableStateOf(true) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showUnitMenu by rememberSaveable { mutableStateOf(false) }
    var showCustomUnitDialog by rememberSaveable { mutableStateOf(false) }

    val existing = medications.firstOrNull { it.id == medicationId }
    LaunchedEffect(existing, medicationId) {
        if (!initialized && medicationId != 0L && existing != null) {
            name = existing.name
            amount = existing.amount.ifBlank { existing.dose.substringBefore(" ").filter { it.isDigit() || it == '.' } }
            unit = existing.unit.ifBlank { "mg" }
            pillsPerDose = existing.pillsPerDose.toString()
            category = existing.category
            instructions = existing.instructions
            timesCsv = existing.timesCsv
            stock = existing.stock.toString()
            colorIndex = existing.colorIndex
            active = existing.active
            initialized = true
        } else if (medicationId == 0L) initialized = true
    }

    val validTimes = normalizeTimes(timesCsv)
    val isValid = name.isNotBlank() && amount.toDoubleOrNull() != null &&
        pillsPerDose.toIntOrNull()?.let { it > 0 } == true && validTimes.isNotEmpty() &&
        stock.toIntOrNull() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (medicationId == 0L) "Nuevo tratamiento" else "Editar tratamiento") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).softScrollEdges(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Información del medicamento", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = name, onValueChange = { name = it }, label = { Text("Nombre") },
                            placeholder = { Text("Ej. Amoxicilina") }, modifier = Modifier.fillMaxWidth(),
                            isError = attempted && name.isBlank(), singleLine = true
                        )
                        Text("Tipo de tratamiento", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = category == TreatmentCategory.MEDICINE.name,
                                onClick = { category = TreatmentCategory.MEDICINE.name },
                                label = { Text("Medicina") }
                            )
                            FilterChip(
                                selected = category == TreatmentCategory.SUPPLEMENT.name,
                                onClick = { category = TreatmentCategory.SUPPLEMENT.name },
                                label = { Text("Suplemento") }
                            )
                        }
                        OutlinedTextField(
                            value = instructions, onValueChange = { instructions = it }, label = { Text("Indicaciones (opcional)") },
                            placeholder = { Text("Ej. Tomar después de comer") }, modifier = Modifier.fillMaxWidth(), minLines = 2
                        )
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Dosis", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { value -> amount = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.') },
                                label = { Text("Cantidad") },
                                placeholder = { Text("500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = attempted && amount.toDoubleOrNull() == null,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Box(Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showUnitMenu = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) { Text("Medida: $unit") }
                                DropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                                    availableUnits.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = { unit = item; showUnitMenu = false }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Añadir medida…") },
                                        leadingIcon = { Icon(Icons.Default.Add, null) },
                                        onClick = { showUnitMenu = false; showCustomUnitDialog = true }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = pillsPerDose,
                            onValueChange = { pillsPerDose = it.filter(Char::isDigit) },
                            label = { Text("Cantidad de pastillas por toma") },
                            supportingText = { Text("Número de unidades que debes tomar en cada horario") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = attempted && (pillsPerDose.toIntOrNull() ?: 0) <= 0,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Horario y recordatorios", style = MaterialTheme.typography.titleLarge)
                        if (validTimes.isEmpty()) {
                            Text(
                                "Todavía no has agregado horarios.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (attempted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(validTimes, key = { it }) { time ->
                                    AssistChip(
                                        onClick = {
                                            timesCsv = validTimes.filterNot { it == time }.joinToString(",")
                                        },
                                        label = { Text(time) },
                                        leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp)) },
                                        trailingIcon = { Icon(Icons.Default.Close, "Eliminar $time", Modifier.size(18.dp)) }
                                    )
                                }
                            }
                        }
                        OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Schedule, null)
                            Spacer(Modifier.size(8.dp))
                            Text("Agregar horario")
                        }
                        Text("MediTrack enviará recordatorios locales aun sin conexión.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Inventario", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it.filter(Char::isDigit) },
                            label = { Text("Cantidad inicial de píldoras") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Color de identificación", style = MaterialTheme.typography.titleLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(availableMedicationColors) { index, color ->
                                Box(
                                    Modifier.size(44.dp).background(color, CircleShape).clickable { colorIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (colorIndex == index) Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Tratamiento activo", style = MaterialTheme.typography.titleMedium)
                                Text("Incluye las dosis en tu agenda", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = active, onCheckedChange = { active = it })
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        attempted = true
                        if (isValid) {
                            viewModel.saveMedication(
                                MedicationEntity(
                                    id = medicationId,
                                    name = name.trim(),
                                    dose = "${pillsPerDose.toInt()} pastilla(s) · ${amount.trim()} $unit",
                                    amount = amount.trim(),
                                    unit = unit,
                                    pillsPerDose = pillsPerDose.toInt(),
                                    category = category,
                                    instructions = instructions.trim(),
                                    timesCsv = validTimes.joinToString(","),
                                    stock = stock.toInt(),
                                    lowStockThreshold = maxOf(1, stock.toInt() / 5),
                                    colorIndex = colorIndex,
                                    active = active,
                                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                                ),
                                onSaved = onBack
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(if (medicationId == 0L) "Guardar tratamiento" else "Guardar cambios") }
                if (attempted && !isValid) {
                    Spacer(Modifier.height(8.dp))
                    Text("Completa los campos obligatorios y usa horarios válidos.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showTimePicker) {
        MedicationTimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { selected ->
                timesCsv = (validTimes + selected).distinct().sorted().joinToString(",")
                showTimePicker = false
            }
        )
    }
    if (showCustomUnitDialog) {
        CustomUnitDialog(
            onDismiss = { showCustomUnitDialog = false },
            onConfirm = { custom ->
                viewModel.addUnit(custom)
                unit = custom.trim().lowercase()
                showCustomUnitDialog = false
            }
        )
    }
}

@Composable
private fun MedicationTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val now = remember { Calendar.getInstance() }
    val state = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecciona la hora") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
            }) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CustomUnitDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva medida") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(12) },
                label = { Text("Abreviatura") },
                placeholder = { Text("Ej. oz, cápsula") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value) }, enabled = value.isNotBlank()) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun normalizeTimes(value: String): List<String> {
    val regex = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
    return value.split(",").map(String::trim).filter { regex.matches(it) }.distinct().sorted()
}
