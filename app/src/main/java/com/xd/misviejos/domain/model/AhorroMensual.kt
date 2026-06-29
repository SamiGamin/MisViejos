package com.xd.misviejos.domain.model

data class AhorroMensual(
    val ahorroId: String = "",       // ID compuesto: "ROD-4029_6_2026"
    val groupId: String = "",
    val mes: Int = 0,
    val anio: Int = 0,
    val metaSugerida: Double = 0.0,
    val totalRecolectado: Double = 0.0,
    val aportes: Map<String, Double> = emptyMap(), // Key: nombre de usuario, Value: monto
    val totalGastado: Double = 0.0,
    val gastos: List<Gasto> = emptyList()
)
