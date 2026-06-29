package com.xd.misviejos.data.model

data class TransaccionFondoDTO(
    val id: String = "",
    val groupId: String = "",
    val tipo: String = "INGRESO", // "INGRESO" | "GASTO"
    val monto: Double = 0.0,
    val titulo: String = "",      // Concepto o nombre del hermano aportante
    val autorToken: String = "",  // Token de quien generó la transacción
    val nombreHermano: String? = null, // Nombre del hermano (si tipo es INGRESO)
    val timestamp: Long = System.currentTimeMillis(),
    val mes: Int = 0,
    val anio: Int = 0
)
