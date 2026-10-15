package com.nexo.app.ui.viewModel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

@Serializable data class CompromisoRemoto(val id:String,val nombre:String,val importe:String,val fecha:String,val estado:String="PENDIENTE",val movimiento:Int?=null,val version:Int=0)
@Serializable private data class ErrorCompromiso(val mensaje:String)
class CompromisosViewModel(private val sesion:SessionManager,private val usuario:Int):ViewModel() {
    var lista by mutableStateOf<List<CompromisoRemoto>>(emptyList());private set
    var ocupado by mutableStateOf(false);private set
    var listo by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null);private set
    init{if(usuario>0)actualizar()}
    private suspend fun token():String {check(sesion.getUserId()==usuario){"Cambió la cuenta. Vuelve a Inicio."};return sesion.getToken()?:error("Inicia sesión otra vez.")}
    fun actualizar(){
        if(ocupado || usuario<=0)return
        ocupado=true;listo=false;error=null
        viewModelScope.launch{try{
            val r=withTimeout(15000){client.get("${ApiConfig.BASE_URL}/compromisos"){bearerAuth(token())}}
            if(r.status==HttpStatusCode.OK){lista=r.body();listo=true}else error=if(r.status==HttpStatusCode.Unauthorized)"Tu sesión venció. Vuelve a entrar desde Perfil." else "No pudimos consultar tus pagos. Intenta de nuevo."
        }catch(e:Exception){if(e is CancellationException && e !is TimeoutCancellationException)throw e;error="No pudimos actualizar tus pagos. Revisa tu conexión."}finally{ocupado=false}}
    }
    fun guardar(c:CompromisoRemoto,confirmado:()->Unit){
        if(ocupado || !listo)return
        ocupado=true;error=null
        viewModelScope.launch{try{
            val r=withTimeout(15000){client.put("${ApiConfig.BASE_URL}/compromisos"){bearerAuth(token());contentType(ContentType.Application.Json);setBody(c)}}
            if(r.status==HttpStatusCode.OK){val guardado=r.body<CompromisoRemoto>();lista=lista.filterNot{it.id==guardado.id}+guardado;confirmado()}
            else {
                error=runCatching{r.body<ErrorCompromiso>().mensaje}.getOrDefault("No pudimos guardar. Actualiza tus pagos.")
                if(r.status!=HttpStatusCode.BadRequest)listo=false
            }
        }catch(e:Exception){if(e is CancellationException && e !is TimeoutCancellationException)throw e;listo=false;error="No pudimos confirmar el guardado. Cierra el formulario y actualiza antes de repetirlo."}finally{ocupado=false}}
    }
}
