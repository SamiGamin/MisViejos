package com.xd.misviejos.domain.repository

import com.xd.misviejos.domain.model.Familia

interface FamiliaRepository {
    suspend fun crearFamilia(familia: Familia): Result<Unit>
    suspend fun obtenerFamilia(groupId: String): Result<Familia?>
    suspend fun actualizarPins(groupId: String, pins: Map<String, String>): Result<Unit>
}
