package com.xd.misviejos.domain.model

data class TransaccionFondo(
    val id: String = "",
    val groupId: String = "",
    val tipo: String = "INGRESO",
    val monto: Double = 0.0,
    val titulo: String = "",
    val autorToken: String = "",
    val nombreHermano: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val mes: Int = 0,
    val anio: Int = 0
)
