package com.xd.misviejos.core.utils

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

private val copLocale = Locale("es", "CO")

private val copFormatter: NumberFormat = NumberFormat.getCurrencyInstance(copLocale).apply {
    maximumFractionDigits = 0
}

/**
 * Formatea un valor Double a pesos colombianos (COP), sin decimales.
 * Ej: 50000.0 → "$ 50.000"
 */
fun Double.toCOP(): String = copFormatter.format(this)

/**
 * Formatea un Long (dígitos crudos) a pesos colombianos para los campos de entrada.
 * Ej: 50000L → "$ 50.000"
 */
fun Long.toCOPInput(): String {
    if (this <= 0L) return ""
    return copFormatter.format(this)
}

/**
 * Extrae el valor numérico Double desde una cadena de dígitos puros.
 * Ej: "50000" → 50000.0
 */
fun rawDigitsToDouble(digits: String): Double = digits.toLongOrNull()?.toDouble() ?: 0.0
