package com.xd.misviejos.feature.timeline

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.R
import com.xd.misviejos.core.datastore.SessionManager
import com.xd.misviejos.core.datastore.UsuarioSesion
import com.xd.misviejos.core.updater.AppUpdater
import com.xd.misviejos.core.updater.UpdateInfo
import com.xd.misviejos.domain.model.AccessToken
import com.xd.misviejos.domain.model.Familia
import com.xd.misviejos.domain.repository.FamiliaRepository
import kotlinx.coroutines.launch
import com.xd.misviejos.core.utils.parseMarkdownToAnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    usuario: UsuarioSesion,
    sessionManager: SessionManager,
    familiaRepository: FamiliaRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Obtener la información de versión de la aplicación
    val packageInfo = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"
    val versionCode = packageInfo?.let {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            it.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            it.versionCode.toLong()
        }
    } ?: 1L

    // 1. Estados de preferencias locales
    val darkModePref by sessionManager.darkModeFlow.collectAsState(initial = null)
    val isDarkActive = when (darkModePref) {
        true -> true
        false -> false
        null -> true // modo oscuro por defecto
    }

    // 2. Estado para el diálogo de edición de familia (Admin únicamente)
    var mostrarEditFamilia by remember { mutableStateOf(false) }
    var mostrarInvitaciones by remember { mutableStateOf(false) }
    var familiaOriginal by remember { mutableStateOf<Familia?>(null) }

    // Campos de edición de familia
    var papaInput by remember { mutableStateOf("") }
    var mamaInput by remember { mutableStateOf("") }
    val hermanosList = remember { mutableStateListOf<String>() }
    var nuevoHermanoInput by remember { mutableStateOf("") }
    var tokensList by remember { mutableStateOf<List<AccessToken>>(emptyList()) }

    // Cargar datos de la familia si se abre la edición o invitaciones
    LaunchedEffect(mostrarEditFamilia, mostrarInvitaciones) {
        if (mostrarEditFamilia || mostrarInvitaciones) {
            val res = familiaRepository.obtenerFamilia(usuario.groupId)
            res.onSuccess { familia ->
                if (familia != null) {
                    familiaOriginal = familia
                    papaInput = familia.papa
                    mamaInput = familia.mama
                    hermanosList.clear()
                    hermanosList.addAll(familia.hermanos)

                    // Cargar tokens de la familia
                    val resTokens = familiaRepository.obtenerTokensDeFamilia(usuario.groupId)
                    resTokens.onSuccess { lista ->
                        tokensList = lista
                    }
                }
            }.onFailure {
                Toast.makeText(context, "Error al cargar familia: ${it.message}", Toast.LENGTH_LONG).show()
                mostrarEditFamilia = false
                mostrarInvitaciones = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // --- SECCIÓN 1: PERFIL ACTIVO ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = usuario.nombreUsuario,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (usuario.rol) {
                                "OWNER" -> "Administrador Principal (Owner)"
                                "CO_ADMIN" -> "Co-Administrador"
                                else -> "Hermano / Cuidador"
                            },
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (usuario.rol) {
                                "OWNER" -> MaterialTheme.colorScheme.primary
                                "CO_ADMIN" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Código de la Tribu: ${usuario.groupId}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- SECCIÓN 2: PREFERENCIAS DE UX ---
        Text(
            text = "PREFERENCIAS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkActive) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "Modo Oscuro",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Ajusta la apariencia visual de la app",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isDarkActive,
                    onCheckedChange = { nuevoValor ->
                        scope.launch {
                            sessionManager.guardarModoOscuro(nuevoValor)
                        }
                    }
                )
            }
        }

        // --- SECCIÓN 3: CONTROL DE FAMILIA (ADMIN) ---
        if (usuario.isAdmin) {
            Text(
                text = "CONFIGURACIÓN FAMILIAR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Edita los datos principales de tu tribu, añade nuevos hermanos/cuidadores o actualiza a quiénes cuidan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { mostrarEditFamilia = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar Datos de la Familia", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { mostrarInvitaciones = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Invitar Hermanos (Compartir Pases)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SECCIÓN 4: ACCIONES DE SESIÓN ---
        Text(
            text = "SESIÓN",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = {
                scope.launch {
                    sessionManager.cerrarSesion()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN 5: ACTUALIZACIONES ---
        Text(
            text = "ACTUALIZACIONES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val updater = remember { AppUpdater(context) }
        var buscandoUpdate by remember { mutableStateOf(false) }
        var updateDisponible by remember { mutableStateOf<UpdateInfo?>(null) }
        var descargando by remember { mutableStateOf(false) }
        var progresoDescarga by remember { mutableStateOf(0) }
        var mensajeUpdate by remember { mutableStateOf<String?>(null) }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mostrar barra de progreso si se está descargando
                if (descargando) {
                    Text(
                        text = "Descargando actualización... $progresoDescarga%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progresoDescarga / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    mensajeUpdate?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                buscandoUpdate = true
                                mensajeUpdate = null
                                val update = updater.verificarActualizacion()
                                buscandoUpdate = false
                                if (update != null) {
                                    updateDisponible = update
                                } else {
                                    mensajeUpdate = "✅ La app está actualizada (v$versionName)"
                                }
                            }
                        },
                        enabled = !buscandoUpdate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (buscandoUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscando...")
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscar Actualización", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Diálogo de confirmación de actualización
        updateDisponible?.let { update ->
            AlertDialog(
                onDismissRequest = { updateDisponible = null },
                icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("🎉 Actualización disponible", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Versión ${update.versionName} lista para instalar.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (update.releaseNotes.isNotBlank()) {
                            HorizontalDivider()
                            Text(
                                text = parseMarkdownToAnnotatedString(update.releaseNotes.take(300)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updateCapturado = update
                            updateDisponible = null
                            descargando = true
                            progresoDescarga = 0
                            scope.launch {
                                updater.descargarEInstalar(updateCapturado) { progreso ->
                                    progresoDescarga = progreso
                                }
                                descargando = false
                            }
                        }
                    ) { Text("Descargar e Instalar") }
                },
                dismissButton = {
                    TextButton(onClick = { updateDisponible = null }) { Text("Ahora no") }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Versión $versionName ($versionCode)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // --- DIÁLOGO DE EDICIÓN DE FAMILIA (Bottom Sheet) ---
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (mostrarEditFamilia && familiaOriginal != null) {
        ModalBottomSheet(
            onDismissRequest = { mostrarEditFamilia = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Editar Miembros de la Familia",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Actualiza los papás/abuelos que están bajo cuidado de la tribu:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = papaInput,
                    onValueChange = { papaInput = it },
                    label = { Text("Nombre de Papá / Abuelo") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mamaInput,
                    onValueChange = { mamaInput = it },
                    label = { Text("Nombre de Mamá / Abuela") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Hermanos / Cuidadores Activos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Lista interactiva de hermanos
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // El administrador es obligatorio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (usuario.nombreUsuario == familiaOriginal!!.adminNombre) {
                                "${familiaOriginal!!.adminNombre} (Tú - Dueño)"
                            } else {
                                "${familiaOriginal!!.adminNombre} (Dueño/Owner)"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Lista del resto de hermanos
                    hermanosList.forEach { hermano ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hermano,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val tokenAsociado = tokensList.find { it.nombreUsuario == hermano }
                                if (tokenAsociado != null) {
                                    Text(
                                        text = "Pase: ${tokenAsociado.token} (${if (tokenAsociado.pin == null) "🔑 PIN sin configurar" else "✓ PIN activo"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (tokenAsociado.pin == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    
                                    // Visualización de Co-Admin según rol
                                    if (usuario.rol == "OWNER") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            val esCoAdmin = tokenAsociado.rol == "CO_ADMIN"
                                            Text(
                                                text = "Co-Administrador",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Switch(
                                                checked = esCoAdmin,
                                                onCheckedChange = { check ->
                                                    val nuevoRol = if (check) "CO_ADMIN" else "MEMBER"
                                                    scope.launch {
                                                        val res = familiaRepository.actualizarRolToken(tokenAsociado.token, nuevoRol)
                                                        res.onSuccess {
                                                            tokensList = tokensList.map {
                                                                if (it.token == tokenAsociado.token) it.copy(rol = nuevoRol) else it
                                                            }
                                                            Toast.makeText(context, "Rol de $hermano cambiado a $nuevoRol", Toast.LENGTH_SHORT).show()
                                                        }.onFailure {
                                                            Toast.makeText(context, "Error al actualizar: ${it.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.scale(0.7f)
                                            )
                                        }
                                    } else if (tokenAsociado.rol == "CO_ADMIN") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Co-Administrador",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "(Nuevo - Se creará pase al guardar)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tokenAsociado = tokensList.find { it.nombreUsuario == hermano }
                                if (tokenAsociado != null) {
                                    IconButton(
                                        onClick = { despacharInvitacionWhatsApp(context, hermano, tokenAsociado.token) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Compartir Pase",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (usuario.rol == "OWNER") {
                                    IconButton(
                                        onClick = { hermanosList.remove(hermano) },
                                        modifier = Modifier.size(32.dp)
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
                    }
                }

                // Campo para agregar un nuevo hermano (Solo Owner)
                if (usuario.rol == "OWNER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nuevoHermanoInput,
                            onValueChange = { nuevoHermanoInput = it },
                            placeholder = { Text("Nombre del nuevo hermano") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (nuevoHermanoInput.isNotBlank()) {
                                    val nombreLimpio = nuevoHermanoInput.trim()
                                    if (nombreLimpio == familiaOriginal!!.adminNombre || hermanosList.contains(nombreLimpio)) {
                                        Toast.makeText(context, "Ese nombre ya existe.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        hermanosList.add(nombreLimpio)
                                        nuevoHermanoInput = ""
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Añadir")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { mostrarEditFamilia = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val familiaActualizada = Familia(
                                groupId = familiaOriginal!!.groupId,
                                adminNombre = familiaOriginal!!.adminNombre,
                                pin = familiaOriginal!!.pin,
                                papa = papaInput.trim(),
                                mama = mamaInput.trim(),
                                hermanos = hermanosList.toList(),
                                pins = familiaOriginal!!.pins
                            )
                            scope.launch {
                                val res = familiaRepository.crearFamilia(familiaActualizada)
                                res.onSuccess {
                                    Toast.makeText(context, "Familia actualizada con éxito", Toast.LENGTH_LONG).show()
                                    mostrarEditFamilia = false
                                }.onFailure {
                                    Toast.makeText(context, "Error al guardar cambios: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar Cambios")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- NUEVO DIÁLOGO DE INVITACIONES / COMPARTIR PASES (Bottom Sheet) ---
    val sheetInvitacionesState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (mostrarInvitaciones && familiaOriginal != null) {
        ModalBottomSheet(
            onDismissRequest = { mostrarInvitaciones = false },
            sheetState = sheetInvitacionesState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Compartir Pases de Acceso",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Envía invitaciones a tus hermanos para que puedan entrar a la aplicación directamente con su pase personalizado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                val hermanosTokens = tokensList.filter { it.rol == "HERMANO" }

                if (hermanosTokens.isEmpty()) {
                    Text(
                        text = "Aún no has agregado hermanos. Puedes agregarlos usando la opción 'Editar Datos de la Familia'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        hermanosTokens.forEach { hermanoToken ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hermanoToken.nombreUsuario,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pase: ${hermanoToken.token} (${if (hermanoToken.pin == null) "🔑 PIN sin registrar" else "✓ Registrado"})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hermanoToken.pin == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    Button(
                                        onClick = { despacharInvitacionWhatsApp(context, hermanoToken.nombreUsuario, hermanoToken.token) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compartir", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { mostrarInvitaciones = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun despacharInvitacionWhatsApp(context: Context, nombreHermano: String, tokenAcceso: String) {
    val linkDescarga = "https://github.com/SamiGamin/MisViejos/releases/latest"
    val nombreApp = context.getString(R.string.app_name)
    val texto = """
        👋 ¡Hola $nombreHermano! Te invito a unirte a *$nombreApp*, la app que usamos en familia para coordinar el cuidado de los papás.
        
        📲 *Descarga la app aquí:*
        $linkDescarga
        
        Una vez instalada, ingresa con tu pase único:
        🔑 *Tu Pase: $tokenAcceso*
        
        La app te pedirá crear un PIN personal de 4 dígitos la primera vez. 🔐
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
        setPackage("com.whatsapp")
    }

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        context.startActivity(Intent.createChooser(intent, "Enviar código por..."))
    }
}
