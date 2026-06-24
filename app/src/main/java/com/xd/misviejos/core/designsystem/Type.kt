package com.xd.misviejos.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xd.misviejos.R

// <-- Verifica que este import coincida con el paquete de tu app

// 1. Instanciamos la familia uniendo los archivos físicos con su peso lógico
val PlusJakartaSans = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold)
)

// 2. Sobreescribimos el Material 3 Typography con nuestra fuente
val Typography = Typography(

    // Nombres de los papás en el Home (Gigante y contundente)
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),

    // Títulos de las tarjetas y botones grandes
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    ),

    // El texto de las notas del médico
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp, // Darle aire a la lectura de los viejos
        letterSpacing = 0.1.sp
    ),

    // El texto pequeño de "Sugerido por el sistema: Carlos"
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)