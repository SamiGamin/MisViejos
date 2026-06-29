package com.xd.misviejos.data.model

data class AhorroMensualDTO(
    val ahorroId: String = "",       // ID compuesto: "ROD-4029_6_2026" (GroupId_Mes_Año)
    val groupId: String = "",        // Para consultas
    val mes: Int = 0,                // Ej: 6 (Junio)
    val anio: Int = 0,               // Ej: 2026
    val metaSugerida: Double = 0.0,  // Meta propuesta
    val totalRecolectado: Double = 0.0,
    val aportes: Map<String, Double> = emptyMap(), // Key: nombre de usuario, Value: monto aportado
    val totalGastado: Double = 0.0,
    val gastos: List<GastoDTO> = emptyList()
)
