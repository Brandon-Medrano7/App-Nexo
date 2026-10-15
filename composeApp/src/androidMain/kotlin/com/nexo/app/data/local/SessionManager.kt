package com.nexo.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("settings")

class SessionManager(private val context: Context) {
    companion object {
        val TOKEN = stringPreferencesKey("session_token")
        val USER_ID = intPreferencesKey("user_id")
    }

    // Leemos el ID. Si no existe, devolvemos 0.
    val userIdFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID] ?: 0
        }

    // Guardamos el ID que obtuviste del Login exitoso
    suspend fun saveUserId(id: Int) {
        context.dataStore.edit { settings ->
            settings[USER_ID] = id
        }
    }
    suspend fun saveSession(id: Int, token: String?) {
        context.dataStore.edit { it[USER_ID] = id; if (token == null) it.remove(TOKEN) else it[TOKEN] = token }
    }
    suspend fun getToken(): String? = context.dataStore.data.first()[TOKEN]
    suspend fun getUserId(): Int{
        return context.dataStore.data.map { it[USER_ID] ?: 0 }.first()
    }
    suspend fun accionPendiente(id:Int):String? = context.dataStore.data.first()[stringPreferencesKey("accion_pendiente_$id")]
    suspend fun guardarAccionPendiente(id:Int, valor:String?) {
        context.dataStore.edit { val key=stringPreferencesKey("accion_pendiente_$id"); if(valor==null) it.remove(key) else it[key]=valor }
    }
    suspend fun cerrarSesion() { context.dataStore.edit { it.remove(USER_ID); it.remove(TOKEN) } }
    suspend fun movimientoPendiente(id:Int):String? = context.dataStore.data.first()[stringPreferencesKey("movimiento_pendiente_$id")]
    suspend fun guardarMovimientoPendiente(id:Int, valor:String?) {
        context.dataStore.edit { val key=stringPreferencesKey("movimiento_pendiente_$id"); if(valor==null) it.remove(key) else it[key]=valor }
    }
}