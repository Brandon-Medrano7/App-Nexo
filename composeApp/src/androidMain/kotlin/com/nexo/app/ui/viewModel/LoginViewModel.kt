package com.nexo.app.ui.viewModel


import androidx.compose.runtime.mutableStateOf
import android.util.Patterns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.model.UsuarioLogin
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import io.ktor.client.request.*
import io.ktor.http.contentType
import io.ktor.client.call.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.UsuarioResponse
import com.nexo.app.util.ApiConfig
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginViewModel(private val sessionManager: SessionManager) : ViewModel() {
    var correo by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    //Estado para manejar el exito del login
    private val _loginExitoso = MutableStateFlow(false)
    val loginExitoso: MutableStateFlow<Boolean> = _loginExitoso
    //Estado para controlar el indicador de carga
    private val _cargando = MutableStateFlow(false)
    val cargando : StateFlow<Boolean> = _cargando

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {json(Json{ignoreUnknownKeys = true})}
    }
    //__________________________________________
    //FUNCIONES PARA ACTUALIZAR EL ESTADO DESDE LA SCREEN
    fun onCorreoChange(value: String) {
        correo = value
    }
    fun onPasswordChange(value: String) {
        password = value
    }
    fun resetLoginState() {
        _loginExitoso.value = false
        password=""
    }
    override fun onCleared() {
        client.close()
        super.onCleared()
    }

    fun login() {
        if (_cargando.value) return
        _mensajeUI.value = null
        if (correo.isBlank() || password.isEmpty()) {
            _mensajeUI.value = "Escribe tu correo y contraseña."
            return
        }
        _cargando.value = true
        val correoSolicitud = correo.trim()
        val passwordSolicitud = password
        viewModelScope.launch{
            try {
                println("DEBUG: Iniciando peticion")
                val respuesta = withTimeout(15_000) { client.post(ApiConfig.LOGIN_URL){
                    contentType(ContentType.Application.Json)
                    setBody(UsuarioLogin(correoSolicitud, passwordSolicitud))
                } }
                println("DEBUG: Respuesta del servidor: ${respuesta.status}")

                if (respuesta.status == HttpStatusCode.OK) {
                    //1.- Se lee el contenido tal cual está
                    val usuarioResponse: UsuarioResponse = respuesta.body()
                    val id = usuarioResponse.idUsuario
                    if(id > 0) {
                        sessionManager.saveSession(id, usuarioResponse.token)
                        println("DEBUG: ID $id guardado exitosamente")
                        //Forzamos el cambio al hilo principal para que la UI se entere
                        withContext(Dispatchers.Main){
                            _loginExitoso.value = true
                        }
                    } else {
                        _mensajeUI.value = "El servidor devolvió una cuenta inválida. Inténtalo más tarde."
                    }

                }else{
                    _loginExitoso.value = false //Estado de error en login
                    _mensajeUI.value = when (respuesta.status.value) {
                        401 -> "El correo o la contraseña son incorrectos."
                        429 -> "Demasiados intentos. Espera un momento y vuelve a probar."
                        else -> "El servidor no pudo iniciar la sesión. Inténtalo más tarde."
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _mensajeUI.value = "El servidor tardó demasiado. Vuelve a intentarlo."
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _mensajeUI.value = "No podemos conectar con NEXO. Revisa tu conexión y que el servidor esté encendido."
            } catch (e: Exception) {
                _mensajeUI.value = "No se pudo completar el inicio de sesión. Inténtalo más tarde."
            }finally {
                _cargando.value = false
            }
        }
    }
}