package com.xd.misviejos.domain.model

data class Familia(
    val groupId: String,
    val adminNombre: String,
    val pin: String,
    val papa: String,
    val mama: String,
    val hermanos: List<String>,
    val pins: Map<String, String> = emptyMap()
)
