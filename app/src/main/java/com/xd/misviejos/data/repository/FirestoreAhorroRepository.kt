package com.xd.misviejos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.xd.misviejos.data.mapper.toDTO
import com.xd.misviejos.data.mapper.toDomain
import com.xd.misviejos.data.model.AhorroMensualDTO
import com.xd.misviejos.data.model.TransaccionFondoDTO
import com.xd.misviejos.domain.model.AhorroMensual
import com.xd.misviejos.domain.model.TransaccionFondo
import com.xd.misviejos.domain.repository.AhorroRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreAhorroRepository(
    private val firestore: FirebaseFirestore
) : AhorroRepository {

    private val coleccionMetas = firestore.collection("ahorros_mensuales")
    private val coleccionTransacciones = firestore.collection("transacciones_fondo")

    override fun observarAhorroMensual(groupId: String, mes: Int, anio: Int): Flow<AhorroMensual?> = callbackFlow {
        val ahorroId = "${groupId}_${mes}_${anio}"
        val listener = coleccionMetas.document(ahorroId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val dto = snapshot.toObject(AhorroMensualDTO::class.java)
                trySend(dto?.toDomain())
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun guardarMetaSugerida(groupId: String, mes: Int, anio: Int, meta: Double): Result<Unit> = runCatching {
        val ahorroId = "${groupId}_${mes}_${anio}"
        val updates = mapOf(
            "ahorroId" to ahorroId,
            "groupId" to groupId,
            "mes" to mes,
            "anio" to anio,
            "metaSugerida" to meta
        )
        coleccionMetas.document(ahorroId).set(updates, SetOptions.merge()).await()
    }

    override fun observarTransaccionesDelMes(groupId: String, mes: Int, anio: Int): Flow<List<TransaccionFondo>> = callbackFlow {
        val listener = coleccionTransacciones
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("mes", mes)
            .whereEqualTo("anio", anio)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TransaccionFondoDTO::class.java)?.toDomain()
                    }.sortedByDescending { it.timestamp }
                    trySend(lista)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun registrarTransaccion(groupId: String, tx: TransaccionFondo): Result<Unit> = runCatching {
        val refGrupo = firestore.collection("familias").document(groupId)
        val refNuevaTx = coleccionTransacciones.document()
        val dtoTx = tx.copy(id = refNuevaTx.id).toDTO()

        firestore.runTransaction { transaction ->
            // 1. Leer saldo actual del grupo
            val snapshotGrupo = transaction.get(refGrupo)
            val saldoAnterior = snapshotGrupo.getDouble("saldo_fondo_actual") ?: 0.0

            val nuevoSaldo = if (tx.tipo == "INGRESO") {
                saldoAnterior + tx.monto
            } else {
                val rem = saldoAnterior - tx.monto
                if (rem < 0) {
                    throw IllegalStateException("FONDOS_INSUFICIENTES")
                }
                rem
            }

            // 2. Actualizar el saldo maestro (merge para no fallar si el doc aún no existe)
            transaction.set(
                refGrupo,
                mapOf("saldo_fondo_actual" to nuevoSaldo),
                SetOptions.merge()
            )

            // 3. Crear el registro en el historial
            transaction.set(refNuevaTx, dtoTx)
        }.await()
    }

    override suspend fun eliminarTransaccion(groupId: String, tx: TransaccionFondo): Result<Unit> = runCatching {
        val refGrupo = firestore.collection("familias").document(groupId)
        val refTx = coleccionTransacciones.document(tx.id)

        firestore.runTransaction { transaction ->
            // 1. Leer saldo actual del grupo
            val snapshotGrupo = transaction.get(refGrupo)
            val saldoAnterior = snapshotGrupo.getDouble("saldo_fondo_actual") ?: 0.0

            val nuevoSaldo = if (tx.tipo == "INGRESO") {
                saldoAnterior - tx.monto
            } else {
                saldoAnterior + tx.monto
            }

            // 2. Actualizar el saldo maestro
            transaction.set(
                refGrupo,
                mapOf("saldo_fondo_actual" to nuevoSaldo),
                SetOptions.merge()
            )

            // 3. Eliminar el registro en el historial
            transaction.delete(refTx)
        }.await()
    }
}
