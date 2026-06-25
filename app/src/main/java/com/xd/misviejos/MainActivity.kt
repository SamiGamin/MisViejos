package com.xd.misviejos // <-- Ojo: verifica que coincida con tu package real

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import com.xd.misviejos.core.datastore.SessionManager
import com.xd.misviejos.core.datastore.UsuarioSesion
import com.xd.misviejos.core.designsystem.MisViejosTheme
import com.xd.misviejos.core.navigation.TabNav
import com.xd.misviejos.core.updater.AppUpdater
import com.xd.misviejos.core.updater.UpdateInfo
import com.xd.misviejos.data.repository.FirestoreFamiliaRepository
import com.xd.misviejos.data.repository.FirestoreTurnoRepository
import com.xd.misviejos.domain.model.AccessToken
import com.xd.misviejos.domain.model.Familia
import com.xd.misviejos.domain.model.Turno
import com.xd.misviejos.domain.repository.FamiliaRepository
import com.xd.misviejos.feature.onboarding.GeneradorOnboardingRoot
import com.xd.misviejos.feature.timeline.AgendarEventoScreen
import com.xd.misviejos.feature.timeline.AjustesScreen
import com.xd.misviejos.feature.timeline.BitacoraScreen
import com.xd.misviejos.feature.timeline.HistorialScreen
import com.xd.misviejos.feature.timeline.MiTurnoScreen
import com.xd.misviejos.feature.timeline.TimelineScreen
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.combine
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import com.xd.misviejos.core.utils.parseMarkdownToAnnotatedString


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionManager = SessionManager(applicationContext)
        val firestore = FirebaseFirestore.getInstance()
        val familiaRepository = FirestoreFamiliaRepository(firestore)
        setContent {
            val scope = rememberCoroutineScope()
            // Escuchamos el DataStore de forma reactiva
            val darkModePref by sessionManager.darkModeFlow.collectAsState(initial = true)
            
            // Unificamos el cargado del estado inicial del DataStore para evitar parpadeos
            val estadoInicialFlow = remember {
                combine(
                    sessionManager.sesionFlow,
                    sessionManager.lastTokenFlow
                ) { sesion, token ->
                    Pair(sesion, token)
                }
            }
            val estadoInicial by estadoInicialFlow.collectAsState(initial = null)

            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (darkModePref) {
                true -> true
                false -> false
                null -> isSystemDark
            }

            MisViejosTheme(darkTheme = darkTheme) {
                val estado = estadoInicial
                if (estado == null) {
                    // Pantalla de carga mientras se leen las preferencias de sesión
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    val (sesionActual, lastToken) = estado
                    if (sesionActual == null) {
                        GeneradorOnboardingRoot(
                            lastToken = lastToken,
                            onLimpiarLastToken = {
                                scope.launch {
                                    sessionManager.borrarLastToken()
                                }
                            },
                            onBuscarToken = { token ->
                                val res = familiaRepository.obtenerAccessToken(token)
                                res.getOrNull()
                            },
                            onRegistrarPin = { token, pin ->
                                familiaRepository.actualizarAccessTokenPin(token, pin)
                            },
                            onLoginExitoso = { token, groupId, nombreUsuario, rol ->
                                scope.launch {
                                    sessionManager.guardarLastToken(token)
                                    sessionManager.guardarSesion(
                                        UsuarioSesion(
                                            groupId = groupId,
                                            nombreUsuario = nombreUsuario,
                                            rol = rol
                                        )
                                    )
                                }
                            },
                            onFundarFamilia = { codigoGenerado, adminNombre, pin, papa, mama, hermanos ->
                                val nuevaFamilia = Familia(
                                    groupId = codigoGenerado,
                                    adminNombre = adminNombre,
                                    pin = pin,
                                    papa = papa,
                                    mama = mama,
                                    hermanos = hermanos,
                                    pins = mapOf(adminNombre to pin)
                                )
                                familiaRepository.fundarFamiliaConTokens(
                                    grupoId = codigoGenerado,
                                    familia = nuevaFamilia,
                                    hermanosNombres = hermanos
                                )
                            }
                        )
                    } else {
                        // [ CASO B: YA TIENEN LLAVE ] -> Les abrimos el chasis pasándole quiénes son
                        PantallaMaestra(sesionActual, sessionManager, familiaRepository)
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaMaestra(
    usuario: UsuarioSesion,
    sessionManager: SessionManager,
    familiaRepository: FamiliaRepository
) {
    // Memoria de la pestaña pisada (Por defecto arranca en "Mi Turno")
    var pestanaActual by remember { mutableStateOf<TabNav>(TabNav.MiTurno) }
    val items = listOf(TabNav.MiTurno, TabNav.Pista, TabNav.Testigo, TabNav.Archivo, TabNav.Ajustes)
    var mostrandoAgendar by remember { mutableStateOf(false) }
    var turnoAEditar by remember { mutableStateOf<Turno?>(null) }

    // ── Detección automática de actualizaciones al abrir la app ──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updater = remember { AppUpdater(context) }
    var updateDisponible by remember { mutableStateOf<UpdateInfo?>(null) }
    var descargando by remember { mutableStateOf(false) }
    var progresoDescarga by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val update = updater.verificarActualizacion()
        if (update != null) updateDisponible = update
    }

    // Sincronizar el rol del usuario en segundo plano desde Firestore
    LaunchedEffect(usuario) {
        sessionManager.lastTokenFlow.collect { token ->
            if (token != null) {
                val res = familiaRepository.obtenerAccessToken(token)
                res.onSuccess { accessToken ->
                    if (accessToken != null && accessToken.rol != usuario.rol) {
                        sessionManager.guardarSesion(
                            usuario.copy(rol = accessToken.rol)
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo flotante de actualización (aparece sobre cualquier pantalla) ──
    updateDisponible?.let { update ->
        AlertDialog(
            onDismissRequest = { updateDisponible = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
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
                    if (descargando) {
                        LinearProgressIndicator(
                            progress = { progresoDescarga / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Descargando... $progresoDescarga%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!descargando) {
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
                    },
                    enabled = !descargando
                ) {
                    if (descargando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Descargar e Instalar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDisponible = null }) { Text("Ahora no") }
            }
        )
    }

    if (mostrandoAgendar || turnoAEditar != null) {
        val firestore = FirebaseFirestore.getInstance()
        val familiaRepositoryInterno = FirestoreFamiliaRepository(firestore)
        val turnoRepository = FirestoreTurnoRepository(firestore)

        AgendarEventoScreen(
            familiaId = usuario.groupId,
            familiaRepository = familiaRepositoryInterno,
            turnoRepository = turnoRepository,
            turnoAEditar = turnoAEditar,
            onAtras = {
                mostrandoAgendar = false
                turnoAEditar = null
            },
            onEventoAgendado = {
                mostrandoAgendar = false
                turnoAEditar = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (pestanaActual) {
                                TabNav.MiTurno -> "Mi Turno"
                                TabNav.Pista -> "Agenda de Cuidado"
                                TabNav.Testigo -> "Bitácora Médica"
                                TabNav.Archivo -> "Historial de Cuidado"
                                TabNav.Ajustes -> "Ajustes y Preferencias"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (pestanaActual) {
                                TabNav.MiTurno -> "Labores de ${usuario.nombreUsuario}"
                                TabNav.Pista -> "Cronograma familiar"
                                TabNav.Testigo -> "Reportes de los hermanos"
                                TabNav.Archivo -> "Turnos finalizados"
                                TabNav.Ajustes -> "Configuración de la app"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (pestanaActual == TabNav.Pista && (usuario.rol == "OWNER" || usuario.rol == "CO_ADMIN")) {
                        IconButton(
                            onClick = { mostrandoAgendar = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agendar Cuidado",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { tab ->
                        val seleccionado = pestanaActual == tab

                        NavigationBarItem(
                            selected = seleccionado,
                            onClick = { pestanaActual = tab },
                            icon = {
                                Icon(
                                    imageVector = if (seleccionado) tab.iconoSeleccionado else tab.iconoNoSeleccionado,
                                    contentDescription = tab.titulo
                                )
                            },
                            label = {
                                Text(
                                    text = tab.titulo,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                // El color de la "píldora" indicadora de selección: Terracota
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                // El icono dentro de la píldora: Blanco o FondoNoche
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                when (pestanaActual) {
                    TabNav.MiTurno -> {
                        val firestore = FirebaseFirestore.getInstance()
                        val repo = FirestoreTurnoRepository(firestore)

                        MiTurnoScreen(
                            groupId = usuario.groupId,
                            nombreUsuario = usuario.nombreUsuario,
                            turnoRepository = repo
                        )
                    }
                    TabNav.Pista -> {
                        val firestore = FirebaseFirestore.getInstance()
                        val repo = FirestoreTurnoRepository(firestore)

                        TimelineScreen(
                            groupId = usuario.groupId,
                            turnoRepository = repo,
                            isAdmin = usuario.rol == "OWNER" || usuario.rol == "CO_ADMIN",
                            onEditarTurno = { turnoAEditar = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TabNav.Testigo -> {
                        val firestore = FirebaseFirestore.getInstance()
                        val repo = FirestoreTurnoRepository(firestore)

                        BitacoraScreen(
                            groupId = usuario.groupId,
                            turnoRepository = repo
                        )
                    }
                    TabNav.Archivo -> {
                        val firestore = FirebaseFirestore.getInstance()
                        val repo = FirestoreTurnoRepository(firestore)

                        HistorialScreen(
                            groupId = usuario.groupId,
                            turnoRepository = repo
                        )
                    }
                    TabNav.Ajustes -> {
                        AjustesScreen(
                            usuario = usuario,
                            sessionManager = sessionManager,
                            familiaRepository = familiaRepository
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaBautizoPreview() {
    MisViejosTheme {
        PantallaMaestra(
            usuario = UsuarioSesion(
                groupId = "PREVIEW-123",
                nombreUsuario = "Usuario Preview",
                rol = "OWNER"
            ),
            sessionManager = SessionManager(androidx.compose.ui.platform.LocalContext.current),
            familiaRepository = object : FamiliaRepository {
                override suspend fun crearFamilia(familia: Familia) = Result.success(Unit)
                override suspend fun obtenerFamilia(groupId: String) = Result.success(null)
                override suspend fun actualizarPins(groupId: String, pins: Map<String, String>) = Result.success(Unit)
                override suspend fun fundarFamiliaConTokens(
                    grupoId: String,
                    familia: Familia,
                    hermanosNombres: List<String>
                ) = Result.success(emptyList<AccessToken>())

                override suspend fun obtenerAccessToken(token: String) = Result.success(null)
                override suspend fun actualizarAccessTokenPin(token: String, pin: String) = Result.success(Unit)
                override suspend fun obtenerTokensDeFamilia(groupId: String) = Result.success(emptyList<AccessToken>())
                override suspend fun actualizarRolToken(token: String, nuevoRol: String) = Result.success(Unit)
            }
        )
    }
}