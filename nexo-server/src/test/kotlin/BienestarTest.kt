package com.example

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlin.test.*
import java.time.LocalDate
import java.util.UUID

class BienestarTest {
    private fun a(tipo:String="crear")=AccionNexo(UUID.randomUUID().toString(),tipo,nombre="Universidad",importe="1500.00",fecha="2026-12-01")
    @Test fun validaDineroYFecha() {
        val hoy=LocalDate.of(2026,10,7)
        validarAccionNexo(a(),hoy)
        listOf("0","-1","NaN","1e3","1.001","100000000","1,000").forEach { assertFailsWith<IllegalArgumentException>{validarAccionNexo(a().copy(importe=it),hoy)} }
        listOf("2026-02-30","2026-10-06","2050-01-01").forEach { assertFailsWith<IllegalArgumentException>{validarAccionNexo(a().copy(fecha=it),hoy)} }
        assertFailsWith<IllegalArgumentException>{validarAccionNexo(a("perfil").copy(edad=0),hoy)}
    }
    @Test fun rachaToleraHastaAyerYNoBorraAcumulados() {
        val hoy=LocalDate.of(2026,10,7)
        val dias=setOf(hoy.minusDays(1),hoy.minusDays(2),hoy.minusDays(4))
        assertEquals(2,rachaNexo(dias,hoy));assertEquals(3,rachaNexo(dias+hoy,hoy))
        assertEquals(0,rachaNexo(dias,hoy.plusDays(1)));assertEquals(3,dias.size)
        assertEquals(7,requisitoPremio("bosque",true));assertNull(requisitoPremio("inventado",true))
    }
    @Test fun sesionRevocadaNoSirve() { val token=SesionesNexo.crear(7);SesionesNexo.revocar(token);assertNull(SesionesNexo.usuario(token)) }
    @Test fun rutasExigenSesionYAislanUsuario()=testApplication {
        environment { config=MapApplicationConfig() }
        val usuarios=mutableListOf<Int>()
        val almacen=object:AlmacenBienestar {
            override suspend fun estado(usuario:Int):EstadoNexo {usuarios+=usuario;return EstadoNexo(PerfilNexo("Prueba","p@example.test",20,"2026-10-01",0),emptyList(),HabitosNexo())}
            override suspend fun aplicar(usuario:Int,accion:AccionNexo)=estado(usuario)
        }
        application {configureSerialization();configureBienestar(almacen)}
        assertEquals(HttpStatusCode.Unauthorized,client.get("/bienestar").status)
        assertEquals(HttpStatusCode.Unauthorized,client.post("/bienestar"){contentType(ContentType.Application.Json);setBody("{}")}.status)
        val token=SesionesNexo.crear(91)
        assertEquals(HttpStatusCode.OK,client.get("/bienestar"){bearerAuth(token)}.status)
        assertEquals(listOf(91),usuarios)
        assertEquals(HttpStatusCode.BadRequest,client.post("/bienestar"){bearerAuth(token);contentType(ContentType.Application.Json);setBody("bad")}.status)
    }
}
