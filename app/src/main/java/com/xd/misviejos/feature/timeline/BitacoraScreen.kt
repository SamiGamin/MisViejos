package com.xd.misviejos.feature.timeline

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Feed
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
import com.xd.misviejos.domain.model.Turno
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BitacoraScreen(
    groupId: String,
    turnoRepository: TurnoRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 1. Obtener la agenda reactiva
    val agendaState by remember(groupId) {
        turnoRepository.obtenerTurnosFamilia(groupId)
    }.collectAsState(initial = emptyList())

    // 2. Filtrar turnos completados que tengan notas médicas y ordenar por fecha descendente (más nuevos arriba)
    val reportesBitacora = remember(agendaState) {
        agendaState.filter { it.estado == EstadoTurno.COMPLETADO && !it.notasDelMedico.isNullOrBlank() }
            .sortedByDescending { it.fechaInstante }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (reportesBitacora.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Feed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Sin reportes aún",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Las notas médicas de los turnos cerrados por los hermanos aparecerán aquí en tiempo real.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(reportesBitacora) { turno ->
                    BitacoraItem(turno = turno, onShare = { compartirEnWhatsApp(context, turno) })
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun BitacoraItem(
    turno: Turno,
    onShare: () -> Unit
) {
    val localDateTime = remember(turno.fechaInstante) {
        LocalDateTime.ofInstant(turno.fechaInstante, ZoneId.systemDefault())
    }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMMM, yyyy - hh:mm a") }

    val inicialHermano = remember(turno.hermanoConfirmado) {
        turno.hermanoConfirmado?.firstOrNull()?.toString()?.uppercase() ?: "H"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECERA DEL POST: Avatar del hermano, nombre y fecha de cierre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inicialHermano,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${turno.hermanoConfirmado ?: "Hermano"} entregó testigo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = localDateTime.format(formatter),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CUERPO DEL REPORTE: Paciente y Detalles del Evento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = turno.paciente,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "${turno.especialidad} en ${turno.lugar}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // NOTAS MÉDICAS (Estilo Bento Quote)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reporte Médico",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = turno.notasDelMedico ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ACCIÓN: Compartir en WhatsApp
            Button(
                onClick = onShare,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir reporte en WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun compartirEnWhatsApp(context: Context, turno: Turno) {
    val localDateTime = LocalDateTime.ofInstant(turno.fechaInstante, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")
    val texto = """
        *Resumen de Cuidado Médico - Nuestros Viejos* 👵👴
        
        *Paciente:* ${turno.paciente}
        *Evento:* ${turno.especialidad}
        *Lugar:* ${turno.lugar}
        ${if (!turno.doctor.isNullOrBlank()) "*Doctor:* ${turno.doctor}\n" else ""}${if (!turno.consultorio.isNullOrBlank()) "*Consultorio:* ${turno.consultorio}\n" else ""}
        *Fecha/Hora:* ${localDateTime.format(formatter)}
        *Acompañó:* ${turno.hermanoConfirmado ?: "N/A"}
        
        *Reporte del Médico / Notas:*
        "${turno.notasDelMedico}"
        
        _Enviado desde la App Nuestros Viejos_ 📲
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, texto)
        type = "text/plain"
        `package` = "com.whatsapp"
    }
    try {
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        val shareIntent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, texto)
            type = "text/plain"
        }, "Compartir reporte vía")
        context.startActivity(shareIntent)
    }
}
