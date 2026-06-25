package com.xd.misviejos.domain.model

data class AccessToken(
    val token: String,
    val groupId: String,
    val nombreUsuario: String,
    val rol: String,
    val pin: String?
)
