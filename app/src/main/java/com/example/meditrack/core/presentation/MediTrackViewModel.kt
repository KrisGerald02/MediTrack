package com.example.meditrack.core.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.core.data.DailyDose
import com.example.meditrack.core.data.DoseStatus
import com.example.meditrack.core.data.HistoryItem
import com.example.meditrack.core.data.MedicationEntity
import com.example.meditrack.core.data.UserProfile
import com.example.meditrack.core.notifications.MedicationAlarmScheduler
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MediTrackApplication
    private val repository = app.repository
    private val scheduler = MedicationAlarmScheduler(application)

    val profile: StateFlow<UserProfile> = app.userPreferences.profile
    val units: StateFlow<List<String>> = app.userPreferences.units
    val appTheme: StateFlow<String> = app.userPreferences.theme

    val medications: StateFlow<List<MedicationEntity>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val todayDoses: StateFlow<List<DailyDose>> = repository.todayDoses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history: StateFlow<List<HistoryItem>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveMedication(medication: MedicationEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            val previous = if (medication.id != 0L) repository.getMedication(medication.id) else null
            previous?.let(scheduler::cancel)
            val id = repository.save(medication)
            repository.getMedication(id)?.let(scheduler::schedule)
            onSaved()
        }
    }

    fun deleteMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            scheduler.cancel(medication)
            repository.delete(medication)
        }
    }

    fun setDoseStatus(dose: DailyDose, status: DoseStatus) {
        viewModelScope.launch {
            repository.recordDose(dose, status)
            if (status == DoseStatus.POSTPONED) {
                scheduler.scheduleRepeat(dose.medication, dose.time, dose.scheduledAt)
            } else if (status == DoseStatus.TAKEN || status == DoseStatus.SKIPPED) {
                scheduler.cancelRepeat(dose.medication, dose.time, dose.scheduledAt)
                NotificationManagerCompat.from(getApplication()).cancel(
                    "${dose.medication.id}-${dose.scheduledAt}".hashCode()
                )
            }
        }
    }

    fun refill(medicationId: Long, amount: Int = 10) {
        viewModelScope.launch { repository.refill(medicationId, amount) }
    }

    fun completeOnboarding(name: String, role: String) {
        app.userPreferences.completeOnboarding(name, role)
    }

    fun addUnit(unit: String) = app.userPreferences.addUnit(unit)
    fun updateName(name: String) = app.userPreferences.updateName(name)
    fun updateTheme(theme: String) = app.userPreferences.updateTheme(theme)
}
