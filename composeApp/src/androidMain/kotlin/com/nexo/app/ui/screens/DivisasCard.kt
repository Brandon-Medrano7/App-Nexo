package com.nexo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexo.app.data.local.SessionManager
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import com.nexo.app.util.mensajeErrorConsulta
import com.nexo.app.util.convertirAMxn
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

@Serializable data class CambioDto(val moneda:String,val tasa:String,val fecha:String,val fuente:String="Frankfurter",val destino:String="MXN")
@Composable fun DivisasCard(sesion:SessionManager) {
    var moneda by rememberSaveable{mutableStateOf("USD")}
    var importe by rememberSaveable{mutableStateOf("")}
    var cambio by remember{mutableStateOf<CambioDto?>(null)}
    var ocupado by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    val resultado=cambio?.let{convertirAMxn(importe,it.tasa)}
    CajaNexo {
        Text("Antes de comprar en otra moneda",style=MaterialTheme.typography.titleLarge)
        Text("Calcula una referencia en pesos para una compra o suscripción.",color=NexoTextoSecundario)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            listOf("USD","EUR").forEach{codigo->FilterChip(selected=moneda==codigo,onClick={moneda=codigo;cambio=null;error=null},enabled=!ocupado,label={Text(if(codigo=="USD")"Dólares · USD" else "Euros · EUR")})}
        }
        OutlinedTextField(value=importe,onValueChange={importe=it.take(18)},label={Text("Importe en $moneda")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),isError=importe.isNotBlank() && convertirAMxn(importe,"1")==null,supportingText={Text("Mayor que cero; máximo 2 decimales. Ejemplo: 12.99")})
        Button(enabled=!ocupado && convertirAMxn(importe,"1")!=null,modifier=Modifier.fillMaxWidth(),onClick={
            ocupado=true;error=null;cambio=null
            scope.launch {
                try {
                    withTimeout(12000) {
                    val r=client.get("${ApiConfig.BASE_URL}/herramientas/tipo-cambio"){bearerAuth(sesion.getToken().orEmpty());parameter("moneda",moneda)}
                    if(r.status==HttpStatusCode.OK) {
                        val dto=r.body<CambioDto>()
                        if(dto.moneda!=moneda || dto.destino!="MXN" || convertirAMxn("1",dto.tasa)==null)error="La respuesta no es válida. Intenta de nuevo." else cambio=dto
                    } else error=if(r.status==HttpStatusCode.Unauthorized)"Tu sesión venció. Vuelve a entrar desde Perfil." else "El tipo de cambio no está disponible. Intenta más tarde."
                    }
                }catch(e:Exception){if(e is CancellationException && e !is TimeoutCancellationException)throw e;error=mensajeErrorConsulta(e)}finally{ocupado=false}
            }
        }){Text(if(ocupado)"Consultando…" else if(error!=null)"Reintentar consulta" else "Consultar conversión")}
        if(ocupado)LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        if(resultado!=null) {
            Text("≈ $resultado MXN",style=MaterialTheme.typography.headlineMedium,color=NexoAcento)
            Text("${cambio!!.fuente} · Tipo publicado: ${cambio!!.fecha}",style=MaterialTheme.typography.labelMedium)
        }
        Text("Referencia diaria; tu banco puede aplicar otro tipo y comisiones. No registra gastos ni realiza pagos. Solo enviamos la moneda al proveedor.",style=MaterialTheme.typography.bodySmall,color=NexoTextoSecundario)
    }
}
