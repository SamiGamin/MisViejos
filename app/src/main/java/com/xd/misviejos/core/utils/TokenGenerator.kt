package com.xd.misviejos.core.utils

object TokenGenerator {
    fun nuevoToken(nombreUsuario: String): String {
        val limpio = nombreUsuario.trim().filter { it.isLetter() }.uppercase()
        val prefijo = if (limpio.length >= 3) limpio.take(3) else limpio.padEnd(3, 'X')
        // Rango de 3 dígitos fijos (100 a 999)
        val sufijo = (100..999).random() 
        
        return "$prefijo-$sufijo" // Ej: "TOB-714"
    }
}
