package com.xd.misviejos.feature.timeline

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.core.datastore.UsuarioSesion
import com.xd.misviejos.core.utils.CurrencyVisualTransformation
import com.xd.misviejos.core.utils.rawDigitsToDouble
import com.xd.misviejos.core.utils.toCOP
import com.xd.misviejos.domain.model.AhorroMensual
import com.xd.misviejos.domain.model.TransaccionFondo
import com.xd.misviejos.domain.repository.AhorroRepository
import com.xd.misviejos.domain.repository.FamiliaRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AhorroScreen(
    usuario: UsuarioSesion,
    familiaRepository: FamiliaRepository,
    ahorroRepository: AhorroRepository,
    eliminarModoActivo: Boolean,
    mostrarDialogoCuotaHoisted: Boolean,
    onMostrarDialogoCuotaChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Fecha de visualización (selector central de mes/año)
    var fechaSeleccionada by remember { mutableStateOf(LocalDate.now()) }

    val nombreMes = remember(fechaSeleccionada) {
        fechaSeleccionada.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    // 2. Observar la familia reactivamente (para conocer el saldo global en caja)
    val familiaState by remember(usuario.groupId) {
        familiaRepository.observarFamilia(usuario.groupId)
    }.collectAsState(initial = null)

    // 3. Observar las transacciones de este mes
    val transaccionesState by remember(usuario.groupId, fechaSeleccionada.monthValue, fechaSeleccionada.year) {
        ahorroRepository.observarTransaccionesDelMes(
            groupId = usuario.groupId,
            mes = fechaSeleccionada.monthValue,
            anio = fechaSeleccionada.year
        )
    }.collectAsState(initial = emptyList())

    // Totales calculados en memoria
    val totalRecolectado = remember(transaccionesState) {
        transaccionesState.filter { it.tipo == "INGRESO" }.sumOf { it.monto }
    }
    val totalGastado = remember(transaccionesState) {
        transaccionesState.filter { it.tipo == "GASTO" }.sumOf { it.monto }
    }

    // Integrantes que ya aportaron en el mes seleccionado
    val integrantesQuePagaron = remember(transaccionesState) {
        transaccionesState
            .filter { it.tipo == "INGRESO" && it.nombreHermano != null }
            .map { it.nombreHermano!! }
            .toSet()
    }

    // Lista de integrantes de la familia (para el dropdown de aportes)
    val integrantes = remember(familiaState) {
        if (familiaState != null) {
            listOf(familiaState!!.adminNombre) + familiaState!!.hermanos
        } else {
            emptyList()
        }
    }

    // Diálogos modales
    val mostrarDialogoCuota = mostrarDialogoCuotaHoisted
    var mostrarDialogoAporte by remember { mutableStateOf(false) }
    var mostrarDialogoGasto by remember { mutableStateOf(false) }
    var transaccionAEliminar by remember { mutableStateOf<TransaccionFondo?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- BLOQUE 1: TARJETA SUPERIOR STICKY (Hero Card - Saldo Global) ---
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "SALDO DISPONIBLE TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = (familiaState?.saldo_fondo_actual ?: 0.0).toCOP(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Acumulado histórico real de la familia",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Botones solo para OWNER
                if (usuario.rol == "OWNER") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { mostrarDialogoAporte = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ingreso", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { mostrarDialogoGasto = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gasto", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- BLOQUE 2: SELECTOR DE TIEMPO CENTRAL ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { fechaSeleccionada = fechaSeleccionada.minusMonths(1) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mes Anterior",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "$nombreMes ${fechaSeleccionada.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { fechaSeleccionada = fechaSeleccionada.plusMonths(1) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mes Siguiente",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtítulo con estadísticas del mes seleccionado
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recaudado: ${totalRecolectado.toCOP()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "|",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Gastado: ${totalGastado.toCOP()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Cuota por integrante
            val cuota = familiaState?.cuotaPorIntegrante ?: 0.0
            if (cuota > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Cuota: ${cuota.toCOP()} c/u",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BLOQUE 3: FEED CRONOLÓGICO DE MOVIMIENTOS ---
        Text(
            text = "MOVIMIENTOS DE ${nombreMes.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (transaccionesState.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay movimientos registrados en este mes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(transaccionesState) { tx ->
                    FilaTransaccionItem(
                        tx = tx,
                        eliminarModoActivo = eliminarModoActivo,
                        onDeleteClick = { transaccionAEliminar = tx }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- DIÁLOGO 1: EDITAR CUOTA POR INTEGRANTE (Solo Owner) ---
    if (mostrarDialogoCuota && usuario.rol == "OWNER") {
        var cuotaDigitos by remember {
            val cuotaActual = familiaState?.cuotaPorIntegrante?.toLong() ?: 0L
            mutableStateOf(if (cuotaActual > 0L) cuotaActual.toString() else "")
        }
        val cuotaValorRaw by remember { derivedStateOf { rawDigitsToDouble(cuotaDigitos) } }

        AlertDialog(
            onDismissRequest = { onMostrarDialogoCuotaChange(false) },
            title = {
                Text(
                    text = "Cuota por Integrante",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Define cuánto debe aportar cada integrante al mes. Puedes cambiarlo cuando la familia lo acuerde:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = cuotaDigitos,
                        onValueChange = { newValue ->
                            cuotaDigitos = newValue.filter { it.isDigit() }.trimStart('0')
                                .ifEmpty { if (newValue.any { it.isDigit() }) "0" else "" }
                        },
                        label = { Text("Cuota mensual por persona ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = CurrencyVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            familiaRepository.actualizarCuotaIntegrante(
                                groupId = usuario.groupId,
                                cuota = cuotaValorRaw
                            ).onSuccess {
                                Toast.makeText(context, "Cuota actualizada", Toast.LENGTH_SHORT).show()
                                onMostrarDialogoCuotaChange(false)
                            }.onFailure {
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { onMostrarDialogoCuotaChange(false) }) { Text("Cancelar") }
            }
        )
    }

    // --- DIÁLOGO 2: REGISTRAR APORTE (Solo Owner) ---
    if (mostrarDialogoAporte && usuario.rol == "OWNER" && integrantes.isNotEmpty()) {
        var integranteSeleccionado by remember { mutableStateOf(integrantes.first()) }
        var montoDigitos by remember { mutableStateOf("") }
        val montoValorRaw by remember { derivedStateOf { rawDigitsToDouble(montoDigitos) } }
        var menuExpandido by remember { mutableStateOf(false) }
        var mesRegistro by remember { mutableStateOf(fechaSeleccionada.monthValue) }
        var anioRegistro by remember { mutableStateOf(fechaSeleccionada.year) }
        val mesesNombres = remember {
            listOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
        }
        val rangoAnios = remember { (2024..2030).toList() }

        AlertDialog(
            onDismissRequest = { mostrarDialogoAporte = false },
            title = {
                Text(
                    text = "Registrar Aporte",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Registra el dinero físico aportado por un integrante para el fondo de este mes:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Selector de Integrante (Dropdown)
                    ExposedDropdownMenuBox(
                        expanded = menuExpandido,
                        onExpandedChange = { menuExpandido = !menuExpandido }
                    ) {
                        OutlinedTextField(
                            value = integranteSeleccionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hermano / Integrante") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpandido) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = menuExpandido,
                            onDismissRequest = { menuExpandido = false }
                        ) {
                            integrantes.forEach { integrante ->
                                DropdownMenuItem(
                                    text = { Text(integrante, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        integranteSeleccionado = integrante
                                        menuExpandido = false
                                    }
                                )
                            }
                        }
                    }

                    // Campo de Monto
                    OutlinedTextField(
                        value = montoDigitos,
                        onValueChange = { newValue ->
                            montoDigitos = newValue.filter { it.isDigit() }.trimStart('0').ifEmpty { if (newValue.any { it.isDigit() }) "0" else "" }
                        },
                        label = { Text("Monto a registrar ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = CurrencyVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Selector de Mes y Año (Dropdowns)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dropdown de Mes
                        var menuMesExpandido by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = menuMesExpandido,
                            onExpandedChange = { menuMesExpandido = !menuMesExpandido },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = mesesNombres[mesRegistro - 1],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mes") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMesExpandido) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = menuMesExpandido,
                                onDismissRequest = { menuMesExpandido = false }
                            ) {
                                mesesNombres.forEachIndexed { index, nombre ->
                                    DropdownMenuItem(
                                        text = { Text(nombre, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            mesRegistro = index + 1
                                            menuMesExpandido = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dropdown de Año
                        var menuAnioExpandido by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = menuAnioExpandido,
                            onExpandedChange = { menuAnioExpandido = !menuAnioExpandido },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = anioRegistro.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Año") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAnioExpandido) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = menuAnioExpandido,
                                onDismissRequest = { menuAnioExpandido = false }
                            ) {
                                rangoAnios.forEach { anio ->
                                    DropdownMenuItem(
                                        text = { Text(anio.toString(), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            anioRegistro = anio
                                            menuAnioExpandido = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (montoValorRaw <= 0) {
                            Toast.makeText(context, "Por favor digita un monto válido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val tx = TransaccionFondo(
                                id = "",
                                groupId = usuario.groupId,
                                tipo = "INGRESO",
                                monto = montoValorRaw,
                                titulo = "Aporte de $integranteSeleccionado",
                                autorToken = usuario.nombreUsuario,
                                nombreHermano = integranteSeleccionado,
                                timestamp = System.currentTimeMillis(),
                                mes = mesRegistro,
                                anio = anioRegistro
                            )
                            ahorroRepository.registrarTransaccion(usuario.groupId, tx)
                                .onSuccess {
                                    Toast.makeText(context, "Aporte registrado con éxito", Toast.LENGTH_LONG).show()
                                    mostrarDialogoAporte = false
                                }.onFailure { error ->
                                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAporte = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- DIÁLOGO 3: REGISTRAR GASTO (Solo Owner) ---
    if (mostrarDialogoGasto && usuario.rol == "OWNER") {
        var descripcionInput by remember { mutableStateOf("") }
        var montoGastoDigitos by remember { mutableStateOf("") }
        val montoGastoRaw by remember { derivedStateOf { rawDigitsToDouble(montoGastoDigitos) } }
        var mesGasto by remember { mutableStateOf(fechaSeleccionada.monthValue) }
        var anioGasto by remember { mutableStateOf(fechaSeleccionada.year) }
        val mesesNombres = remember {
            listOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
        }
        val rangoAnios = remember { (2024..2030).toList() }

        AlertDialog(
            onDismissRequest = { mostrarDialogoGasto = false },
            title = {
                Text(
                    text = "Registrar Gasto",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Registra un egreso/gasto realizado desde el fondo común familiar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Concepto del Gasto
                    OutlinedTextField(
                        value = descripcionInput,
                        onValueChange = { descripcionInput = it },
                        label = { Text("Concepto del Gasto") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Monto a registrar
                    OutlinedTextField(
                        value = montoGastoDigitos,
                        onValueChange = { newValue ->
                            montoGastoDigitos = newValue.filter { it.isDigit() }.trimStart('0').ifEmpty { if (newValue.any { it.isDigit() }) "0" else "" }
                        },
                        label = { Text("Monto gastado ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = CurrencyVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Selector de Mes y Año
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dropdown de Mes
                        var menuMesExpandido by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = menuMesExpandido,
                            onExpandedChange = { menuMesExpandido = !menuMesExpandido },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = mesesNombres[mesGasto - 1],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mes") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMesExpandido) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = menuMesExpandido,
                                onDismissRequest = { menuMesExpandido = false }
                            ) {
                                mesesNombres.forEachIndexed { index, nombre ->
                                    DropdownMenuItem(
                                        text = { Text(nombre, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            mesGasto = index + 1
                                            menuMesExpandido = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dropdown de Año
                        var menuAnioExpandido by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = menuAnioExpandido,
                            onExpandedChange = { menuAnioExpandido = !menuAnioExpandido },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = anioGasto.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Año") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAnioExpandido) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = menuAnioExpandido,
                                onDismissRequest = { menuAnioExpandido = false }
                            ) {
                                rangoAnios.forEach { anio ->
                                    DropdownMenuItem(
                                        text = { Text(anio.toString(), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            anioGasto = anio
                                            menuAnioExpandido = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val desc = descripcionInput.trim()
                        if (desc.isEmpty()) {
                            Toast.makeText(context, "Por favor digita un concepto o descripción", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (montoGastoRaw <= 0) {
                            Toast.makeText(context, "Por favor digita un monto válido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val tx = TransaccionFondo(
                                id = "",
                                groupId = usuario.groupId,
                                tipo = "GASTO",
                                monto = montoGastoRaw,
                                titulo = desc,
                                autorToken = usuario.nombreUsuario,
                                timestamp = System.currentTimeMillis(),
                                mes = mesGasto,
                                anio = anioGasto
                            )
                            ahorroRepository.registrarTransaccion(usuario.groupId, tx)
                                .onSuccess {
                                    Toast.makeText(context, "Gasto registrado con éxito", Toast.LENGTH_LONG).show()
                                    mostrarDialogoGasto = false
                                }.onFailure { error ->
                                    if (error.message?.contains("FONDOS_INSUFICIENTES") == true) {
                                        Toast.makeText(context, "El gasto supera el dinero disponible en el fondo", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Error al registrar gasto: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGasto = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- DIÁLOGO 4: CONFIRMAR ELIMINACIÓN DE TRANSACCIÓN (Solo Owner) ---
    transaccionAEliminar?.let { tx ->
        AlertDialog(
            onDismissRequest = { transaccionAEliminar = null },
            title = {
                Text(
                    text = "¿Eliminar movimiento?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de eliminar este movimiento? Se ajustará automáticamente el saldo disponible del fondo por ${tx.monto.toCOP()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            ahorroRepository.eliminarTransaccion(usuario.groupId, tx)
                                .onSuccess {
                                    Toast.makeText(context, "Movimiento eliminado", Toast.LENGTH_SHORT).show()
                                    transaccionAEliminar = null
                                }.onFailure { error ->
                                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { transaccionAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FilaTransaccionItem(
    tx: TransaccionFondo,
    eliminarModoActivo: Boolean,
    onDeleteClick: () -> Unit
) {
    val esIngreso = tx.tipo == "INGRESO"
    val colorIndicador = if (esIngreso) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Indicador circular verde/rojo con icono
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colorIndicador.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (esIngreso) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = colorIndicador,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.titulo,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (esIngreso) "Aporte mensual" else "Gasto del fondo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val signo = if (esIngreso) "+" else "-"
                    Text(
                        text = "$signo ${tx.monto.toCOP()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorIndicador
                    )

                    // Formateador de fecha corto (ej: "12 Jun")
                    val fechaFormateada = remember(tx.timestamp) {
                        val instant = Instant.ofEpochMilli(tx.timestamp)
                        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                        val dia = localDateTime.dayOfMonth
                        val mesNombre = localDateTime.month.getDisplayName(TextStyle.SHORT, Locale("es", "ES"))
                            .replace(".", "")
                            .replaceFirstChar { it.uppercase() }
                        "$dia $mesNombre"
                    }
                    Text(
                        text = fechaFormateada,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (eliminarModoActivo) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar movimiento",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
