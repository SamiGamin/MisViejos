package com.xd.misviejos.domain.model

data class Familia(
    val groupId: String,
    val adminNombre: String,
    val pin: String,
    val papa: String,
    val mama: String,
    val hermanos: List<String>,
    val pins: Map<String, String> = emptyMap(),
    val isAhorroActivo: Boolean = false,
    val saldo_fondo_actual: Double = 0.0,
    val cuotaPorIntegrante: Double = 0.0
)
