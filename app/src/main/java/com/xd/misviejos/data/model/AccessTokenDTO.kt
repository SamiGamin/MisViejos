package com.xd.misviejos.data.model

data class AccessTokenDTO(
    val token: String = "",          // Document ID (Ej: "TOB-714")
    val groupId: String = "",        // Referencia al grupo (Ej: "ROD-4029")
    val nombreUsuario: String = "",  // "Tobías Martínez"
    val rol: String = "HERMANO",     // "ADMIN" | "HERMANO"
    val pin: String? = null          // El del Admin nace con "1234", el del hermano nace en null
)
