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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

@Composable
fun MedicationFormScreen(
    medicationId: Long,
    viewModel: MediTrackViewModel,
    onBack: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val availableMedicationColors = medicationColors()
    var initialized by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var pillsPerDose by rememberSaveable { mutableStateOf("1") }
    var category by rememberSaveable {
        mutableStateOf(
            if (profile.role == "ATHLETE") TreatmentCategory.SUPPLEMENT.name
            else TreatmentCategory.MEDICINE.name
        )
    }
    var instructions by rememberSaveable { mutableStateOf("") }
    var timesCsv by rememberSaveable { mutableStateOf("08:00,20:00") }
    var reminderIntervalMinutes by rememberSaveable { mutableStateOf("5") }
    var useDoseInterval by rememberSaveable { mutableStateOf(false) }
    var doseIntervalHours by rememberSaveable { mutableStateOf("4") }
    var treatmentStartDate by rememberSaveable {
        mutableStateOf(com.example.meditrack.core.data.MedicationRepository.startOfDay(System.currentTimeMillis()))
    }
    var limitedDuration by rememberSaveable { mutableStateOf(false) }
    var treatmentDurationDays by rememberSaveable { mutableStateOf("7") }
    var stock by rememberSaveable { mutableStateOf("30") }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var active by rememberSaveable { mutableStateOf(true) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }

    val existing = medications.firstOrNull { it.id == medicationId }
    LaunchedEffect(existing, medicationId) {
        if (!initialized && medicationId != 0L && existing != null) {
            name = existing.name
            pillsPerDose = existing.pillsPerDose.toString()
            category = existing.category
            instructions = existing.instructions
            timesCsv = existing.timesCsv
            reminderIntervalMinutes = existing.reminderIntervalMinutes.toString()
            useDoseInterval = existing.doseIntervalHours > 0
            doseIntervalHours = (existing.doseIntervalHours.takeIf { it > 0 } ?: 4).toString()
            treatmentStartDate = existing.normalizedTreatmentStartDate
            limitedDuration = existing.treatmentDurationDays > 0
            treatmentDurationDays = (existing.treatmentDurationDays.takeIf { it > 0 } ?: 7).toString()
            stock = existing.stock.toString()
            colorIndex = existing.colorIndex
            active = existing.active
            initialized = true
        } else if (medicationId == 0L) initialized = true
    }

    val validTimes = normalizeTimes(timesCsv)
    val intervalHours = doseIntervalHours.toIntOrNull()
    val validDoseInterval = !useDoseInterval || intervalHours?.let { it in supportedDoseIntervals } == true
    val validDuration = !limitedDuration || treatmentDurationDays.toIntOrNull()?.let { it in 1..3650 } == true
    val isValid = name.isNotBlank() && pillsPerDose.toIntOrNull()?.let { it > 0 } == true && validTimes.isNotEmpty() &&
        stock.toIntOrNull() != null &&
        reminderIntervalMinutes.toIntOrNull()?.let { it in 1..1440 } == true &&
        validDoseInterval && validDuration

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
                        Text("Frecuencia del tratamiento", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = !useDoseInterval,
                                onClick = { useDoseInterval = false },
                                label = { Text("Horarios") }
                            )
                            FilterChip(
                                selected = useDoseInterval,
                                onClick = {
                                    useDoseInterval = true
                                    timesCsv = validTimes.firstOrNull() ?: "08:00"
                                },
                                label = { Text("Intervalo") }
                            )
                        }
                        if (useDoseInterval) {
                            OutlinedTextField(
                                value = doseIntervalHours,
                                onValueChange = { doseIntervalHours = it.filter(Char::isDigit).take(2) },
                                label = { Text("Repetir tratamiento cada (horas)") },
                                placeholder = { Text("Ej. 4") },
                                supportingText = { Text("Opciones: 1, 2, 3, 4, 6, 8, 12 o 24 horas") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = attempted && !validDoseInterval,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
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
                                        label = { Text(if (useDoseInterval) "Inicia $time" else time) },
                                        leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp)) },
                                        trailingIcon = { Icon(Icons.Default.Close, "Eliminar $time", Modifier.size(18.dp)) }
                                    )
                                }
                            }
                        }
                        OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Schedule, null)
                            Spacer(Modifier.size(8.dp))
                            Text(if (useDoseInterval) "Seleccionar hora inicial" else "Agregar horario")
                        }
                        if (useDoseInterval && validDoseInterval && validTimes.isNotEmpty()) {
                            val preview = MedicationEntity(
                                name = "",
                                dose = "",
                                doseIntervalHours = intervalHours ?: 0,
                                timesCsv = validTimes.first(),
                                stock = 0,
                                lowStockThreshold = 0
                            ).times.joinToString(" · ")
                            Text(
                                "Horarios calculados: $preview",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedTextField(
                            value = reminderIntervalMinutes,
                            onValueChange = { reminderIntervalMinutes = it.filter(Char::isDigit).take(4) },
                            label = { Text("Repetir alarma cada (minutos)") },
                            supportingText = { Text("Se repetirá mientras no marques la dosis como tomada") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = attempted && reminderIntervalMinutes.toIntOrNull()?.let { it in 1..1440 } != true,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Duración del tratamiento", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = !limitedDuration,
                                onClick = { limitedDuration = false },
                                label = { Text("Siempre") }
                            )
                            FilterChip(
                                selected = limitedDuration,
                                onClick = { limitedDuration = true },
                                label = { Text("Por días") }
                            )
                        }
                        OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Inicia: ${formatTreatmentDate(treatmentStartDate)}")
                        }
                        if (limitedDuration) {
                            OutlinedTextField(
                                value = treatmentDurationDays,
                                onValueChange = { treatmentDurationDays = it.filter(Char::isDigit).take(4) },
                                label = { Text("Duración en días") },
                                placeholder = { Text("Ej. 7") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = attempted && !validDuration,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            val duration = treatmentDurationDays.toIntOrNull()
                            if (duration != null && duration > 0) {
                                Text(
                                    "Resumen: inicia ${formatTreatmentDate(treatmentStartDate)} y finaliza ${formatTreatmentDate(calculateTreatmentEnd(treatmentStartDate, duration))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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
                                Text(
                                    if (active) "Tratamiento activo" else "Tratamiento cancelado",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    if (active) "Incluye las dosis en tu agenda y mantiene sus alarmas."
                                    else "Cancela las alarmas, pero conserva el tratamiento y todo su historial.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                    dose = if (pillsPerDose.toInt() == 1) "1 pastilla" else "${pillsPerDose.toInt()} pastillas",
                                    amount = "",
                                    unit = "",
                                    pillsPerDose = pillsPerDose.toInt(),
                                    category = category,
                                    reminderIntervalMinutes = reminderIntervalMinutes.toInt(),
                                    doseIntervalHours = if (useDoseInterval) intervalHours ?: 0 else 0,
                                    treatmentStartDate = treatmentStartDate,
                                    treatmentDurationDays = if (limitedDuration) treatmentDurationDays.toInt() else 0,
                                    instructions = instructions.trim(),
                                    timesCsv = if (useDoseInterval) validTimes.first() else validTimes.joinToString(","),
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
                timesCsv = if (useDoseInterval) selected
                else (validTimes + selected).distinct().sorted().joinToString(",")
                showTimePicker = false
            }
        )
    }
    if (showStartDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToPickerMillis(treatmentStartDate)
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { treatmentStartDate = pickerMillisToLocalDate(it) }
                    showStartDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = dateState) }
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

private fun normalizeTimes(value: String): List<String> {
    val regex = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
    return value.split(",").map(String::trim).filter { regex.matches(it) }.distinct().sorted()
}

private val supportedDoseIntervals = setOf(1, 2, 3, 4, 6, 8, 12, 24)

private fun calculateTreatmentEnd(start: Long, durationDays: Int): Long = Calendar.getInstance().apply {
    timeInMillis = start
    add(Calendar.DAY_OF_YEAR, durationDays - 1)
}.timeInMillis

private fun formatTreatmentDate(time: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale("es", "NI")).format(Date(time))

private fun localDateToPickerMillis(localTime: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localTime }
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun pickerMillisToLocalDate(pickerTime: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerTime }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
