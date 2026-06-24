package com.xd.misviejos.feature.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.domain.model.EstadoTurno
import com.xd.misviejos.domain.model.TipoEventoMedico
import com.xd.misviejos.domain.model.Turno
import com.xd.misviejos.data.repository.TurnoRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun TimelineScreen(
    groupId: String,
    turnoRepository: TurnoRepository,
    isAdmin: Boolean,
    onEditarTurno: (Turno) -> Unit,
    modifier: Modifier = Modifier
) {
    var turnoAEliminar by remember { mutableStateOf<Turno?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // 1. Obtener la agenda reactiva de Firestore
    val agendaState by remember(groupId) {
        turnoRepository.obtenerTurnosFamilia(groupId)
    }.collectAsState(initial = emptyList())

    // 2. Estado de filtro
    var filtroSeleccionado by remember { mutableStateOf<TipoEventoMedico?>(null) }

    // 3. Filtrar y ordenar cronológicamente (Solo turnos Pendientes o Confirmados)
    val turnosFiltrados = remember(agendaState, filtroSeleccionado) {
        val activos = agendaState.filter { it.estado == EstadoTurno.PENDIENTE || it.estado == EstadoTurno.CONFIRMADO }
        val lista = if (filtroSeleccionado == null) {
            activos
        } else {
            activos.filter { it.tipoEvento == filtroSeleccionado }
        }
        lista.sortedBy { it.fechaInstante }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- FILTROS RÁPIDOS ---
        Text(
            text = "Filtrar por tipo de cuidado",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomFilterChip(
                selected = filtroSeleccionado == null,
                onClick = { filtroSeleccionado = null },
                label = "Todos",
                modifier = Modifier.weight(1f)
            )
            CustomFilterChip(
                selected = filtroSeleccionado == TipoEventoMedico.CITA_MEDICA,
                onClick = { filtroSeleccionado = TipoEventoMedico.CITA_MEDICA },
                label = "Citas",
                modifier = Modifier.weight(1f)
            )
            CustomFilterChip(
                selected = filtroSeleccionado == TipoEventoMedico.RECOGER_MEDICAMENTOS,
                onClick = { filtroSeleccionado = TipoEventoMedico.RECOGER_MEDICAMENTOS },
                label = "Medicamentos",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomFilterChip(
                selected = filtroSeleccionado == TipoEventoMedico.AUTORIZACION_TRAMITE,
                onClick = { filtroSeleccionado = TipoEventoMedico.AUTORIZACION_TRAMITE },
                label = "Trámites",
                modifier = Modifier.weight(1f)
            )
            CustomFilterChip(
                selected = filtroSeleccionado == TipoEventoMedico.EXAMEN_MEDICO,
                onClick = { filtroSeleccionado = TipoEventoMedico.EXAMEN_MEDICO },
                label = "Exámenes",
                modifier = Modifier.weight(1f)
            )
        }

        // --- LÍNEA DE TIEMPO (LazyColumn) ---
        if (turnosFiltrados.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Agenda vacía",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Los eventos que programes se verán aquí en tiempo real.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(turnosFiltrados) { turno ->
                    TimelineItem(
                        turno = turno,
                        isAdmin = isAdmin,
                        onEditar = onEditarTurno,
                        onEliminar = { turnoAEliminar = it }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (turnoAEliminar != null) {
        AlertDialog(
            onDismissRequest = { turnoAEliminar = null },
            title = { Text("Eliminar Evento", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar este evento de cuidado? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        val turno = turnoAEliminar ?: return@Button
                        scope.launch {
                            val res = turnoRepository.eliminarTurno(groupId, turno.id)
                            res.onSuccess {
                                Toast.makeText(context, "Evento eliminado exitosamente", Toast.LENGTH_SHORT).show()
                                turnoAEliminar = null
                            }.onFailure {
                                Toast.makeText(context, "Error al eliminar: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { turnoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CustomFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimelineItem(
    turno: Turno,
    isAdmin: Boolean,
    onEditar: (Turno) -> Unit,
    onEliminar: (Turno) -> Unit
) {
    val localDateTime = remember(turno.fechaInstante) {
        LocalDateTime.ofInstant(turno.fechaInstante, ZoneId.systemDefault())
    }
    val formateadorFecha = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val formateadorHora = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    val colorTipo = when (turno.tipoEvento) {
        TipoEventoMedico.CITA_MEDICA -> MaterialTheme.colorScheme.primary
        TipoEventoMedico.RECOGER_MEDICAMENTOS -> MaterialTheme.colorScheme.secondary
        TipoEventoMedico.AUTORIZACION_TRAMITE -> Color(0xFFE5A93B)
        TipoEventoMedico.EXAMEN_MEDICO -> Color(0xFF6B4EE0)
    }

    val iconTipo = when (turno.tipoEvento) {
        TipoEventoMedico.CITA_MEDICA -> Icons.Default.MedicalServices
        TipoEventoMedico.RECOGER_MEDICAMENTOS -> Icons.Default.Medication
        TipoEventoMedico.AUTORIZACION_TRAMITE -> Icons.Default.Description
        TipoEventoMedico.EXAMEN_MEDICO -> Icons.Default.Science
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // LADO IZQUIERDO: Hora, Fecha y Tipo Icono
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Text(
                    text = localDateTime.format(formateadorHora),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = localDateTime.format(formateadorFecha).uppercase(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(colorTipo.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconTipo,
                        contentDescription = null,
                        tint = colorTipo,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // LADO DERECHO: Detalles del Evento
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag de paciente
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = turno.paciente,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Estado del turno
                    val colorEstado = when (turno.estado) {
                        EstadoTurno.PENDIENTE -> MaterialTheme.colorScheme.tertiary
                        EstadoTurno.CONFIRMADO -> MaterialTheme.colorScheme.primary
                        EstadoTurno.COMPLETADO -> MaterialTheme.colorScheme.secondary
                        EstadoTurno.CANCELADO -> MaterialTheme.colorScheme.error
                    }
                    val textEstado = when (turno.estado) {
                        EstadoTurno.PENDIENTE -> "Pendiente"
                        EstadoTurno.CONFIRMADO -> "Confirmado"
                        EstadoTurno.COMPLETADO -> "Completado"
                        EstadoTurno.CANCELADO -> "Cancelado"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = textEstado,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorEstado
                        )
                        if (isAdmin && turno.estado == EstadoTurno.PENDIENTE) {
                            IconButton(
                                onClick = { onEditar(turno) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onEliminar(turno) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = turno.especialidad,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Lugar: ${turno.lugar}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Campos condicionales
                when (turno.tipoEvento) {
                    TipoEventoMedico.CITA_MEDICA -> {
                        if (!turno.doctor.isNullOrBlank() || !turno.consultorio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Doc: ${turno.doctor ?: "N/A"} - Cons: ${turno.consultorio ?: "N/A"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TipoEventoMedico.RECOGER_MEDICAMENTOS -> {
                        if (!turno.dosis.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Indicaciones: ${turno.dosis}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TipoEventoMedico.AUTORIZACION_TRAMITE -> {
                        if (!turno.documentos.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Requisitos: ${turno.documentos}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TipoEventoMedico.EXAMEN_MEDICO -> {
                        if (!turno.requisitos.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Preparación: ${turno.requisitos}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!turno.notasDelMedico.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notas: ${turno.notesExtra()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Separador
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Responsables
                if (turno.hermanoConfirmado != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Cubre: ${turno.hermanoConfirmado}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Asignado: ${turno.hermanoSugerido}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Extensión para devolver la nota del médico o notas de agenda
private fun Turno.notesExtra(): String {
    return this.notasDelMedico ?: ""
}
