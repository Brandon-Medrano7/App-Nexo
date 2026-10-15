package com.nexo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexo.app.data.local.SessionManager
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import com.nexo.app.util.mensajeErrorConsulta
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

@Serializable data class ConsultaGuia(val tema:String)
@Serializable data class RespuestaGuia(val titulo:String,val texto:String,val fuente:String="Resumen local",val fecha:String)
@Composable fun AsistenteScreen(navController:NavController,sesion:SessionManager) {
    val scope=rememberCoroutineScope()
    var respuesta by remember{mutableStateOf<RespuestaGuia?>(null)}
    var ocupado by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var seleccion by remember{mutableStateOf("")}
    fun consultar(tema:String){if(ocupado)return;ocupado=true;error=null;respuesta=null;seleccion=tema
        scope.launch {try {
            withTimeout(15000) {
            val r=client.post("${ApiConfig.BASE_URL}/asistente/consulta"){bearerAuth(sesion.getToken().orEmpty());contentType(ContentType.Application.Json);setBody(ConsultaGuia(tema))}
            if(r.status==HttpStatusCode.OK)respuesta=r.body() else error=if(r.status.value==401)"Tu sesión venció. Vuelve a entrar desde Perfil." else "El resumen no está disponible. Vuelve a intentar."
            }
        }catch(e:Exception){if(e is CancellationException && e !is TimeoutCancellationException)throw e;error=mensajeErrorConsulta(e)}finally{ocupado=false}}
    }
    Scaffold(containerColor=NexoFondo) {padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item{TextButton(onClick={navController.popBackStack()}){Text("‹ Volver")};NexoEncabezado("Guía","Respuestas basadas en tus registros")}
            item{CajaNexo {Text("¿Qué quieres revisar?",style=MaterialTheme.typography.titleLarge)
                listOf("presupuesto" to "¿Cuánto queda hasta mi paga?","gastos" to "¿Cómo van mis gastos?","metas" to "¿Cómo van mis metas?","habitos" to "¿Cómo consigo premios?").forEach{(id,texto)->
                    OutlinedButton(onClick={consultar(id)},enabled=!ocupado,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text(texto)}
                }
            }}
            if(ocupado)item{LinearProgressIndicator(modifier=Modifier.fillMaxWidth());Text("Consultando tus registros…",color=NexoTextoSecundario)}
            error?.let{item{NexoAviso("No pudimos preparar la respuesta",it,"Reintentar"){consultar(seleccion)}}}
            respuesta?.let{r->item{CajaNexo{Text(r.titulo,style=MaterialTheme.typography.titleLarge);Text(r.texto,style=MaterialTheme.typography.bodyLarge);HorizontalDivider();Text("${r.fuente} · ${com.nexo.app.util.fechaNexo(r.fecha)}",style=MaterialTheme.typography.labelMedium,color=NexoAcento)}}}
            item{DetalleNexo("Herramientas · Conversor de monedas"){DivisasCard(sesion)}}
            item{Text("Sin IA generativa. No modifica tus registros.",color=NexoTextoSecundario,style=MaterialTheme.typography.bodySmall)}
        }
    }
}
