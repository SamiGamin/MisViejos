package com.xd.misviejos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.xd.misviejos.data.mapper.toDTO
import com.xd.misviejos.data.mapper.toDomain
import com.xd.misviejos.data.model.TurnoDTO
import com.xd.misviejos.domain.model.EstadoTurno
import com.xd.misviejos.domain.model.Turno
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreTurnoRepository(
    private val firestore: FirebaseFirestore
) : TurnoRepository {

    // Guardamos todo en una sola colección plana
    private val coleccion = firestore.collection("turnos_viejos")

    override fun obtenerTurnosFamilia(groupId: String): Flow<List<Turno>> = callbackFlow {
        val listener = coleccion
            .whereEqualTo("groupId", groupId)
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val turnos = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TurnoDTO::class.java)?.apply { id = doc.id }?.toDomain()
                    }
                    trySend(turnos)
                }
            }

        // [EL SEGURO DE VIDA]: Cuando el ViewModel se muere, destruye el socket de Firebase
        awaitClose { listener.remove() }
    }

    override suspend fun guardarTurno(turno: Turno): Result<Unit> = runCatching {
        val dto = turno.toDTO()
        val docRef = if (turno.id.isEmpty()) coleccion.document() else coleccion.document(turno.id)

        dto.id = docRef.id
        docRef.set(dto).await() // <-- Requiere el import de kotlinx-coroutines-play-services
    }

    override suspend fun aceptarTurno(turnoId: String, nombreHermano: String): Result<Unit> = runCatching {
        coleccion.document(turnoId).update(
            mapOf(
                "hermanoConfirmado" to nombreHermano,
                "estado" to EstadoTurno.CONFIRMADO.name
            )
        ).await()
    }

    override suspend fun entregarTestigo(turnoId: String, notasDelMedico: String): Result<Unit> = runCatching {
        coleccion.document(turnoId).update(
            mapOf(
                "notasDelMedico" to notasDelMedico,
                "estado" to EstadoTurno.COMPLETADO.name
            )
        ).await()
    }

    override suspend fun sugerirHermano(groupId: String, miembros: List<String>): Result<String> = runCatching {
        if (miembros.isEmpty()) return@runCatching ""
        val snapshot = coleccion.whereEqualTo("groupId", groupId).get().await()
        val conteos = miembros.associateWith { 0 }.toMutableMap()
        snapshot.documents.forEach { doc ->
            val confirmado = doc.getString("hermanoConfirmado")
            if (confirmado != null && conteos.containsKey(confirmado)) {
                conteos[confirmado] = conteos[confirmado]!! + 1
            }
        }
        val sugerido = conteos.minByOrNull { it.value }?.key ?: miembros.first()
        sugerido
    }
}