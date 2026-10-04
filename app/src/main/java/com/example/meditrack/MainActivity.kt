package com.example.meditrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.meditrack.core.presentation.MediTrackViewModel
import com.example.meditrack.core.ui.theme.MediTrackTheme
import com.example.meditrack.feature.onboarding.MediTrackEntry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MediTrackApplication
        setContent {
            val theme by app.userPreferences.theme.collectAsState()
            MediTrackTheme(theme = theme) {
                val viewModel: MediTrackViewModel = viewModel()
                MediTrackEntry(viewModel)
            }
        }
    }
}
