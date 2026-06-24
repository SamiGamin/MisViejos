package com.xd.misviejos // <-- Ojo: verifica que coincida con tu package real

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.xd.misviejos.core.datastore.SessionManager
import com.xd.misviejos.core.datastore.UsuarioSesion
import com.xd.misviejos.core.designsystem.MisViejosTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.xd.misviejos.core.navigation.TabNav
import com.xd.misviejos.feature.onboarding.GeneradorOnboardingRoot
import com.xd.misviejos.domain.model.Familia
import com.xd.misviejos.data.repository.FirestoreFamiliaRepository
import com.xd.misviejos.data.repository.FirestoreTurnoRepository
import com.xd.misviejos.feature.timeline.AgendarEventoScreen
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionManager = SessionManager(applicationContext)
        val firestore = FirebaseFirestore.getInstance()
        val familiaRepository = FirestoreFamiliaRepository(firestore)
        setContent {
            MisViejosTheme {
                val scope = rememberCoroutineScope()
                // Escuchamos el DataStore de forma reactiva
                val sesionActual by sessionManager.sesionFlow.collectAsState(initial = null)

                if (sesionActual == null) {
                    GeneradorOnboardingRoot(
                        onBuscarFamilia = { codigoGrupo ->
                            val res = familiaRepository.obtenerFamilia(codigoGrupo)
                            res.getOrNull()
                        },
                        onFamiliaUnida = { codigoGrupo, nombreHermano, pin, esRegistro ->
                            scope.launch {
                                val resFamilia = familiaRepository.obtenerFamilia(codigoGrupo)
                                resFamilia.onSuccess { familia ->
                                    if (familia != null) {
                                        val esAdmin = nombreHermano == familia.adminNombre
                                        if (esRegistro) {
                                            val nuevosPins = familia.pins.toMutableMap().apply {
                                                put(nombreHermano, pin)
                                            }
                                            val resUpdate = familiaRepository.actualizarPins(codigoGrupo, nuevosPins)
                                            resUpdate.onSuccess {
                                                sessionManager.guardarSesion(
                                                    UsuarioSesion(
                                                        groupId = codigoGrupo,
                                                        nombreUsuario = nombreHermano,
                                                        isAdmin = esAdmin
                                                    )
                                                )
                                            }.onFailure {
                                                Toast.makeText(applicationContext, "Error al registrar PIN: ${it.message}", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            sessionManager.guardarSesion(
                                                UsuarioSesion(
                                                    groupId = codigoGrupo,
                                                    nombreUsuario = nombreHermano,
                                                    isAdmin = esAdmin
                                                )
                                            )
                                        }
                                    } else {
                                        Toast.makeText(applicationContext, "La familia $codigoGrupo no existe", Toast.LENGTH_LONG).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(applicationContext, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onFamiliaFundada = { codigoGenerado, adminNombre, pin, papa, mama, hermanos ->
                            scope.launch {
                                val nuevaFamilia = Familia(
                                    groupId = codigoGenerado,
                                    adminNombre = adminNombre,
                                    pin = pin,
                                    papa = papa,
                                    mama = mama,
                                    hermanos = hermanos,
                                    pins = mapOf(adminNombre to pin)
                                )
                                val res = familiaRepository.crearFamilia(nuevaFamilia)
                                res.onSuccess {
                                    sessionManager.guardarSesion(
                                        UsuarioSesion(
                                            groupId = codigoGenerado,
                                            nombreUsuario = adminNombre,
                                            isAdmin = true
                                        )
                                    )
                                }.onFailure {
                                    Toast.makeText(applicationContext, "Error al guardar familia: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                } else {
                    // [ CASO B: YA TIENEN LLAVE ] -> Les abrimos el chasis pasándole quiénes son
                    PantallaMaestra(sesionActual!!, sessionManager)
                }
            }
        }
    }
}

@Composable
fun PantallaMaestra(usuario: UsuarioSesion, sessionManager: SessionManager) {
    // Memoria de la pestaña pisada (Por defecto arranca en "Mi Turno")
    var pestanaActual by remember { mutableStateOf<TabNav>(TabNav.MiTurno) }
    val items = listOf(TabNav.MiTurno, TabNav.Pista, TabNav.Testigo, TabNav.Archivo)
    var mostrandoAgendar by remember { mutableStateOf(false) }

    if (mostrandoAgendar) {
        val firestore = FirebaseFirestore.getInstance()
        val familiaRepository = FirestoreFamiliaRepository(firestore)
        val turnoRepository = FirestoreTurnoRepository(firestore)

        AgendarEventoScreen(
            familiaId = usuario.groupId,
            familiaRepository = familiaRepository,
            turnoRepository = turnoRepository,
            onAtras = { mostrandoAgendar = false },
            onEventoAgendado = { mostrandoAgendar = false }
        )
    } else {
        Scaffold(
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

            // [HABITÁCULO TEMPORAL]: Un Box que cambia su texto según la pestaña que toques
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Cargando módulo:\n[ ${pestanaActual.titulo} ]\nUsuario: ${usuario.nombreUsuario} (${usuario.groupId})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    if (pestanaActual == TabNav.Pista && usuario.isAdmin) {
                        Button(
                            onClick = { mostrandoAgendar = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agendar Evento Médico")
                        }
                    }

                    val scope = rememberCoroutineScope()
                    Button(
                        onClick = {
                            scope.launch {
                                sessionManager.cerrarSesion()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar Sesión")
                    }
                }
            }
        }
    }
}
@Composable
fun PantallaLoginFalso(sessionManager: SessionManager) {
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = {
            scope.launch {
                // Simulamos que Wendy digita su código y entra
                sessionManager.guardarSesion(
                    UsuarioSesion(
                        groupId = "MARTINEZ-2026",
                        nombreUsuario = "Wendy",
                        isAdmin = true
                    )
                )
            }
        }) {
            Text("Simular entrada de: [ Wendy (Admin) ]")
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
                isAdmin = true
            ),
            sessionManager = SessionManager(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}