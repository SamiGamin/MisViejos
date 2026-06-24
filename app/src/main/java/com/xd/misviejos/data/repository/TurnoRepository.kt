package com.xd.misviejos.data.repository

import com.xd.misviejos.domain.model.Turno
import kotlinx.coroutines.flow.Flow

interface TurnoRepository {
    fun obtenerTurnosFamilia(groupId: String): Flow<List<Turno>>
    suspend fun guardarTurno(turno: Turno): Result<Unit>
    suspend fun aceptarTurno(groupId: String, turnoId: String, nombreHermano: String): Result<Unit>
    suspend fun entregarTestigo(groupId: String, turnoId: String, notasDelMedico: String): Result<Unit>
    suspend fun sugerirHermano(groupId: String, miembros: List<String>): Result<String>
    suspend fun eliminarTurno(groupId: String, turnoId: String): Result<Unit>
}