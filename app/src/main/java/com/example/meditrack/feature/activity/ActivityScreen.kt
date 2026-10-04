@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.meditrack.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.feature.dashboard.DashboardScreen
import com.example.meditrack.feature.history.HistoryScreen

@Composable
fun ActivityScreen(viewModel: MediTrackViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Actividad", style = MaterialTheme.typography.headlineMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTab == 0) {
                        Button(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) { Text("Dashboard") }
                        OutlinedButton(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) { Text("Historial") }
                    } else {
                        OutlinedButton(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) { Text("Dashboard") }
                        Button(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) { Text("Historial") }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (selectedTab == 0) DashboardScreen(viewModel, embedded = true)
            else HistoryScreen(viewModel, embedded = true)
        }
    }
}
