package com.xd.misviejos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.xd.misviejos.core.utils.TokenGenerator
import com.xd.misviejos.data.model.AccessTokenDTO
import com.xd.misviejos.data.mapper.toDomain
import com.xd.misviejos.domain.model.AccessToken
import com.xd.misviejos.domain.model.Familia
import com.xd.misviejos.domain.repository.FamiliaRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreFamiliaRepository(
    private val firestore: FirebaseFirestore
) : FamiliaRepository {

    private val coleccion = firestore.collection("familias")

    override suspend fun crearFamilia(familia: Familia): Result<Unit> = runCatching {
        val batch = firestore.batch()
        
        // 1. Escribir el documento de la familia
        val refFamilia = coleccion.document(familia.groupId)
        val data = mapOf(
            "groupId" to familia.groupId,
            "adminNombre" to familia.adminNombre,
            "pin" to familia.pin,
            "papa" to familia.papa,
            "mama" to familia.mama,
            "hermanos" to familia.hermanos,
            "pins" to familia.pins,
            "isAhorroActivo" to familia.isAhorroActivo
        )
        batch.set(refFamilia, data)

        // 2. Obtener tokens existentes para sincronizar
        val snap = firestore.collection("access_tokens")
            .whereEqualTo("groupId", familia.groupId)
            .get()
            .await()
        val tokensExistentes = snap.toObjects(AccessTokenDTO::class.java)

        val nombresExistentes = tokensExistentes.map { it.nombreUsuario }.toSet()
        val nuevosHermanos = familia.hermanos.filter { it !in nombresExistentes }
        
        val nombresNuevos = (familia.hermanos + familia.adminNombre).toSet()
        val tokensAEliminar = tokensExistentes.filter { it.nombreUsuario !in nombresNuevos }

        // 3. Crear tokens para nuevos hermanos
        nuevosHermanos.filter { it.isNotBlank() }.forEach { nombreHermano ->
            val tokenHermano = TokenGenerator.nuevoToken(nombreHermano)
            val hermanoDTO = AccessTokenDTO(tokenHermano, familia.groupId, nombreHermano, "MEMBER", null)
            val refHermano = firestore.collection("access_tokens").document(tokenHermano)
            batch.set(refHermano, hermanoDTO)
        }

        // 4. Eliminar tokens de hermanos removidos
        tokensAEliminar.forEach { tokenDTO ->
            val refEliminar = firestore.collection("access_tokens").document(tokenDTO.token)
            batch.delete(refEliminar)
        }

        batch.commit().await()
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
            val isAhorroActivo = snapshot.getBoolean("isAhorroActivo") ?: false
            val saldoFondoActual = snapshot.getDouble("saldo_fondo_actual") ?: 0.0
            val cuotaPorIntegrante = snapshot.getDouble("cuotaPorIntegrante") ?: 0.0
            Familia(groupId, adminNombre, pin, papa, mama, hermanos, pins, isAhorroActivo, saldoFondoActual, cuotaPorIntegrante)
        } else {
            null
        }
    }

    override suspend fun actualizarPins(groupId: String, pins: Map<String, String>): Result<Unit> = runCatching {
        coleccion.document(groupId).update("pins", pins).await()
    }

    override suspend fun fundarFamiliaConTokens(
        grupoId: String,
        familia: Familia,
        hermanosNombres: List<String>
    ): Result<List<AccessToken>> = runCatching {
        val batch = firestore.batch()
        val listaTokensGenerados = mutableListOf<AccessToken>()

        // 1. Escribir el documento de la familia
        val refFamilia = coleccion.document(grupoId)
        val dataFamilia = mapOf(
            "groupId" to familia.groupId,
            "adminNombre" to familia.adminNombre,
            "pin" to familia.pin,
            "papa" to familia.papa,
            "mama" to familia.mama,
            "hermanos" to familia.hermanos,
            "pins" to familia.pins,
            "isAhorroActivo" to familia.isAhorroActivo,
            "saldo_fondo_actual" to familia.saldo_fondo_actual,
            "cuotaPorIntegrante" to familia.cuotaPorIntegrante
        )
        batch.set(refFamilia, dataFamilia)

        // 2. Generar y escribir el Token del Administrador (Nace reclamado y con PIN)
        val tokenAdmin = TokenGenerator.nuevoToken(familia.adminNombre)
        val adminDTO = AccessTokenDTO(tokenAdmin, grupoId, familia.adminNombre, "OWNER", familia.pin)
        val refAdmin = firestore.collection("access_tokens").document(tokenAdmin)
        batch.set(refAdmin, adminDTO)
        listaTokensGenerados.add(adminDTO.toDomain())

        // 3. Generar y escribir los Tokens de los hermanos (Nacen vírgenes, pin = null)
        hermanosNombres.filter { it.isNotBlank() }.forEach { nombreHermano ->
            val tokenHermano = TokenGenerator.nuevoToken(nombreHermano)
            val hermanoDTO = AccessTokenDTO(tokenHermano, grupoId, nombreHermano, "MEMBER", null)
            val refHermano = firestore.collection("access_tokens").document(tokenHermano)
            
            batch.set(refHermano, hermanoDTO)
            listaTokensGenerados.add(hermanoDTO.toDomain())
        }

        // Impactar todo en Firestore de forma atómica
        batch.commit().await()

        return@runCatching listaTokensGenerados
    }

    override suspend fun obtenerAccessToken(token: String): Result<AccessToken?> = runCatching {
        val doc = firestore.collection("access_tokens").document(token).get().await()
        if (doc.exists()) {
            doc.toObject(AccessTokenDTO::class.java)!!.toDomain()
        } else {
            null
        }
    }

    override suspend fun actualizarAccessTokenPin(token: String, pin: String): Result<Unit> = runCatching {
        val docRef = firestore.collection("access_tokens").document(token)
        val snap = docRef.get().await()
        if (snap.exists()) {
            val dto = snap.toObject(AccessTokenDTO::class.java)!!
            val batch = firestore.batch()
            // 1. Actualizar el pin en access_tokens
            batch.update(docRef, "pin", pin)
            // 2. Actualizar el pins en la familia correspondiente
            val famRef = coleccion.document(dto.groupId)
            batch.update(famRef, "pins.${dto.nombreUsuario}", pin)

            batch.commit().await()
        } else {
            throw Exception("El token especificado no existe.")
        }
    }

    override suspend fun obtenerTokensDeFamilia(groupId: String): Result<List<AccessToken>> = runCatching {
        val snap = firestore.collection("access_tokens")
            .whereEqualTo("groupId", groupId)
            .get()
            .await()
        snap.documents.map { doc ->
            doc.toObject(AccessTokenDTO::class.java)!!.toDomain()
        }
    }

    override suspend fun actualizarRolToken(token: String, nuevoRol: String): Result<Unit> = runCatching {
        firestore.collection("access_tokens").document(token).update("rol", nuevoRol).await()
    }

    override suspend fun actualizarAhorroActivo(groupId: String, isActivo: Boolean): Result<Unit> = runCatching {
        coleccion.document(groupId).update("isAhorroActivo", isActivo).await()
    }

    override suspend fun actualizarCuotaIntegrante(groupId: String, cuota: Double): Result<Unit> = runCatching {
        coleccion.document(groupId).update("cuotaPorIntegrante", cuota).await()
    }

    override fun observarFamilia(groupId: String): Flow<Familia?> = callbackFlow {
        val listener = coleccion.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val adminNombre = snapshot.getString("adminNombre") ?: ""
                val pin = snapshot.getString("pin") ?: ""
                val papa = snapshot.getString("papa") ?: ""
                val mama = snapshot.getString("mama") ?: ""
                @Suppress("UNCHECKED_CAST")
                val hermanos = snapshot.get("hermanos") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val pins = snapshot.get("pins") as? Map<String, String> ?: emptyMap()
                val isAhorroActivo = snapshot.getBoolean("isAhorroActivo") ?: false
                val saldoFondoActual = snapshot.getDouble("saldo_fondo_actual") ?: 0.0
                val cuotaPorIntegrante = snapshot.getDouble("cuotaPorIntegrante") ?: 0.0
                trySend(Familia(groupId, adminNombre, pin, papa, mama, hermanos, pins, isAhorroActivo, saldoFondoActual, cuotaPorIntegrante))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }
}
