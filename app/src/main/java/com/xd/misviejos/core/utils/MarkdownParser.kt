package com.xd.misviejos.core.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Parsea un subconjunto básico de Markdown (títulos con #, listas con - o * y negritas con **)
 * a un AnnotatedString de Jetpack Compose para que se renderice correctamente en los componentes Text.
 */
fun parseMarkdownToAnnotatedString(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEachIndexed { index, line ->
            var cleanLine = line.trim()
            
            // Detectar títulos
            var isHeader = false
            if (cleanLine.startsWith("#")) {
                isHeader = true
                cleanLine = cleanLine.replace(Regex("^#+\\s*"), "")
            }
            
            // Detectar listas
            if (cleanLine.startsWith("-") || cleanLine.startsWith("*")) {
                cleanLine = "• " + cleanLine.substring(1).trim()
            }
            
            if (isHeader) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(cleanLine)
                }
            } else {
                // Parsear texto en negrita (**texto**)
                var textToParse = cleanLine
                var boldStart = textToParse.indexOf("**")
                while (boldStart != -1) {
                    append(textToParse.substring(0, boldStart))
                    textToParse = textToParse.substring(boldStart + 2)
                    val boldEnd = textToParse.indexOf("**")
                    if (boldEnd != -1) {
                        val boldText = textToParse.substring(0, boldEnd)
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldText)
                        }
                        textToParse = textToParse.substring(boldEnd + 2)
                    } else {
                        append("**") // Negrita no cerrada
                    }
                    boldStart = textToParse.indexOf("**")
                }
                append(textToParse)
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}
