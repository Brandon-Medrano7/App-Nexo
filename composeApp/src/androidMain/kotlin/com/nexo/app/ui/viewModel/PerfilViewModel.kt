package com.nexo.app.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.PerfilUsuario
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class PerfilViewModel(private val sessionManager: SessionManager) : ViewModel() {
    private var userIdReal: Int = 0

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    init {
        viewModelScope.launch {
            // Buscamos el ID automáticamente al iniciar VM
            val id = sessionManager.userIdFlow.first { it != 0 }
            userIdReal = id
            cargarPerfil(id)
        }
    } // Aquí se cierra el init, pero NO la clase

    fun mensajeMostrado() {
        _mensajeUI.value = null
    }

    fun cargarPerfil(idUsuario: Int) {
        viewModelScope.launch {
            if (idUsuario == 0) return@launch

            _cargando.value = true
            println("DEBUG ANDROID: Iniciando carga para ID: $idUsuario")
            println("DEBUG ANDROID: URL destino: ${ApiConfig.PERFIL_URL}/$idUsuario")

            try {
                // El delay para dar tiempo a la conexión
                delay(500)

                _perfil.value = client.get("${ApiConfig.PERFIL_URL}/$idUsuario").body()
                println("DEBUG ANDROID: Respuesta exitosa: ${_perfil.value?.nombre}")
            } catch (e: Exception) {
                _mensajeUI.value = "No se pudo cargar el perfil"
                println("DEBUG ANDROID: FALLÓ LA PETICIÓN - Mensaje: ${e.message}")
                e.printStackTrace()
            } finally {
                _cargando.value = false
            }
        }
    }
} // <--- ESTA llave debe ser la última del archivo