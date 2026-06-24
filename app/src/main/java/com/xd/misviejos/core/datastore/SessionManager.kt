package com.xd.misviejos.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. Inyectamos el DataStore como un Singleton nativo del Context de Android
private val Context.dataStore by preferencesDataStore(name = "sesion_nuestros_viejos")

// 2. La entidad limpia que va a viajar por toda la app
data class UsuarioSesion(
    val groupId: String,
    val nombreUsuario: String,
    val isAdmin: Boolean
)

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_GROUP_ID = stringPreferencesKey("key_group_id")
        private val KEY_USER_NAME = stringPreferencesKey("key_user_name")
        private val KEY_IS_ADMIN = booleanPreferencesKey("key_is_admin")
    }

    // El "Río" de datos: Emite null si no hay nadie, o el objeto UsuarioSesion si ya entraron
    val sesionFlow: Flow<UsuarioSesion?> = context.dataStore.data.map { preferencias ->
        val groupId = preferencias[KEY_GROUP_ID] ?: return@map null
        val nombre = preferencias[KEY_USER_NAME] ?: return@map null
        val isAdmin = preferencias[KEY_IS_ADMIN] ?: false

        UsuarioSesion(groupId, nombre, isAdmin)
    }

    suspend fun guardarSesion(usuario: UsuarioSesion) {
        context.dataStore.edit { preferencias ->
            preferencias[KEY_GROUP_ID] = usuario.groupId
            preferencias[KEY_USER_NAME] = usuario.nombreUsuario
            preferencias[KEY_IS_ADMIN] = usuario.isAdmin
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { it.clear() }
    }
}