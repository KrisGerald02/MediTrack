@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.components.softScrollEdges

private data class ThemeOption(val id: String, val label: String, val color: Color)

private val themeOptions = listOf(
    ThemeOption("PINK", "Rosado", Color(0xFFE84393)),
    ThemeOption("PURPLE", "Morado", Color(0xFF7C3AED)),
    ThemeOption("BLUE", "Azul", Color(0xFF2563EB)),
    ThemeOption("GREEN", "Verde", Color(0xFF168A62)),
    ThemeOption("RED", "Rojo", Color(0xFFDC2626)),
    ThemeOption("ORANGE", "Naranja", Color(0xFFEA580C)),
    ThemeOption("TEAL", "Turquesa", Color(0xFF0F766E))
)

@Composable
fun SettingsScreen(viewModel: MediTrackViewModel, onBack: () -> Unit) {
    val profile by viewModel.profile.collectAsState()
    val selectedTheme by viewModel.appTheme.collectAsState()
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).softScrollEdges(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Tu perfil", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = { viewModel.updateName(name) },
                            enabled = name.isNotBlank() && name.trim() != profile.name,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Guardar nombre") }
                    }
                }
            }
            item { Text("Color de la aplicación", style = MaterialTheme.typography.titleLarge) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    themeOptions.forEach { option ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.updateTheme(option.id) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTheme == option.id) option.color.copy(alpha = .12f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(40.dp).background(option.color, CircleShape))
                                Text(option.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 14.dp))
                                if (selectedTheme == option.id) Icon(Icons.Default.Check, "Seleccionado", tint = option.color)
                            }
                        }
                    }
                }
            }
        }
    }
}
