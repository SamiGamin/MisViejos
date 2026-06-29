package com.xd.misviejos.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TabNav(
    val ruta: String,
    val titulo: String,
    val iconoSeleccionado: ImageVector,
    val iconoNoSeleccionado: ImageVector
) {
    object MiTurno : TabNav(
        ruta = "mi_turno",
        titulo = "Mi Turno",
        // La pildorita
        iconoSeleccionado = Icons.Filled.Medication,
        iconoNoSeleccionado = Icons.Outlined.Medication
    )

    object Pista : TabNav(
        ruta = "pista_general",
        titulo = "Agenda",
        // El calendario de turnos
        iconoSeleccionado = Icons.Filled.CalendarMonth,
        iconoNoSeleccionado = Icons.Outlined.CalendarMonth
    )

    object Testigo : TabNav(
        ruta = "el_testigo",
        titulo = "Bitácora",
        // [Joyita de Extended]: Una tabla de hospital con una cruz médica
        iconoSeleccionado = Icons.Filled.MedicalInformation,
        iconoNoSeleccionado = Icons.Outlined.MedicalInformation
    )

    object Archivo : TabNav(
        ruta = "archivo",
        titulo = "Historial",
        // La carpeta de archivo tradicional
        iconoSeleccionado = Icons.Filled.Folder,
        iconoNoSeleccionado = Icons.Outlined.Folder
    )

    object Ahorro : TabNav(
        ruta = "ahorro_screen",
        titulo = "Fondo",
        iconoSeleccionado = Icons.Filled.MonetizationOn,
        iconoNoSeleccionado = Icons.Outlined.MonetizationOn
    )

    object Ajustes : TabNav(
        ruta = "ajustes",
        titulo = "Ajustes",
        iconoSeleccionado = Icons.Filled.Settings,
        iconoNoSeleccionado = Icons.Outlined.Settings
    )
}