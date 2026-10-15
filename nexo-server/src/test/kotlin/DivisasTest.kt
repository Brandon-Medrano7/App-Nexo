package com.example
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import kotlin.test.*
class DivisasTest {
 @Test fun validaProveedor() {
  val hoy=LocalDate.of(2026,10,13)
  fun cuerpo(t:String="18.50",f:String="2026-10-12",b:String="USD")="""{"base":"$b","quote":"MXN","rate":$t,"date":"$f"}"""
  assertEquals("18.50",interpretarTipoCambio("USD",cuerpo(),hoy).tasa)
  listOf(cuerpo("0"),cuerpo("-2"),cuerpo(f="2026-10-14"),cuerpo(f="2026-09-01"),cuerpo(b="EUR")).forEach{assertFails{interpretarTipoCambio("USD",it,hoy)}}
 }
 @Test fun cacheReutilizaYExpira():Unit=runBlocking {
  var llamadas=0;var ahora=Instant.parse("2026-10-13T00:00:00Z")
  val cache=DivisasCache(object:FuenteDivisas{override suspend fun obtener(moneda:String):TipoCambio{llamadas++;return TipoCambio(moneda,"18.5","2026-10-12")}}, {ahora})
  cache.obtener("USD");cache.obtener("USD");assertEquals(1,llamadas)
  cache.obtener("EUR");assertEquals(2,llamadas)
  ahora=ahora.plusSeconds(1800);cache.obtener("USD");assertEquals(3,llamadas)
 }
 @Test fun protegeEndpoint()=testApplication {
  environment{config=MapApplicationConfig()};var llamadas=0
  application{configureSerialization();configureDivisas(object:FuenteDivisas{override suspend fun obtener(moneda:String):TipoCambio{llamadas++;return TipoCambio(moneda,"18.5","2026-10-12")}})}
  assertEquals(HttpStatusCode.Unauthorized,client.get("/herramientas/tipo-cambio").status)
  val token=SesionesNexo.crear(3)
  assertEquals(HttpStatusCode.BadRequest,client.get("/herramientas/tipo-cambio?moneda=ABC"){bearerAuth(token)}.status);assertEquals(0,llamadas)
  assertEquals(HttpStatusCode.OK,client.get("/herramientas/tipo-cambio?moneda=USD"){bearerAuth(token)}.status);assertEquals(1,llamadas)
 }
 @Test fun fallaControlada()=testApplication {
  environment{config=MapApplicationConfig()}
  application{configureSerialization();configureDivisas(object:FuenteDivisas{override suspend fun obtener(moneda:String):TipoCambio=error("Interno")})}
  val token=SesionesNexo.crear(3)
  assertEquals(HttpStatusCode.ServiceUnavailable,client.get("/herramientas/tipo-cambio"){bearerAuth(token)}.status)
 }
}
