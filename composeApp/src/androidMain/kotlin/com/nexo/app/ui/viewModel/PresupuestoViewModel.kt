package com.nexo.app.ui.viewModel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.util.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class PresupuestoRemoto(val id: String, val importe: String, val inicio: String, val pago: String, val version: Int = 0)
@Serializable private data class ErrorPresupuesto(val mensaje: String)

class PresupuestoViewModel(private val sesiones: SessionManager, private val usuario: Int): ViewModel() {
    var periodos by mutableStateOf<List<PresupuestoRemoto>>(emptyList()); private set
    var ocupado by mutableStateOf(false); private set
    var cargado by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set
    private val client = HttpClient(OkHttp) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    init { actualizar() }
    private suspend fun token(): String {
        check(sesiones.getUserId() == usuario) { "Cambió la cuenta. Abre de nuevo Inicio." }
        return sesiones.getToken() ?: error("Inicia sesión de nuevo para sincronizar el presupuesto.")
    }
    fun actualizar() {
        if (ocupado) return
        ocupado = true; mensaje = null; cargado = false
        viewModelScope.launch {
            try {
                val respuesta = withTimeout(15_000) { client.get("${ApiConfig.BASE_URL}/presupuestos") { bearerAuth(token()) } }
                if (respuesta.status == HttpStatusCode.OK) {
                    periodos = respuesta.body<List<PresupuestoRemoto>>().sortedByDescending { it.inicio }
                    cargado = true
                } else mensaje = respuesta.body<ErrorPresupuesto>().mensaje
            } catch (e: TimeoutCancellationException) { mensaje = "La consulta tardó demasiado. Vuelve a intentar." }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalStateException) { mensaje = e.message }
            catch (e: Exception) { mensaje = "No pudimos consultar tus periodos. Revisa la conexión y el servidor." }
            finally { ocupado = false }
        }
    }
    fun guardar(periodo: PresupuestoRemoto, confirmado: () -> Unit) {
        if (ocupado || !cargado) return
        ocupado = true; mensaje = null
        viewModelScope.launch {
            try {
                val respuesta = withTimeout(15_000) {
                    client.put("${ApiConfig.BASE_URL}/presupuestos") { bearerAuth(token()); contentType(ContentType.Application.Json); setBody(periodo) }
                }
                if (respuesta.status == HttpStatusCode.OK) {
                    val guardado = respuesta.body<PresupuestoRemoto>()
                    periodos = (periodos.filterNot { it.id == guardado.id } + guardado).sortedByDescending { it.inicio }
                    confirmado()
                } else {
                    mensaje = respuesta.body<ErrorPresupuesto>().mensaje
                    if (respuesta.status == HttpStatusCode.Conflict) cargado = false
                }
            } catch (e: TimeoutCancellationException) { mensaje = "No pudimos confirmar el guardado. Cierra el formulario y actualiza antes de repetirlo."; cargado = false }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalStateException) { mensaje = e.message }
            catch (e: Exception) { mensaje = "No pudimos confirmar el guardado. Cierra el formulario y actualiza antes de repetirlo."; cargado = false }
            finally { ocupado = false }
        }
    }
    override fun onCleared() { client.close(); super.onCleared() }
}
