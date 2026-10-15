package com.nexo.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.MetaAhorro
import com.nexo.app.model.ProgresoMetaVista
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MetasViewModel (private val sessionManager: SessionManager) : ViewModel() {
    private var userIdReal: Int = 0
    init {
        viewModelScope.launch {
            sessionManager.userIdFlow.collect {id ->
                if (id != 0){
                    userIdReal = id
                    cargarMetas(id)
                }
            }
        }
    }

    var nombreMeta by mutableStateOf("")
        private set
    var montoObjetivo by mutableStateOf("")
        private set
    var fechaLimite by mutableStateOf("")
        private set

    private val _metas = MutableStateFlow<List<ProgresoMetaVista>>(emptyList())
    val metas: StateFlow<List<ProgresoMetaVista>> = _metas

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    fun onNombreMetaChange(value: String) { nombreMeta = value }
    fun onMontoObjetivoChange(value: String) { montoObjetivo = value.filter { it.isDigit() || it == '.' } }
    fun onFechaLimiteChange(value: String) { fechaLimite = value }
    fun mensajeMostrado() { _mensajeUI.value = null }

    private fun limpiarFormulario() {
        nombreMeta = ""
        montoObjetivo = ""
        fechaLimite = ""
    }

    fun cargarMetas(idUsuario: Int) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                _metas.value = client.get("${ApiConfig.METAS_URL}/$idUsuario").body()
            } catch (e: Exception) {
                _mensajeUI.value = "No se pudieron cargar las metas"
                println("Error al cargar metas: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }
    }

    fun registrarMeta() {
        if (userIdReal == 0){
            _mensajeUI.value = "Error de sesión"
            return
        }
        if (nombreMeta.isBlank() || montoObjetivo.isBlank() || fechaLimite.isBlank()) {
            _mensajeUI.value = "Completa todos los campos"
            return
        }

        val monto = montoObjetivo.toDoubleOrNull()
        if (monto == null || monto <= 0.0) {
            _mensajeUI.value = "Monto objetivo inválido"
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            try {
                val nuevaMeta = MetaAhorro(
                    idUsuario = userIdReal,
                    nombreMeta = nombreMeta.trim(),
                    montoObjetivo = monto,
                    fechaLimite = fechaLimite.trim(),
                    activa = true
                )

                val response = client.post(ApiConfig.METAS_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(nuevaMeta)
                }

                if (response.status == HttpStatusCode.Created) {
                    _mensajeUI.value = "Meta registrada con éxito"
                    limpiarFormulario()
                    cargarMetas(userIdReal)
                } else {
                    _mensajeUI.value = "Error al registrar la meta"
                }
            } catch (e: Exception) {
                _mensajeUI.value = "Error de red al registrar meta"
                println("Error registrarMeta: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }
    }
}
