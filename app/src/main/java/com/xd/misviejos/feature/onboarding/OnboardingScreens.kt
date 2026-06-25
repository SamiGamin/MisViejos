package com.xd.misviejos.feature.onboarding

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.R
import com.xd.misviejos.domain.model.AccessToken
import com.xd.misviejos.domain.model.Familia
import kotlinx.coroutines.launch

// --- ESTADO INTERNO DEL ARCHIVO ---
private enum class PasoOnboarding {
    CARGANDO,
    BIFURCACION,
    FUNDAR_WIZARD,
    MOSTRAR_TOKENS_GENESIS,
    INGRESAR_PIN,
    REGISTRAR_PIN
}

@Composable
fun GeneradorOnboardingRoot(
    lastToken: String?,
    onLimpiarLastToken: () -> Unit,
    onBuscarToken: suspend (String) -> AccessToken?,
    onRegistrarPin: suspend (String, String) -> Result<Unit>,
    onLoginExitoso: (token: String, groupId: String, nombreUsuario: String, isAdmin: Boolean) -> Unit,
    onFundarFamilia: suspend (codigoGenerado: String, adminNombre: String, pin: String, papa: String, mama: String, hermanos: List<String>) -> Result<List<AccessToken>>
) {
    var vistaActual by remember { 
        mutableStateOf(
            if (lastToken.isNullOrBlank()) PasoOnboarding.BIFURCACION else PasoOnboarding.CARGANDO
        ) 
    }
    var tokenSeleccionado by remember { mutableStateOf<AccessToken?>(null) }
    var tokensGenerados by remember { mutableStateOf<List<AccessToken>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // LaunchedEffect para iniciar sesión automáticamente si existe un lastToken
    LaunchedEffect(lastToken) {
        if (!lastToken.isNullOrBlank()) {
            vistaActual = PasoOnboarding.CARGANDO
            val resToken = onBuscarToken(lastToken)
            if (resToken != null) {
                tokenSeleccionado = resToken
                if (resToken.pin == null) {
                    vistaActual = PasoOnboarding.REGISTRAR_PIN
                } else {
                    vistaActual = PasoOnboarding.INGRESAR_PIN
                }
            } else {
                vistaActual = PasoOnboarding.BIFURCACION
            }
        } else {
            vistaActual = PasoOnboarding.BIFURCACION
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(targetState = vistaActual, label = "transicion_onboarding") { vista ->
                when (vista) {
                    PasoOnboarding.CARGANDO -> {
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
                    }
                    PasoOnboarding.BIFURCACION -> {
                        PantallaBifurcacion(
                            onEntrarConToken = { token ->
                                scope.launch {
                                    vistaActual = PasoOnboarding.CARGANDO
                                    val resToken = onBuscarToken(token)
                                    if (resToken != null) {
                                        tokenSeleccionado = resToken
                                        if (resToken.pin == null) {
                                            vistaActual = PasoOnboarding.REGISTRAR_PIN
                                        } else {
                                            vistaActual = PasoOnboarding.INGRESAR_PIN
                                        }
                                    } else {
                                        vistaActual = PasoOnboarding.BIFURCACION
                                        snackbarHostState.showSnackbar("Pase de invitación inválido o no existe.")
                                    }
                                }
                            },
                            onHundirCrearFamilia = { vistaActual = PasoOnboarding.FUNDAR_WIZARD }
                        )
                    }
                    PasoOnboarding.FUNDAR_WIZARD -> {
                        PantallaFundarFamilia(
                            onAtras = { vistaActual = PasoOnboarding.BIFURCACION },
                            onFinalizarCreacion = { codigoGenerado, adminNombre, pin, papa, mama, hermanos ->
                                scope.launch {
                                    val res = onFundarFamilia(codigoGenerado, adminNombre, pin, papa, mama, hermanos)
                                    res.onSuccess { listaTokens ->
                                        tokensGenerados = listaTokens
                                        vistaActual = PasoOnboarding.MOSTRAR_TOKENS_GENESIS
                                    }.onFailure {
                                        snackbarHostState.showSnackbar("Error al crear familia: ${it.message}")
                                    }
                                }
                            }
                        )
                    }
                    PasoOnboarding.MOSTRAR_TOKENS_GENESIS -> {
                        PantallaMostrarTokensGenesis(
                            tokens = tokensGenerados,
                            onComenzar = {
                                val adminToken = tokensGenerados.find { it.rol == "ADMIN" }
                                if (adminToken != null) {
                                    onLoginExitoso(adminToken.token, adminToken.groupId, adminToken.nombreUsuario, true)
                                }
                            }
                        )
                    }
                    PasoOnboarding.REGISTRAR_PIN -> {
                        PantallaRegistrarPin(
                            token = tokenSeleccionado!!,
                            onAtras = {
                                onLimpiarLastToken()
                                tokenSeleccionado = null
                                vistaActual = PasoOnboarding.BIFURCACION
                            },
                            onConfirmar = { pin ->
                                scope.launch {
                                    val res = onRegistrarPin(tokenSeleccionado!!.token, pin)
                                    res.onSuccess {
                                        onLoginExitoso(tokenSeleccionado!!.token, tokenSeleccionado!!.groupId, tokenSeleccionado!!.nombreUsuario, tokenSeleccionado!!.rol == "ADMIN")
                                    }.onFailure {
                                        snackbarHostState.showSnackbar("Error al registrar PIN: ${it.message}")
                                    }
                                }
                            }
                        )
                    }
                    PasoOnboarding.INGRESAR_PIN -> {
                        PantallaIngresarPin(
                            token = tokenSeleccionado!!,
                            onAtras = {
                                onLimpiarLastToken()
                                tokenSeleccionado = null
                                vistaActual = PasoOnboarding.BIFURCACION
                            },
                            onConfirmar = {
                                onLoginExitoso(tokenSeleccionado!!.token, tokenSeleccionado!!.groupId, tokenSeleccionado!!.nombreUsuario, tokenSeleccionado!!.rol == "ADMIN")
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// PANTALLA 1: LA BIFURCACIÓN (Entrar con Token o Crear Familia)
// ============================================================================
@Composable
private fun PantallaBifurcacion(
    onEntrarConToken: (String) -> Unit,
    onHundirCrearFamilia: () -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    val errorToken = tokenInput.isNotEmpty() && tokenInput.length < 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- CABECERA ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Logística médica familiar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // --- BENTO TOP: YA TENGO CÓDIGO ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("¿Fuiste invitado a una familia?", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it.uppercase().trim() },
                    label = { Text("Ingresa tu Pase (Ej: ABC-714)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    singleLine = true,
                    isError = errorToken,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onEntrarConToken(tokenInput) },
                    enabled = tokenInput.length >= 6,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Ingresar con mi Pase", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- SEPARADOR VISUAL ---
        Text("— o si eres el primero en llegar —", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

        // --- BENTO BOTTOM: FUNDAR ---
        OutlinedCard(
            onClick = onHundirCrearFamilia,
            shape = RoundedCornerShape(24.dp),
            border = CardDefaults.outlinedCardBorder(enabled = true).copy(width = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("Fundar una nueva familia", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Crea el espacio y pases para tus hermanos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ============================================================================
// PANTALLA 2: EL WIZARD DE FUNDACIÓN (Crear Familia)
// ============================================================================
@Composable
private fun PantallaFundarFamilia(
    onAtras: () -> Unit,
    onFinalizarCreacion: (codigoGenerado: String, adminNombre: String, pin: String, papa: String, mama: String, hermanos: List<String>) -> Unit
) {
    val scrollState = rememberScrollState()

    // Campos de formulario
    var apellido by remember { mutableStateOf("") }
    var adminNombre by remember { mutableStateOf("") }
    var adminPin by remember { mutableStateOf("") }
    var papaNombre by remember { mutableStateOf("") }
    var mamaNombre by remember { mutableStateOf("") }

    // Hermanos
    val hermanosList = remember { mutableStateListOf<String>() }

    // El sufijo numérico nace fijo para evitar que baile al teclear
    val sufijoFijo = remember { (1000..9999).random() }

    val codigoGenerado = remember(apellido) {
        val limpio = apellido.trim().filter { it.isLetter() }.uppercase()
        if (limpio.length >= 2) "$limpio-$sufijoFijo" else ""
    }

    val formularioValido = apellido.isNotBlank() && adminNombre.isNotBlank() && adminPin.length == 4 && (papaNombre.isNotBlank() || mamaNombre.isNotBlank())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        IconButton(onClick = onAtras, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }

        Text("Crear Familia", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Tú serás el administrador de este espacio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        Text("1. El Apellido de la tribu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = apellido,
            onValueChange = { apellido = it },
            label = { Text("Apellido (Ej: Rodríguez)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        if (codigoGenerado.isNotEmpty()) {
            Box(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer).border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "ID de tu tribu: $codigoGenerado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("2. ¿A quiénes vamos a cuidar?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = papaNombre,
                onValueChange = { papaNombre = it },
                label = { Text("Papá, Abuelo") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = mamaNombre,
                onValueChange = { mamaNombre = it },
                label = { Text("Mamá, Abuela") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("3. Tus datos de organizador", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = adminNombre,
                onValueChange = { adminNombre = it },
                label = { Text("Tu nombre") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(0.65f)
            )
            OutlinedTextField(
                value = adminPin,
                onValueChange = { if (it.length <= 4) adminPin = it.filter { c -> c.isDigit() } },
                label = { Text("PIN (4 dig)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(0.35f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("4. Los demás hermanos (Opcional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            TextButton(
                onClick = { hermanosList.add("") },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("+ Agregar Hermano", fontWeight = FontWeight.Bold)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            for (index in hermanosList.indices) {
                val hermano = hermanosList[index]
                OutlinedTextField(
                    value = hermano,
                    onValueChange = { nuevoValor -> hermanosList[index] = nuevoValor },
                    label = { Text("Hermano ${index + 2}") },
                    trailingIcon = {
                        IconButton(onClick = { hermanosList.removeAt(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val listaHermanos = hermanosList.filter { it.isNotBlank() }
                onFinalizarCreacion(codigoGenerado, adminNombre, adminPin, papaNombre, mamaNombre, listaHermanos)
            },
            enabled = formularioValido,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("¡Dar vida a esta Familia!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ============================================================================
// PANTALLA 3: MOSTRAR TOKENS GÉNESIS (Códigos para compartir en WhatsApp)
// ============================================================================
@Composable
private fun PantallaMostrarTokensGenesis(
    tokens: List<AccessToken>,
    onComenzar: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val adminToken = remember(tokens) { tokens.find { it.rol == "ADMIN" } }
    val hermanosTokens = remember(tokens) { tokens.filter { it.rol == "HERMANO" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Tribu Creada! 🎉",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Aquí tienes los pases de invitación únicos para tus hermanos. Compártelos por WhatsApp para que puedan ingresar sin contraseñas complicadas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (adminToken != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tu pase como Administrador:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(adminToken.nombreUsuario, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(adminToken.token, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Text(
            text = "Pases para tus hermanos/cuidadores:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            hermanosTokens.forEach { hermano ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hermano.nombreUsuario, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Pase: ${hermano.token}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { despacharInvitacionWhatsApp(context, hermano.nombreUsuario, hermano.token) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir por WhatsApp",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onComenzar,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Entrar a la app", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================================
// PANTALLA 4: REGISTRAR PIN (Para el primer acceso del hermano)
// ============================================================================
@Composable
private fun PantallaRegistrarPin(
    token: AccessToken,
    onAtras: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        IconButton(onClick = onAtras, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }

        Text("Crear tu PIN de acceso", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Hola ${token.nombreUsuario}, configura un PIN de 4 dígitos para proteger tu cuenta.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Este PIN será tu contraseña personal para acceder a la aplicación la próxima vez que ingreses con tu pase.",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = { newValue ->
                if (newValue.length <= 4) {
                    pinInput = newValue.filter { it.isDigit() }
                    errorMensaje = null
                }
            },
            label = { Text("Nuevo PIN (4 dígitos)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = errorMensaje != null,
            supportingText = {
                errorMensaje?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (pinInput.length != 4) {
                    errorMensaje = "El PIN debe ser exactamente de 4 dígitos"
                } else {
                    onConfirmar(pinInput)
                }
            },
            enabled = pinInput.length == 4,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Registrar PIN e Ingresar", fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================================
// PANTALLA 5: INGRESAR PIN (Login para pases ya registrados)
// ============================================================================
@Composable
private fun PantallaIngresarPin(
    token: AccessToken,
    onAtras: () -> Unit,
    onConfirmar: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var intentosFallidos by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        IconButton(onClick = onAtras, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }

        Text("Ingresar PIN", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Hola ${token.nombreUsuario}, digita tu PIN de seguridad de 4 dígitos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        val intentosRestantes = 3 - intentosFallidos
        Text(
            text = if (intentosRestantes > 0) "Intentos restantes: $intentosRestantes" else "Acceso bloqueado por intentos fallidos",
            color = if (intentosRestantes > 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && intentosFallidos < 3) {
                    pinInput = newValue.filter { it.isDigit() }
                    errorMensaje = null
                }
            },
            label = { Text("PIN de seguridad") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            enabled = intentosFallidos < 3,
            isError = errorMensaje != null || intentosFallidos >= 3,
            supportingText = {
                errorMensaje?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (pinInput.length != 4) {
                    errorMensaje = "El PIN debe ser de 4 dígitos"
                } else if (pinInput != token.pin) {
                    intentosFallidos += 1
                    pinInput = ""
                    errorMensaje = if (intentosFallidos >= 3) {
                        "Acceso bloqueado. Contacta al administrador para que reinicie tu PIN."
                    } else {
                        "PIN incorrecto. Inténtalo de nuevo."
                    }
                } else {
                    onConfirmar()
                }
            },
            enabled = pinInput.length == 4 && intentosFallidos < 3,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Ingresar", fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================================
// FUNCIONES AUXILIARES
// ============================================================================
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
        // Fallback
        context.startActivity(Intent.createChooser(intent, "Enviar código por..."))
    }
}