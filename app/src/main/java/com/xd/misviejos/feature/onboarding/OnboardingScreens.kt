package com.xd.misviejos.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xd.misviejos.domain.model.Familia
import kotlinx.coroutines.launch

// --- ESTADO INTERNO DEL ARCHIVO ---
private enum class PasoOnboarding { BIFURCACION, FUNDAR_WIZARD, SELECCIONAR_HERMANO }

@Composable
fun GeneradorOnboardingRoot(
    onBuscarFamilia: suspend (String) -> Familia?,
    onFamiliaUnida: (codigoGrupo: String, nombreHermano: String, pin: String, esRegistro: Boolean) -> Unit,
    onFamiliaFundada: (codigoGenerado: String, adminNombre: String, pin: String, papa: String, mama: String, hermanos: List<String>) -> Unit
) {
    var vistaActual by remember { mutableStateOf(PasoOnboarding.BIFURCACION) }
    var familiaSeleccionada by remember { mutableStateOf<Familia?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(targetState = vistaActual, label = "transicion_onboarding") { vista ->
                when (vista) {
                    PasoOnboarding.BIFURCACION -> {
                        PantallaBifurcacion(
                            onEntrarConCodigo = { codigo ->
                                scope.launch {
                                    val familia = onBuscarFamilia(codigo)
                                    if (familia != null) {
                                        familiaSeleccionada = familia
                                        vistaActual = PasoOnboarding.SELECCIONAR_HERMANO
                                    } else {
                                        snackbarHostState.showSnackbar("La familia con código $codigo no existe")
                                    }
                                }
                            },
                            onHundirCrearFamilia = { vistaActual = PasoOnboarding.FUNDAR_WIZARD }
                        )
                    }
                    PasoOnboarding.FUNDAR_WIZARD -> {
                        PantallaFundarFamilia(
                            onAtras = { vistaActual = PasoOnboarding.BIFURCACION },
                            onFinalizarCreacion = onFamiliaFundada
                        )
                    }
                    PasoOnboarding.SELECCIONAR_HERMANO -> {
                        PantallaSeleccionarHermano(
                            familia = familiaSeleccionada!!,
                            onAtras = { vistaActual = PasoOnboarding.BIFURCACION },
                            onConfirmar = onFamiliaUnida
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// PANTALLA 1: LA BIFURCACIÓN (El Peaje)
// ============================================================================
@Composable
private fun PantallaBifurcacion(
    onEntrarConCodigo: (String) -> Unit,
    onHundirCrearFamilia: () -> Unit
) {
    var codigoInput by remember { mutableStateOf("") }
    val errorCodigo = codigoInput.isNotEmpty() && codigoInput.length < 5

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
            Text("Nuestros Viejos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
                Text("¿Tu familia ya usa la app?", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = codigoInput,
                    onValueChange = { codigoInput = it.uppercase().trim() },
                    label = { Text("Código de Familia (Ej: MARTINEZ-412)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    singleLine = true,
                    isError = errorCodigo,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onEntrarConCodigo(codigoInput) },
                    enabled = codigoInput.length >= 6,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Entrar a mi tribu", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    Text("Crea el código para tus hermanos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ============================================================================
// PANTALLA 2: EL WIZARD DE FUNDACIÓN (Big Bang)
// ============================================================================
@Composable
private fun PantallaFundarFamilia(
    onAtras: () -> Unit,
    onFinalizarCreacion: (String, String, String, String, String, List<String>) -> Unit
) {
    val scrollState = rememberScrollState()

    // Campos de formulario
    var apellido by remember { mutableStateOf("") }
    var adminNombre by remember { mutableStateOf("") }
    var adminPin by remember { mutableStateOf("") }
    var papaNombre by remember { mutableStateOf("") }
    var mamaNombre by remember { mutableStateOf("") }

    // Hermanos (Dinámicos para permitir cambiar la capacidad de hermanos)
    val hermanosList = remember { mutableStateListOf<String>() }

    // [ TRUCO SENIOR ]: El sufijo numérico nace fijo al abrir la pantalla para que no baile al teclear
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
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Botón atrás
        IconButton(onClick = onAtras, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }

        Text("Crear Familia", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Tú serás el administrador de este espacio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        // --- PASO 1: IDENTIDAD ---
        Text("1. El Apellido de la tribu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = apellido,
            onValueChange = { apellido = it },
            label = { Text("Apellido (Ej: Rodríguez)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        // BANNER DE CÓDIGO EN VIVO
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

        // --- PASO 2: LOS VIEJOS ---
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

        // --- PASO 3: EL ADMINISTRADOR ---
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

        // --- PASO 4: LOS HERMANOS ---
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

        // BOTÓN GIGANTE DE FUNDACIÓN
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

@Composable
private fun PantallaSeleccionarHermano(
    familia: Familia,
    onAtras: () -> Unit,
    onConfirmar: (codigoGrupo: String, nombreHermano: String, pin: String, esRegistro: Boolean) -> Unit
) {
    val perfiles = remember(familia) {
        listOf(familia.adminNombre) + familia.hermanos
    }
    var perfilSeleccionado by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var intentosFallidos by remember { mutableStateOf(0) }

    val tienePin = remember(perfilSeleccionado, familia.pins) {
        perfilSeleccionado != null && (familia.pins.containsKey(perfilSeleccionado) || (perfilSeleccionado == familia.adminNombre && familia.pin.isNotEmpty()))
    }

    val pinCorrecto = remember(perfilSeleccionado, familia) {
        if (perfilSeleccionado == familia.adminNombre) {
            familia.pins[familia.adminNombre] ?: familia.pin
        } else {
            familia.pins[perfilSeleccionado]
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onAtras, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }

        Text("¿Quién eres?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Selecciona tu perfil en la familia", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))

        if (perfilSeleccionado == null) {
            // MOSTRAR LISTA DE PERFILES COMPLETA si no hay selección
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                perfiles.forEach { perfil ->
                    val registrado = familia.pins.containsKey(perfil) || (perfil == familia.adminNombre && familia.pin.isNotEmpty())
                    
                    OutlinedCard(
                        onClick = {
                            perfilSeleccionado = perfil
                            pinInput = ""
                            errorMensaje = null
                            intentosFallidos = 0
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = perfil + if (perfil == familia.adminNombre) " (Admin)" else "",
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = if (registrado) "✓ PIN Configurado" else "🔑 Crear PIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (registrado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else {
            // MOSTRAR EL CHIP DEL PERFIL SELECCIONADO
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    onClick = {
                        perfilSeleccionado = null
                        pinInput = ""
                        errorMensaje = null
                        intentosFallidos = 0
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = perfilSeleccionado!! + if (perfilSeleccionado == familia.adminNombre) " (Admin)" else "",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cambiar selección",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!tienePin) {
                // MENSAJE DE CREACIÓN DE PIN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "El PIN de 4 dígitos que crees ahora será el que uses para iniciar sesión la próxima vez.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // MENSAJE DE INTENTOS RESTANTES
                val intentosRestantes = 3 - intentosFallidos
                Text(
                    text = if (intentosRestantes > 0) "Intentos restantes: $intentosRestantes" else "Acceso bloqueado por intentos fallidos",
                    color = if (intentosRestantes > 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (pinInput.length != 4) {
                        errorMensaje = "El PIN debe ser de 4 dígitos"
                    } else if (tienePin && pinInput != pinCorrecto) {
                        intentosFallidos += 1
                        pinInput = ""
                        errorMensaje = if (intentosFallidos >= 3) {
                            "Acceso bloqueado. Debes contactar al administrador de la familia para que te renueve el PIN."
                        } else {
                            "PIN incorrecto. Inténtalo de nuevo."
                        }
                    } else {
                        val registrarNuevo = !tienePin
                        onConfirmar(familia.groupId, perfilSeleccionado!!, pinInput, registrarNuevo)
                    }
                },
                enabled = pinInput.length == 4 && intentosFallidos < 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (tienePin) "Ingresar" else "Registrar PIN e Ingresar", fontWeight = FontWeight.Bold)
            }
        }
    }
}