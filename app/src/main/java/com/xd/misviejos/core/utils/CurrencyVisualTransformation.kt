package com.xd.misviejos.core.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

/**
 * VisualTransformation que convierte dígitos crudos ("50000") en formato
 * de pesos colombianos ("$ 50.000") en tiempo real, sin alterar el estado
 * subyacente. El cursor se mantiene siempre al final del texto formateado.
 */
class CurrencyVisualTransformation : VisualTransformation {

    private val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val number = digits.toLongOrNull() ?: 0L
        val formatted = if (number == 0L) "" else formatter.format(number)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formatted.length
            override fun transformedToOriginal(offset: Int): Int = digits.length
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
