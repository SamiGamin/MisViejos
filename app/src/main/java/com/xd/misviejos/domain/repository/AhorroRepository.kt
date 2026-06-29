package com.xd.misviejos.domain.repository

import com.xd.misviejos.domain.model.AhorroMensual
import com.xd.misviejos.domain.model.TransaccionFondo
import kotlinx.coroutines.flow.Flow

interface AhorroRepository {
    fun observarAhorroMensual(groupId: String, mes: Int, anio: Int): Flow<AhorroMensual?>
    suspend fun guardarMetaSugerida(groupId: String, mes: Int, anio: Int, meta: Double): Result<Unit>
    fun observarTransaccionesDelMes(groupId: String, mes: Int, anio: Int): Flow<List<TransaccionFondo>>
    suspend fun registrarTransaccion(groupId: String, tx: TransaccionFondo): Result<Unit>
    suspend fun eliminarTransaccion(groupId: String, tx: TransaccionFondo): Result<Unit>
}
