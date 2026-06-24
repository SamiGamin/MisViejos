package com.xd.misviejos.feature.timeline

import android.widget.Toast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.data.repository.TurnoRepository
import com.xd.misviejos.domain.model.EstadoTurno
import com.xd.misviejos.domain.model.TipoEventoMedico
import com.xd.misviejos.domain.model.Turno
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MiTurnoScreen(
    groupId: String,
    nombreUsuario: String,
    turnoRepository: TurnoRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Obtener los turnos reactivamente
    val agendaState by remember(groupId) {
        turnoRepository.obtenerTurnosFamilia(groupId)
    }.collectAsState(initial = emptyList())

    // 2. Filtrar turnos donde el usuario es el responsable (sugerido o confirmado) y están pendientes o activos
    val misTurnosActivos = remember(agendaState, nombreUsuario) {
        agendaState.filter { turno ->
            (turno.hermanoSugerido == nombreUsuario || turno.hermanoConfirmado == nombreUsuario) &&
                    (turno.estado == EstadoTurno.PENDIENTE || turno.estado == EstadoTurno.CONFIRMADO)
        }.sortedBy { it.fechaInstante }
    }

    // Estado del diálogo de cierre
    var turnoAEntregar by remember { mutableStateOf<Turno?>(null) }
    var notasCierreInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Encabezado rápido del perfil activo
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column {
                    Text(
                        text = "Mis Labores de Cuidado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Hermano activo: $nombreUsuario",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Listado de turnos
        if (misTurnosActivos.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "¡Libre de Turnos! ☕",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No tienes labores de cuidado pendientes o por confirmar hoy.",
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
                items(misTurnosActivos) { turno ->
                    MiTurnoItem(
                        turno = turno,
                        nombreUsuario = nombreUsuario,
                        onAceptar = {
                            scope.launch {
                                val res = turnoRepository.aceptarTurno(groupId, turno.id, nombreUsuario)
                                res.onSuccess {
                                    Toast.makeText(context, "¡Turno confirmado!", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "Error al confirmar: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onEntregar = {
                            turnoAEntregar = turno
                            notasCierreInput = ""
                        }
                    )
                }
            }
        }
    }

    // Diálogo de entrega de testigo
    if (turnoAEntregar != null) {
        AlertDialog(
            onDismissRequest = { turnoAEntregar = null },
            title = {
                Text(
                    text = "Cerrar Turno de Cuidado",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Instrucciones para el siguiente hermano:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = notasCierreInput,
                        onValueChange = { notasCierreInput = it },
                        placeholder = { Text("Escribe lo que recetó el doctor, novedades, dosis, o recomendaciones...") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val turno = turnoAEntregar ?: return@Button
                        if (notasCierreInput.isBlank()) {
                            Toast.makeText(context, "Debes dejar una nota médica de cierre.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        scope.launch {
                            val res = turnoRepository.entregarTestigo(groupId, turno.id, notasCierreInput)
                            res.onSuccess {
                                Toast.makeText(context, "¡Turno completado e historial actualizado!", Toast.LENGTH_LONG).show()
                                turnoAEntregar = null
                            }.onFailure {
                                Toast.makeText(context, "Error al entregar: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = notasCierreInput.isNotBlank()
                ) {
                    Text("Guardar y Cerrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { turnoAEntregar = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun MiTurnoItem(
    turno: Turno,
    nombreUsuario: String,
    onAceptar: () -> Unit,
    onEntregar: () -> Unit
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

    val esPendiente = turno.estado == EstadoTurno.PENDIENTE

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esPendiente) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(
            width = if (esPendiente) 1.dp else 2.dp,
            color = if (esPendiente) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
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

            // LADO DERECHO: Detalles y Acciones
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

                    // Estado
                    val badgeColor = if (esPendiente) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    val badgeText = if (esPendiente) "Por confirmar" else "Confirmado"
                    Text(
                        text = badgeText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
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

                // Detalles específicos
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
                                text = "Dosis/Instrucciones: ${turno.dosis}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TipoEventoMedico.AUTORIZACION_TRAMITE -> {
                        if (!turno.documentos.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Documentos: ${turno.documentos}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TipoEventoMedico.EXAMEN_MEDICO -> {
                        if (!turno.requisitos.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Requisitos: ${turno.requisitos}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!turno.notasDelMedico.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notas: ${turno.notasDelMedico}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Acciones
                if (esPendiente) {
                    Button(
                        onClick = onAceptar,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar que me haré cargo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = onEntregar,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Turno / Entregar Testigo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
