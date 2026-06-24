package com.xd.misviejos.core.designsystem

import androidx.compose.ui.graphics.Color

// --- MODO CLARO (Día en la casa) ---
val FondoAvena = Color(0xFFF9F6F0)
val TerracotaCalido = Color(0xFFD35A38)
val VerdeEucalipto = Color(0xFF2C5E43)
val AlertaMiel = Color(0xFFE59500)
val TextoCarbon = Color(0xFF1E2220)
val TextoGrisPlomo = Color(0xFF707571)
val SuperficieBento = Color(0xFFFFFFFF)
val SuperficieBentoGris = Color(0xFFEFECE6)

// --- MODO OSCURO (Noche en la botica) ---
val FondoNoche = Color(0xFF141716)           // Gris asfalto profundo.
val SuperficieBentoNoche = Color(0xFF212523) // Un tono por encima del fondo para las Cards.
val SuperficieBentoGrisNoche = Color(0xFF2C312E) // Tarjetas inactivas en oscuro.

// Gemelos calibrados para contraste WCAG en fondo oscuro:
val TerracotaNoche = Color(0xFFE27353)       // Más vivo, para que no se apague sobre el gris.
val VerdeSageNoche = Color(0xFF6B9A7E)       // Un verde salvia suave (el eucalipto de día se fundiría).
val TextoAvena = Color(0xFFEBE6DF)           // Poesía pura: usamos el tono del fondo claro como texto de noche.