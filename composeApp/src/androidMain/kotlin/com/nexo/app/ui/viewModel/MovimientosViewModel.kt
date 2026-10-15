package com.nexo.app.ui.viewModel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.Movimiento
import com.nexo.app.model.MensajeNexo
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Serializable data class MovimientoPendiente(val solicitud:String,val movimiento:Movimiento)
class MovimientosViewModel(private val sessionManager:SessionManager):ViewModel() {
    var errorRegistro by mutableStateOf<String?>(null);private set
    var errorCarga by mutableStateOf<String?>(null);private set
    var usuarioActual by mutableIntStateOf(0);private set
    var datosListos by mutableStateOf(false);private set
    var saldoTotal by mutableDoubleStateOf(0.0);private set
    var descripcionInput by mutableStateOf("");private set
    var montoInput by mutableStateOf("");private set
    var categoriaSeleccionada by mutableStateOf("");private set
    var esIngresoInput by mutableStateOf(false);private set
    var fechaInput by mutableStateOf(LocalDate.now().toString());private set
    var pendiente by mutableStateOf<MovimientoPendiente?>(null);private set
    var ultimoGuardado by mutableStateOf<Movimiento?>(null);private set
    val listaDeMovimientos=mutableStateListOf<Movimiento>()
    private val _cargando=MutableStateFlow(false)
    val cargando:StateFlow<Boolean> = _cargando
    private var trabajo:Job?=null
    init {viewModelScope.launch {sessionManager.userIdFlow.collect {id->
        if(id!=usuarioActual) {
            trabajo?.cancel();usuarioActual=id;listaDeMovimientos.clear();saldoTotal=0.0;datosListos=false;errorCarga=null;errorRegistro=null;_cargando.value=false;pendiente=null;ultimoGuardado=null;resetFormulario()
            if(id>0) {
                pendiente=sessionManager.movimientoPendiente(id)?.let{runCatching{Json.decodeFromString<MovimientoPendiente>(it)}.getOrNull()}
                pendiente?.movimiento?.let {m->montoInput=BigDecimal.valueOf(kotlin.math.abs(m.monto)).toPlainString();categoriaSeleccionada=listOf("","Trabajo","Comida","Ocio","Trans.").getOrElse(m.idCategoria){""};descripcionInput=m.descripcion.orEmpty();esIngresoInput=m.monto>0;fechaInput=m.fecha}
                cargarDatosDelServidor()
            }
        }
    }}}
    fun cargarDatosDelServidor() {
        if(usuarioActual<=0 || _cargando.value)return
        val id=usuarioActual;_cargando.value=true;errorCarga=null
        trabajo=viewModelScope.launch {
            try {
                val r=withTimeout(15000){client.get("${ApiConfig.MOVIMIENTOS_URL}/usuario/$id"){bearerAuth(sessionManager.getToken().orEmpty())}}
                if(id!=usuarioActual)return@launch
                if(r.status==HttpStatusCode.OK){val lista=r.body<List<Movimiento>>();listaDeMovimientos.clear();listaDeMovimientos.addAll(lista);saldoTotal=lista.sumOf{it.monto};datosListos=true}
                else errorCarga=if(r.status.value==401) "Tu sesión venció. Vuelve a entrar desde Perfil." else "No pudimos actualizar. Tus últimos datos siguen visibles."
            }catch(e:Exception){if(e is CancellationException && e !is TimeoutCancellationException)throw e;if(id==usuarioActual)errorCarga="Sin conexión con NEXO. Revisa la red y vuelve a actualizar."}
            finally{if(id==usuarioActual)_cargando.value=false}
        }
    }
    fun onDescripcionChanged(v:String){if(pendiente==null)descripcionInput=v.take(200)}
    fun onMontoChange(v:String){if(pendiente==null){val n=v.replace(',','.');if(n.matches(Regex("[0-9]*\\.?[0-9]*")))montoInput=n}}
    fun onCategoriaChange(v:String){if(pendiente==null)categoriaSeleccionada=v}
    fun onEsIngresoChange(v:Boolean){if(pendiente==null)esIngresoInput=v}
    fun onFechaChange(v:String){if(pendiente==null)fechaInput=v}
    fun resetFormulario(){if(pendiente!=null)return;montoInput="";descripcionInput="";categoriaSeleccionada="";esIngresoInput=false;fechaInput=LocalDate.now().toString();errorRegistro=null}
    fun registrarMovimiento(onSuccess:()->Unit) {
        if(_cargando.value)return
        pendiente?.let{enviar(it,onSuccess);return}
        val importe=montoInput.toBigDecimalOrNull();val fecha=runCatching{LocalDate.parse(fechaInput)}.getOrNull()
        errorRegistro=when {
            usuarioActual<=0->"Inicia sesión antes de guardar."
            importe==null || importe<=BigDecimal.ZERO || importe.scale()>2 || importe>BigDecimal("999999999.99")->"Escribe un importe positivo con máximo dos decimales."
            categoriaSeleccionada !in listOf("Trabajo","Comida","Ocio","Trans.")->"Elige una categoría."
            fecha==null || fecha>LocalDate.now() || fecha<LocalDate.of(1900,1,1)->"Elige una fecha válida, de hoy o anterior."
            descripcionInput.isBlank()->"Escribe un concepto para reconocerlo después."
            else->null
        }
        if(errorRegistro!=null)return
        val categoria=listOf("Trabajo","Comida","Ocio","Trans.").indexOf(categoriaSeleccionada)+1
        val m=Movimiento(0,usuarioActual,categoria,importe!!.toDouble()*(if(esIngresoInput)1 else -1),fechaInput,descripcionInput.trim(),if(esIngresoInput)"Ingreso" else "Gasto")
        enviar(MovimientoPendiente(UUID.randomUUID().toString(),m),onSuccess)
    }
    private fun enviar(p:MovimientoPendiente,onSuccess:()->Unit) {
        val id=usuarioActual;_cargando.value=true;errorRegistro=null
        trabajo=viewModelScope.launch {
            try {
                sessionManager.guardarMovimientoPendiente(id,Json.encodeToString(p));pendiente=p
                val r=withTimeout(15000){client.post(ApiConfig.MOVIMIENTOS_URL){bearerAuth(sessionManager.getToken().orEmpty());header("Idempotency-Key",p.solicitud);contentType(ContentType.Application.Json);setBody(p.movimiento)}}
                if(id!=usuarioActual)return@launch
                if(r.status.value in 200..299) {
                    val guardado=r.body<Movimiento>();sessionManager.guardarMovimientoPendiente(id,null);pendiente=null;ultimoGuardado=guardado
                    listaDeMovimientos.removeAll{it.idMovimiento==guardado.idMovimiento};listaDeMovimientos.add(0,guardado);saldoTotal=listaDeMovimientos.sumOf{it.monto};resetFormulario();onSuccess()
                }else if(r.status.value in listOf(400,403,409)) {
                    errorRegistro=runCatching{r.body<MensajeNexo>().mensaje}.getOrDefault("Revisa los datos enviados.")
                    sessionManager.guardarMovimientoPendiente(id,null);pendiente=null
                }else errorRegistro=if(r.status.value==401)"Tu sesión venció. Vuelve a entrar; el guardado pendiente se conserva." else "Aún no podemos confirmar el guardado. Reintenta esta solicitud; no se duplicará."
            }catch(e:Exception){if(e is CancellationException && e !is TimeoutCancellationException)throw e;if(id==usuarioActual)errorRegistro="Guardado sin confirmar. Pulsa Reintentar cuando vuelva la conexión; conservamos tu solicitud."}
            finally{if(id==usuarioActual)_cargando.value=false}
        }
    }
}
