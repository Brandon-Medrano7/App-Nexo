package com.nexo.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import android.util.Patterns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import androidx.lifecycle.ViewModel
//--------------------------------
import androidx.lifecycle.viewModelScope
import com.nexo.app.model.Usuario
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistroViewModel : ViewModel() {

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI
    private val _registroExitoso = MutableStateFlow(false)
    val registroExitoso: StateFlow<Boolean> = _registroExitoso

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    //Estados para el formulario
    var nombre = mutableStateOf("")
        private set
    var edad = mutableStateOf("")
        private set
    var correo = mutableStateOf("")
        private set
    var password = mutableStateOf("")
        private set
    var confirmar = mutableStateOf("")
        private set

    //Función para limpiar el mensaje después de mostrarlo
    fun mensajeMostrado(){
        _mensajeUI.value = null
    }
    fun onNombreChange(value: String) {
        nombre.value = value
    }
    //Le agregué esta función de edad
    fun onEdadChange(value: String) {
        val edadLimpia = value.trim()
        if (edadLimpia.isEmpty() || edadLimpia.all { it.isDigit() }){
            edad.value = edadLimpia
        }
    }
    fun onCorreoChange(value: String) {
        correo.value = value
    }
    fun onPasswordChange(value: String) {
        password.value = value
    }
    fun onConfirmarChange(value: String) {
        confirmar.value = value
    }
    fun registrar() {
        if (_cargando.value) return
        _mensajeUI.value = null
        //Validación de campos vacíos
        if (nombre.value.isBlank() || correo.value.isBlank() || password.value.isEmpty()){
            _mensajeUI.value = "Completa tu nombre, correo y contraseña."
            return
        }
        //Validación de coincidencia de contraseñas
        if (password.value != confirmar.value) {
            _mensajeUI.value = "Las contraseñas no coinciden. Revísalas."
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo.value.trim()).matches()) {
            _mensajeUI.value = "Escribe un correo válido, por ejemplo nombre@correo.com."
            return
        }
        val edadNumerica = edad.value.toIntOrNull() ?: 0
        if (edadNumerica <13 || edadNumerica > 120){
            _mensajeUI.value = "Revisa tu edad (13 a 120 años)."
            return
        }

        if (password.value.length !in 12..128) { _mensajeUI.value = "Usa una contraseña de 12 a 128 caracteres."; return }
        if (nombre.value.trim().length > 100 || correo.value.trim().length > 100) { _mensajeUI.value = "Nombre y correo admiten hasta 100 caracteres."; return }
        _cargando.value = true
        //2.- Llamada a la API
        viewModelScope.launch {
            try {
                val nuevoUsuario = Usuario(
                    nombre = nombre.value.trim(),
                    edad = edad.value.toIntOrNull() ?: 0,
                    correo = correo.value.trim(),
                    password = password.value,
                )
                //Se usa el cliente que configuramos antes
                //Cambiar URL por la del servidor


                //Requests==
                val respuesta = withTimeout(15_000) { client.post(ApiConfig.REGISTER_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(nuevoUsuario)
                } }
                //Mensaje en Consola
                println("--RESPUESTA DEL SERVIDOR--")
                println("Status Code: ${respuesta.status}")

                if (respuesta.status.value in 200..299) {
                    println("Usuario registrado en el servidor: ${nombre.value}")
                    //Estado de registro exitoso:
                    _registroExitoso.value = true
                    _mensajeUI.value = "¡Cuenta creada con éxito!"
                }else{
                    _mensajeUI.value = when (respuesta.status.value) {
                        409 -> "Ese correo ya tiene una cuenta. Intenta iniciar sesión."
                        in 400..499 -> "No se pudo crear la cuenta. Revisa los datos."
                        else -> "El servidor no pudo completar el registro. Inténtalo más tarde."
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _mensajeUI.value = "El servidor tardó demasiado. Antes de repetir el registro, intenta iniciar sesión."
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _mensajeUI.value = "No pudimos confirmar el registro. Revisa tu conexión y prueba iniciar sesión antes de repetirlo."
            } catch (e: Exception) {
                _mensajeUI.value = "No pudimos confirmar el registro. Intenta iniciar sesión o vuelve más tarde."
            } finally {
                _cargando.value = false
            }
        }
    }
    //Función para "limpiar" el estado después de navegar
    fun resetRegistroEstado(){
        _registroExitoso.value = false
        password.value=""; confirmar.value=""
    }
}