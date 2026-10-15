package com.nexo.app.ui.viewModel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.*
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class BienestarViewModel(private val sesion:SessionManager):ViewModel() {
    var estado by mutableStateOf<EstadoNexo?>(null); private set
    var ocupado by mutableStateOf(false); private set
    var mensaje by mutableStateOf<String?>(null); private set
    var pendiente by mutableStateOf<AccionNexo?>(null); private set
    var guardados by mutableIntStateOf(0); private set
    private var usuario=0
    private var trabajo:Job?=null
    init { viewModelScope.launch { sesion.userIdFlow.collect { id ->
        if(id!=usuario) {
            trabajo?.cancel(); usuario=id; estado=null; mensaje=null; ocupado=false; pendiente=null
            if(id>0) {
                pendiente=sesion.accionPendiente(id)?.let { runCatching { Json.decodeFromString<AccionNexo>(it) }.getOrNull() }
                actualizar()
            }
        }
    } } }
    fun actualizar() {
        if(ocupado || usuario<=0) return
        val id=usuario
        trabajo=viewModelScope.launch {
            ocupado=true; mensaje=null
            try {
                val respuesta=withTimeout(15000) { client.get("${ApiConfig.BASE_URL}/bienestar") { bearerAuth(sesion.getToken().orEmpty()) } }
                if(usuario!=id) return@launch
                if(respuesta.status==HttpStatusCode.OK) { estado=respuesta.body(); if(pendiente!=null) mensaje="Hay un cambio pendiente de confirmar. Pulsa Reintentar cambio." }
                else mensaje=errorRespuesta(respuesta.status.value)
            } catch(e:Exception) { if(e is CancellationException && e !is TimeoutCancellationException) throw e; if(usuario==id) mensaje="No pudimos conectar. Verifica tu conexión y vuelve a actualizar." }
            finally { if(usuario==id) ocupado=false }
        }
    }
    private fun errorRespuesta(codigo:Int)=when(codigo) {401->"Tu sesión venció. Cierra sesión y vuelve a entrar.";404->"El servidor necesita la actualización de Metas y Perfil.";else->"No pudimos cargar los datos. Intenta de nuevo."}
    fun enviar(accion:AccionNexo) {
        if(ocupado || usuario<=0) return
        if(pendiente!=null && pendiente!=accion) { mensaje="Confirma el cambio pendiente antes de hacer otro."; return }
        val id=usuario
        trabajo=viewModelScope.launch {
            ocupado=true; mensaje=null
            try {
                sesion.guardarAccionPendiente(id,Json.encodeToString(accion)); pendiente=accion
                val r=withTimeout(15000) { client.post("${ApiConfig.BASE_URL}/bienestar") { bearerAuth(sesion.getToken().orEmpty()); contentType(ContentType.Application.Json); setBody(accion) } }
                if(usuario!=id) return@launch
                if(r.status==HttpStatusCode.OK) {
                    estado=r.body(); sesion.guardarAccionPendiente(id,null); pendiente=null; guardados++; mensaje="Cambio guardado."
                } else if(r.status.value in listOf(400,409)) {
                    mensaje=runCatching { r.body<MensajeNexo>().mensaje }.getOrDefault("Revisa tus datos y actualiza.")
                    sesion.guardarAccionPendiente(id,null); pendiente=null
                } else mensaje=if(r.status.value==401) errorRespuesta(401) else "No pudimos confirmar el cambio. Reintenta: se conserva la misma solicitud para evitar duplicados."
            } catch(e:Exception) { if(e is CancellationException && e !is TimeoutCancellationException) throw e; if(usuario==id) mensaje="Cambio pendiente de confirmar. Reintenta cuando tengas conexión; no lo registres otra vez." }
            finally { if(usuario==id) ocupado=false }
        }
    }
    fun reintentar() { pendiente?.let(::enviar) }
    fun salir(fin:()->Unit) {
        trabajo?.cancel()
        viewModelScope.launch {
            runCatching { withTimeout(3000) { client.post("${ApiConfig.BASE_URL}/bienestar/salir") { bearerAuth(sesion.getToken().orEmpty()) } } }
            sesion.cerrarSesion(); estado=null; pendiente=null; mensaje=null; ocupado=false; fin()
        }
    }
}
