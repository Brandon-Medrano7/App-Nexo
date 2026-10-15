package com.example

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.*

class CompromisosTest {
    private fun pago()=CompromisoDto(UUID.randomUUID().toString(),"Internet","400.00","2026-10-14")
    @Test fun validaImporteEstadoYFechas() {
        validarCompromiso(pago())
        listOf(pago().copy(nombre=" "),pago().copy(importe="-1"),pago().copy(importe="1.001"),pago().copy(importe="0"),pago().copy(importe="NaN"),pago().copy(fecha="2026-02-30"),pago().copy(id="otro"),pago().copy(estado="PAGADO"),pago().copy(movimiento=1),pago().copy(version=-1)).forEach{assertFailsWith<IllegalArgumentException>{validarCompromiso(it)}}
    }
    @Test fun reservaVencidosPeroNoPagadosNiDiaDePaga() {
        val lista=listOf(pago().copy(fecha="2026-09-01"),pago(),pago().copy(fecha="2026-10-15"),pago().copy(estado="PAGADO",movimiento=1),pago().copy(estado="CANCELADO"))
        assertEquals(0,BigDecimal("800").compareTo(reservadoCompromisos(lista,LocalDate.of(2026,10,15))))
    }
    @Test fun guiaRestaPagosYAdvierteSiNoAlcanza()=kotlinx.coroutines.runBlocking {
        val contexto=ContextoGuia("2026-10-14",BigDecimal.ZERO,BigDecimal.ZERO,0,0,BigDecimal.ZERO,0,BigDecimal("1000"),BigDecimal("200"),2,BigDecimal("300"))
        val texto=MotorGuiaLocal().responder("presupuesto",contexto).texto
        assertTrue(texto.contains("500.00"));assertTrue(texto.contains("250.00"));assertTrue(texto.contains("300.00"))
        assertTrue(MotorGuiaLocal().responder("presupuesto",contexto.copy(comprometido=BigDecimal("900"))).texto.contains("superan"))
    }
    @Test fun rutasUsanUsuarioDeSesionYRechazanDatosInvalidos()=testApplication {
        environment{config=MapApplicationConfig()}
        val usuarios=mutableListOf<Int>()
        application{configureSerialization();configureCompromisos(object:AlmacenCompromisos {
            override suspend fun listar(usuario:Int):List<CompromisoDto>{usuarios+=usuario;return emptyList()}
            override suspend fun guardar(usuario:Int,c:CompromisoDto):CompromisoDto{usuarios+=usuario;return c.copy(version=1)}
        })}
        assertEquals(HttpStatusCode.Unauthorized,client.get("/compromisos").status)
        assertEquals(HttpStatusCode.Unauthorized,client.put("/compromisos").status)
        val token=SesionesNexo.crear(7)
        assertEquals(HttpStatusCode.OK,client.get("/compromisos?usuario=99"){bearerAuth(token)}.status)
        assertEquals(HttpStatusCode.BadRequest,client.put("/compromisos"){bearerAuth(token);contentType(ContentType.Application.Json);setBody("{}")}.status)
        assertEquals(HttpStatusCode.OK,client.put("/compromisos"){bearerAuth(token);contentType(ContentType.Application.Json);setBody("""{"id":"${UUID.randomUUID()}","nombre":"Renta","importe":"200","fecha":"2026-10-16","usuario":99}""")}.status)
        assertEquals(listOf(7,7),usuarios)
    }
    @Test fun conflictoTieneRespuestaRecuperable()=testApplication {
        environment{config=MapApplicationConfig()}
        application{configureSerialization();configureCompromisos(object:AlmacenCompromisos {
            override suspend fun listar(usuario:Int):List<CompromisoDto> = error("fallo")
            override suspend fun guardar(usuario:Int,c:CompromisoDto):CompromisoDto=throw ConflictoCompromiso("Actualiza.")
        })}
        val token=SesionesNexo.crear(8)
        assertEquals(HttpStatusCode.ServiceUnavailable,client.get("/compromisos"){bearerAuth(token)}.status)
        assertEquals(HttpStatusCode.Conflict,client.put("/compromisos"){bearerAuth(token);contentType(ContentType.Application.Json);setBody("""{"id":"${UUID.randomUUID()}","nombre":"Renta","importe":"200","fecha":"2026-10-16"}""")}.status)
    }
}
