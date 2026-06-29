package com.xd.misviejos.domain.model

data class Gasto(
    val id: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val fechaRegistro: Long = 0L
)
