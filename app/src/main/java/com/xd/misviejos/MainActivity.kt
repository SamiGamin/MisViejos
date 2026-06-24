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
import com.xd.misviejos.core.datastore.SessionManager
import com.xd.misviejos.core.datastore.UsuarioSesion
import com.xd.misviejos.core.designsystem.MisViejosTheme
import com.xd.misviejos.core.navigation.TabNav
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionManager = SessionManager(applicationContext)
        setContent {
            MisViejosTheme {
                // Escuchamos el DataStore de forma reactiva
                val sesionActual by sessionManager.sesionFlow.collectAsState(initial = null)

                if (sesionActual == null) {
                    // [ CASO A: NO HAY NADIE LOGUEADO ] -> Mostramos un login falso temporal
                    PantallaLoginFalso(sessionManager)
                } else {
                    // [ CASO B: YA TIENEN LLAVE ] -> Les abrimos el chasis pasándole quiénes son
                    PantallaMaestra(sesionActual!!)
                }
            }
        }
    }
}

@Composable
fun PantallaMaestra(usuario: UsuarioSesion) {
    // Memoria de la pestaña pisada (Por defecto arranca en "Mi Turno")
    var pestanaActual by remember { mutableStateOf<TabNav>(TabNav.MiTurno) }
    val items = listOf(TabNav.MiTurno, TabNav.Pista, TabNav.Testigo, TabNav.Archivo)

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
            Text(
                text = "Cargando módulo:\n[ ${pestanaActual.titulo} ]",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
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
            )
        )
    }
}