@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.example.meditrack.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.navigation.MediTrackApp
import com.example.meditrack.core.presentation.MediTrackViewModel

private data class OnboardingPage(val title: String, val description: String, val icon: ImageVector)

private val pages = listOf(
    OnboardingPage(
        "Tus tratamientos, en orden",
        "Organiza medicinas y suplementos, dosis, horarios e inventario desde un solo lugar.",
        Icons.Default.Medication
    ),
    OnboardingPage(
        "Recordatorios que insisten",
        "Si una toma sigue pendiente, MediTrack te avisará nuevamente cada cinco minutos.",
        Icons.Default.NotificationsActive
    ),
    OnboardingPage(
        "Conoce tu progreso",
        "Consulta el cumplimiento, las tomas pospuestas u omitidas y el estado de tus existencias.",
        Icons.Default.Insights
    )
)

@Composable
fun MediTrackEntry(viewModel: MediTrackViewModel) {
    val profile by viewModel.profile.collectAsState()

    when {
        !profile.onboardingComplete -> OnboardingFlow { name, role ->
            viewModel.completeOnboarding(name, role)
        }
        else -> {
            NotificationPermissionEffect()
            MediTrackApp(viewModel)
        }
    }
}

@Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}

@Composable
private fun OnboardingFlow(onComplete: (String, String) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("CHRONIC") }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier.weight(1f).height(6.dp).background(
                        if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50)
                    )
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
            },
            label = "onboarding"
        ) { current ->
            if (current < pages.size) {
                val page = pages[current]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(36.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(page.icon, null, Modifier.padding(42.dp).size(74.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(34.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ProfileSetup(name, { name = it }, role, { role = it })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) {
                OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Atrás") }
            }
            Button(
                onClick = { if (step < 3) step++ else onComplete(name, role) },
                enabled = step < 3 || name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text(if (step < 3) "Continuar" else "Comenzar") }
        }
    }
}

@Composable
private fun ProfileSetup(
    name: String,
    onNameChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Personaliza MediTrack", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("¿Para qué quieres utilizar la app?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Tu nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(18.dp))
        RoleOption("CHRONIC", role, "Paciente crónico", "Seguimiento continuo de medicamentos", Icons.Default.Medication, onRoleChange)
        Spacer(Modifier.height(10.dp))
        RoleOption("ATHLETE", role, "Deportista", "Suplementos y rutinas de apoyo", Icons.Default.FitnessCenter, onRoleChange)
        Spacer(Modifier.height(10.dp))
        RoleOption("GENERAL", role, "Uso general", "Tratamientos temporales o cuidado diario", Icons.Default.Insights, onRoleChange)
    }
}

@Composable
private fun RoleOption(
    value: String,
    selected: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onSelect: (String) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        },
        leadingIcon = { Icon(icon, null) },
        modifier = Modifier.fillMaxWidth()
    )
}
