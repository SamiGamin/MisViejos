package com.xd.misviejos.data.model

data class GastoDTO(
    val gastoId: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val fechaRegistro: Long = 0L
)
