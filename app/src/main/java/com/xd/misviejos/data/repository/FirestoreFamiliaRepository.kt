package com.xd.misviejos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.xd.misviejos.domain.model.Familia
import com.xd.misviejos.domain.repository.FamiliaRepository
import kotlinx.coroutines.tasks.await

class FirestoreFamiliaRepository(
    private val firestore: FirebaseFirestore
) : FamiliaRepository {

    private val coleccion = firestore.collection("familias")

    override suspend fun crearFamilia(familia: Familia): Result<Unit> = runCatching {
        val data = mapOf(
            "groupId" to familia.groupId,
            "adminNombre" to familia.adminNombre,
            "pin" to familia.pin,
            "papa" to familia.papa,
            "mama" to familia.mama,
            "hermanos" to familia.hermanos,
            "pins" to familia.pins
        )
        coleccion.document(familia.groupId).set(data).await()
    }

    override suspend fun obtenerFamilia(groupId: String): Result<Familia?> = runCatching {
        val snapshot = coleccion.document(groupId).get().await()
        if (snapshot.exists()) {
            val adminNombre = snapshot.getString("adminNombre") ?: ""
            val pin = snapshot.getString("pin") ?: ""
            val papa = snapshot.getString("papa") ?: ""
            val mama = snapshot.getString("mama") ?: ""
            @Suppress("UNCHECKED_CAST")
            val hermanos = snapshot.get("hermanos") as? List<String> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val pins = snapshot.get("pins") as? Map<String, String> ?: emptyMap()
            Familia(groupId, adminNombre, pin, papa, mama, hermanos, pins)
        } else {
            null
        }
    }

    override suspend fun actualizarPins(groupId: String, pins: Map<String, String>): Result<Unit> = runCatching {
        coleccion.document(groupId).update("pins", pins).await()
    }
}
