package com.xd.misviejos.feature.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.data.repository.TurnoRepository
import com.xd.misviejos.domain.model.EstadoTurno
import com.xd.misviejos.domain.model.TipoEventoMedico
import com.xd.misviejos.domain.model.Turno
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistorialScreen(
    groupId: String,
    turnoRepository: TurnoRepository,
    modifier: Modifier = Modifier
) {
    // 1. Obtener la agenda reactiva
    val agendaState by remember(groupId) {
        turnoRepository.obtenerTurnosFamilia(groupId)
    }.collectAsState(initial = emptyList())

    // 2. Filtrar turnos archivados (Completados y Cancelados)
    val turnosArchivados = remember(agendaState) {
        agendaState.filter { it.estado == EstadoTurno.COMPLETADO || it.estado == EstadoTurno.CANCELADO }
            .sortedByDescending { it.fechaInstante }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (turnosArchivados.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Historial vacío",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Los turnos completados o cancelados se archivarán en esta sección para futura referencia.",
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
                items(turnosArchivados) { turno ->
                    HistorialItem(turno = turno)
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HistorialItem(turno: Turno) {
    val localDateTime = remember(turno.fechaInstante) {
        LocalDateTime.ofInstant(turno.fechaInstante, ZoneId.systemDefault())
    }
    val formateadorFecha = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val formateadorHora = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    val esCompletado = turno.estado == EstadoTurno.COMPLETADO

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // LADO IZQUIERDO: Hora, Fecha e Icono Muted
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Text(
                    text = localDateTime.format(formateadorHora),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = localDateTime.format(formateadorFecha).uppercase(),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(colorTipo.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconTipo,
                        contentDescription = null,
                        tint = colorTipo.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // LADO DERECHO: Detalles
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag de paciente
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = turno.paciente,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Estado
                    val colorEstado = if (esCompletado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    val textEstado = if (esCompletado) "Completado" else "Cancelado"
                    Text(
                        text = textEstado,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorEstado.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = turno.especialidad,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Text(
                    text = "Lugar: ${turno.lugar}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (esCompletado && !turno.hermanoConfirmado.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Acompañó: ${turno.hermanoConfirmado}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!turno.notasDelMedico.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Nota: ${turno.notasDelMedico}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
