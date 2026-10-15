@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@Serializable data class TipoCambio(val moneda:String,val tasa:String,val fecha:String,@kotlinx.serialization.EncodeDefault val fuente:String="Frankfurter",@kotlinx.serialization.EncodeDefault val destino:String="MXN")
interface FuenteDivisas { suspend fun obtener(moneda:String):TipoCambio }
class Frankfurter:FuenteDivisas {
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build()
    override suspend fun obtener(moneda:String):TipoCambio=withContext(Dispatchers.IO) {
        require(moneda in setOf("USD","EUR"))
        val request=HttpRequest.newBuilder(URI("https://api.frankfurter.dev/v2/rate/${moneda.lowercase()}/mxn")).timeout(Duration.ofSeconds(6)).GET().build()
        val response=http.send(request,HttpResponse.BodyHandlers.ofString())
        check(response.statusCode()==200){"Proveedor no disponible"}
        interpretarTipoCambio(moneda,response.body())
    }
}
fun interpretarTipoCambio(moneda:String,cuerpo:String,hoy:LocalDate=LocalDate.now()):TipoCambio {
    val json=Json.parseToJsonElement(cuerpo).jsonObject
    check(json["base"]?.jsonPrimitive?.content==moneda && json["quote"]?.jsonPrimitive?.content=="MXN")
    val tasa=json.getValue("rate").jsonPrimitive.content.toBigDecimal()
    check(tasa.signum()>0 && tasa<java.math.BigDecimal("1000000"))
    val fecha=LocalDate.parse(json.getValue("date").jsonPrimitive.content)
    check(!fecha.isAfter(hoy) && !fecha.isBefore(hoy.minusDays(7))){"Tipo de cambio fuera de fecha"}
    return TipoCambio(moneda,tasa.toPlainString(),fecha.toString())
}
class DivisasCache(private val fuente:FuenteDivisas,private val ahora:()->Instant={Instant.now()}):FuenteDivisas {
    private val mutex=Mutex()
    private val datos=mutableMapOf<String,Pair<Instant,TipoCambio>>()
    override suspend fun obtener(moneda:String):TipoCambio=mutex.withLock {
        require(moneda in setOf("USD","EUR"))
        val existente=datos[moneda]
        if(existente!=null && Duration.between(existente.first,ahora())<Duration.ofMinutes(30))return@withLock existente.second
        fuente.obtener(moneda).also{datos[moneda]=ahora() to it}
    }
}
fun Application.configureDivisas(fuente:FuenteDivisas=DivisasCache(Frankfurter())) {
    routing {get("/herramientas/tipo-cambio") {
        call.usuarioNexo()?:return@get
        val moneda=call.request.queryParameters["moneda"]?:"USD"
        if(moneda !in setOf("USD","EUR")){call.respond(HttpStatusCode.BadRequest,MensajeNexo("Elige USD o EUR."));return@get}
        try {call.respond(fuente.obtener(moneda))}
        catch(e:CancellationException){throw e}
        catch(e:Exception){call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos consultar el tipo de cambio. Intenta más tarde."))}
    }}
}
