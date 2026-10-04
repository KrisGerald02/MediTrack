package com.example.meditrack.core.navigation

sealed class Destination(val route: String, val label: String) {
    data object Home : Destination("home", "Hoy")
    data object Dashboard : Destination("dashboard", "Actividad")
    data object Treatments : Destination("treatments", "Tratamientos")
    data object Inventory : Destination("inventory", "Inventario")
    data object Settings : Destination("settings", "Configuración")
    data object Alerts : Destination("alerts", "Alertas")
    data object AddMedication : Destination("medication/{id}", "Medicamento") {
        fun route(id: Long = 0) = "medication/$id"
    }
}
