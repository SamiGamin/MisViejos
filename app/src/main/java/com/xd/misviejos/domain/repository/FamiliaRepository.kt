package com.xd.misviejos.domain.repository

import com.xd.misviejos.domain.model.AccessToken
import com.xd.misviejos.domain.model.Familia

interface FamiliaRepository {
    suspend fun crearFamilia(familia: Familia): Result<Unit>
    suspend fun obtenerFamilia(groupId: String): Result<Familia?>
    suspend fun actualizarPins(groupId: String, pins: Map<String, String>): Result<Unit>
    
    suspend fun fundarFamiliaConTokens(
        grupoId: String,
        familia: Familia,
        hermanosNombres: List<String>
    ): Result<List<AccessToken>>

    suspend fun obtenerAccessToken(token: String): Result<AccessToken?>
    suspend fun actualizarAccessTokenPin(token: String, pin: String): Result<Unit>
    suspend fun obtenerTokensDeFamilia(groupId: String): Result<List<AccessToken>>
}
