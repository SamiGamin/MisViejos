package com.xd.misviejos.feature.timeline

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.domain.model.EstadoTurno
import com.xd.misviejos.domain.model.TipoEventoMedico
import com.xd.misviejos.domain.model.Turno
import com.xd.misviejos.domain.repository.FamiliaRepository
import com.xd.misviejos.data.repository.TurnoRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AgendarEventoScreen(
    familiaId: String,
    familiaRepository: FamiliaRepository,
    turnoRepository: TurnoRepository,
    turnoAEditar: Turno? = null,
    onAtras: () -> Unit,
    onEventoAgendado: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Estados de carga e información de familia
    var cargando by remember { mutableStateOf(true) }
    var viejosOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var hermanoSugerido by remember { mutableStateOf("") }
    var todosLosMiembros by remember { mutableStateOf<List<String>>(emptyList()) }
    var usarSugerido by remember { mutableStateOf(turnoAEditar == null) }
    var hermanoAsignadoManual by remember { mutableStateOf(turnoAEditar?.hermanoSugerido ?: "") }

    // Campos comunes
    var tipoSeleccionado by remember { mutableStateOf(turnoAEditar?.tipoEvento ?: TipoEventoMedico.CITA_MEDICA) }
    var pacienteSeleccionado by remember { mutableStateOf(turnoAEditar?.paciente ?: "") }
    var especialidadInput by remember { mutableStateOf(turnoAEditar?.especialidad ?: "") }
    var lugarInput by remember { mutableStateOf(turnoAEditar?.lugar ?: "") }
    var notasInput by remember { mutableStateOf(turnoAEditar?.notasDelMedico ?: "") }

    // Fecha y hora
    var fechaSeleccionada by remember {
        mutableStateOf(
            turnoAEditar?.fechaInstante?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()).toLocalDate() }
                ?: LocalDate.now()
        )
    }
    var horaSeleccionada by remember {
        mutableStateOf(
            turnoAEditar?.fechaInstante?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()).toLocalTime() }
                ?: LocalTime.of(8, 0)
        )
    }

    // Campos específicos
    var doctorInput by remember { mutableStateOf(turnoAEditar?.doctor ?: "") }
    var consultorioInput by remember { mutableStateOf(turnoAEditar?.consultorio ?: "") }
    var dosisInput by remember { mutableStateOf(turnoAEditar?.dosis ?: "") }
    var requisitosInput by remember { mutableStateOf(turnoAEditar?.requisitos ?: "") }
    var documentosInput by remember { mutableStateOf(turnoAEditar?.documentos ?: "") }

    // Formateadores para visualización
    val formateadorFecha = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formateadorHora = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    // Inicializar datos de la familia
    LaunchedEffect(key1 = familiaId) {
        val res = familiaRepository.obtenerFamilia(familiaId)
        res.onSuccess { familia ->
            if (familia != null) {
                val list = mutableListOf<String>()
                if (familia.papa.isNotBlank()) list.add("Papá (${familia.papa})")
                if (familia.mama.isNotBlank()) list.add("Mamá (${familia.mama})")
                viejosOptions = list
                if (list.isNotEmpty()) {
                    pacienteSeleccionado = list.first()
                }

                // Calcular hermano sugerido
                val miembros = listOf(familia.adminNombre) + familia.hermanos
                todosLosMiembros = miembros
                if (miembros.isNotEmpty() && turnoAEditar == null) {
                    hermanoAsignadoManual = familia.adminNombre // Por defecto el admin
                }
                val resSug = turnoRepository.sugerirHermano(familiaId, miembros)
                resSug.onSuccess { sug ->
                    hermanoSugerido = sug
                }
            }
        }.onFailure {
            Toast.makeText(context, "Error al cargar datos familiares: ${it.message}", Toast.LENGTH_LONG).show()
        }
        cargando = false
    }

    // Date & Time pickers
    val datePickerDialog = remember(fechaSeleccionada) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = LocalDate.of(year, month + 1, dayOfMonth)
            },
            fechaSeleccionada.year,
            fechaSeleccionada.monthValue - 1,
            fechaSeleccionada.dayOfMonth
        )
    }

    val timePickerDialog = remember(horaSeleccionada) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                horaSeleccionada = LocalTime.of(hourOfDay, minute)
            },
            horaSeleccionada.hour,
            horaSeleccionada.minute,
            false
        )
    }

    val formularioValido = pacienteSeleccionado.isNotBlank() &&
            especialidadInput.isNotBlank() &&
            lugarInput.isNotBlank()

    Scaffold(
        topBar = {
            OptOutTopBar(onAtras = onAtras)
        }
    ) { innerPadding ->
        if (cargando) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = if (turnoAEditar != null) "Editar Cuidado" else "Agendar Cuidado",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (turnoAEditar != null) "Modifica los datos del evento de cuidado" else "Programa citas, medicamentos, exámenes o autorizaciones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 1. TIPO DE EVENTO (Filter Chips) ---
                Text("1. Selecciona el Tipo de Cuidado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TipoChip(
                            label = "Cita Médica",
                            icon = Icons.Default.MedicalServices,
                            seleccionado = tipoSeleccionado == TipoEventoMedico.CITA_MEDICA,
                            onClick = { tipoSeleccionado = TipoEventoMedico.CITA_MEDICA },
                            modifier = Modifier.weight(1f)
                        )
                        TipoChip(
                            label = "Medicamentos",
                            icon = Icons.Default.Medication,
                            seleccionado = tipoSeleccionado == TipoEventoMedico.RECOGER_MEDICAMENTOS,
                            onClick = { tipoSeleccionado = TipoEventoMedico.RECOGER_MEDICAMENTOS },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TipoChip(
                            label = "Autorizaciones",
                            icon = Icons.Default.Description,
                            seleccionado = tipoSeleccionado == TipoEventoMedico.AUTORIZACION_TRAMITE,
                            onClick = { tipoSeleccionado = TipoEventoMedico.AUTORIZACION_TRAMITE },
                            modifier = Modifier.weight(1f)
                        )
                        TipoChip(
                            label = "Examen Médico",
                            icon = Icons.Default.Science,
                            seleccionado = tipoSeleccionado == TipoEventoMedico.EXAMEN_MEDICO,
                            onClick = { tipoSeleccionado = TipoEventoMedico.EXAMEN_MEDICO },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. PACIENTE (Selector de Viejos) ---
                Text("2. ¿A quién cuidamos?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                if (viejosOptions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Aviso: No has registrado papás o abuelos en la familia. Edita la familia para agregarlos.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viejosOptions.forEach { opcion ->
                            val esSeleccionado = pacienteSeleccionado == opcion
                            OutlinedCard(
                                onClick = { pacienteSeleccionado = opcion },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (esSeleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (esSeleccionado) 2.dp else 1.dp,
                                    color = if (esSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opcion,
                                        fontWeight = FontWeight.Bold,
                                        color = if (esSeleccionado) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 3. CAMPOS DINÁMICOS SEGÚN EL TIPO DE EVENTO ---
                Text("3. Detalles del Cuidado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                // Campo 1: Título o Especialidad
                val labelEspecialidad = when (tipoSeleccionado) {
                    TipoEventoMedico.CITA_MEDICA -> "Especialidad (Ej: Cardiología)"
                    TipoEventoMedico.RECOGER_MEDICAMENTOS -> "Medicamento a reclamar (Ej: Losartán 50mg)"
                    TipoEventoMedico.AUTORIZACION_TRAMITE -> "Tipo de Trámite (Ej: Autorización de Resonancia)"
                    TipoEventoMedico.EXAMEN_MEDICO -> "Nombre del Examen (Ej: Cuadro Hemático)"
                }
                OutlinedTextField(
                    value = especialidadInput,
                    onValueChange = { especialidadInput = it },
                    label = { Text(labelEspecialidad) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo 2: Lugar
                val labelLugar = when (tipoSeleccionado) {
                    TipoEventoMedico.CITA_MEDICA -> "Lugar / Clínica (Ej: Fundación Cardioinfantil)"
                    TipoEventoMedico.RECOGER_MEDICAMENTOS -> "Farmacia / Punto de Entrega (Ej: Audifarma)"
                    TipoEventoMedico.AUTORIZACION_TRAMITE -> "Entidad / EPS (Ej: Sanitas)"
                    TipoEventoMedico.EXAMEN_MEDICO -> "Laboratorio / Sede (Ej: Idime)"
                }
                OutlinedTextField(
                    value = lugarInput,
                    onValueChange = { lugarInput = it },
                    label = { Text(labelLugar) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Campos Específicos por Tipo
                when (tipoSeleccionado) {
                    TipoEventoMedico.CITA_MEDICA -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = doctorInput,
                                onValueChange = { doctorInput = it },
                                label = { Text("Doctor / Especialista") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = consultorioInput,
                                onValueChange = { consultorioInput = it },
                                label = { Text("Consultorio") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                    }
                    TipoEventoMedico.RECOGER_MEDICAMENTOS -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = dosisInput,
                            onValueChange = { dosisInput = it },
                            label = { Text("Dosis e Indicaciones (Ej: 1 tableta cada 12h)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TipoEventoMedico.AUTORIZACION_TRAMITE -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = documentosInput,
                            onValueChange = { documentosInput = it },
                            label = { Text("Documentos requeridos (Ej: Cédula, Orden médica)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TipoEventoMedico.EXAMEN_MEDICO -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = requisitosInput,
                            onValueChange = { requisitosInput = it },
                            label = { Text("Requisitos / Preparación (Ej: Ayuno de 8 horas)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 4. FECHA Y HORA ---
                Text("4. Fecha y Hora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { datePickerDialog.show() },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = fechaSeleccionada.format(formateadorFecha), fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.CalendarToday, contentDescription = "Cambiar fecha", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    OutlinedCard(
                        onClick = { timePickerDialog.show() },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = horaSeleccionada.format(formateadorHora), fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.AccessTime, contentDescription = "Cambiar hora", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 5. NOTAS EXTRA ---
                Text("5. Notas o Recomendaciones", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notasInput,
                    onValueChange = { notasInput = it },
                    label = { Text("Instrucciones adicionales para el hermano") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 6. ASIGNAR RESPONSABLE ---
                Text("6. Asignar Responsable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedCard(
                        onClick = { usarSugerido = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (usarSugerido) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (usarSugerido) 2.dp else 1.dp,
                            color = if (usarSugerido) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (usarSugerido) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sugerido: $hermanoSugerido",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (usarSugerido) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    OutlinedCard(
                        onClick = { usarSugerido = false },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (!usarSugerido) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (!usarSugerido) 2.dp else 1.dp,
                            color = if (!usarSugerido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (!usarSugerido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Asignar Manual",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (!usarSugerido) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (!usarSugerido && todosLosMiembros.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Selecciona el responsable:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        todosLosMiembros.forEach { miembro ->
                            val esSeleccionado = hermanoAsignadoManual == miembro
                            OutlinedCard(
                                onClick = { hermanoAsignadoManual = miembro },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (esSeleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (esSeleccionado) 2.dp else 1.dp,
                                    color = if (esSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = miembro,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (esSeleccionado) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN DE AGENDAR
                Button(
                    onClick = {
                        val combinada = LocalDateTime.of(fechaSeleccionada, horaSeleccionada)
                        val instante = combinada.atZone(ZoneId.systemDefault()).toInstant()
                        val responsableFinal = if (usarSugerido) hermanoSugerido else hermanoAsignadoManual

                        val nuevoTurno = Turno(
                            id = turnoAEditar?.id ?: "",
                            groupId = familiaId,
                            tipoEvento = tipoSeleccionado,
                            paciente = pacienteSeleccionado,
                            especialidad = especialidadInput,
                            lugar = lugarInput,
                            fechaInstante = instante,
                            doctor = doctorInput.takeIf { it.isNotBlank() },
                            consultorio = consultorioInput.takeIf { it.isNotBlank() },
                            medicamento = if (tipoSeleccionado == TipoEventoMedico.RECOGER_MEDICAMENTOS) especialidadInput else null,
                            dosis = dosisInput.takeIf { it.isNotBlank() },
                            requisitos = requisitosInput.takeIf { it.isNotBlank() },
                            documentos = documentosInput.takeIf { it.isNotBlank() },
                            hermanoSugerido = responsableFinal,
                            hermanoConfirmado = turnoAEditar?.hermanoConfirmado,
                            estado = turnoAEditar?.estado ?: EstadoTurno.PENDIENTE,
                            notasDelMedico = notasInput.takeIf { it.isNotBlank() }
                        )

                        scope.launch {
                            val res = turnoRepository.guardarTurno(nuevoTurno)
                            res.onSuccess {
                                val msg = if (turnoAEditar != null) "Cambios guardados exitosamente" else "Evento agendado exitosamente"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                onEventoAgendado()
                            }.onFailure {
                                Toast.makeText(context, "Error al guardar evento: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = formularioValido,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text(
                        text = if (turnoAEditar != null) "Guardar Cambios" else "Agendar Evento Médico",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun TipoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (seleccionado) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptOutTopBar(onAtras: () -> Unit) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onAtras) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}
