package com.example

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlin.test.*
import java.time.LocalDate
import java.util.UUID

class PresupuestosTest {
    private fun periodo(importe: String = "1000.00", inicio: String = "2026-10-01", pago: String = "2026-10-15") =
        PresupuestoDto(UUID.randomUUID().toString(), importe, inicio, pago)
    @Test fun validaImportesYFechas() {
        val hoy = LocalDate.parse("2026-10-06")
        validarPresupuesto(periodo(), hoy)
        listOf("0", "-1", "1.001", "NaN", "1e3", "1000000000").forEach { importe ->
            assertFailsWith<IllegalArgumentException> { validarPresupuesto(periodo(importe), hoy) }
        }
        assertFailsWith<IllegalArgumentException> { validarPresupuesto(periodo(pago = "2026-09-30"), hoy) }
        assertFailsWith<IllegalArgumentException> { validarPresupuesto(periodo(inicio = "2026-10-07"), hoy) }
        assertFailsWith<IllegalArgumentException> { validarPresupuesto(periodo(pago = "2026-02-30"), hoy) }
    }
    @Test fun periodosContiguosNoSeCruzan() {
        assertFalse(seCruzan(periodo(), periodo(inicio = "2026-10-15", pago = "2026-10-30")))
        assertTrue(seCruzan(periodo(), periodo(inicio = "2026-10-14", pago = "2026-10-30")))
    }
    @Test fun sesionesNoAceptanTokensInventados() {
        assertNull(SesionesNexo.usuario("inventado"))
        val a = SesionesNexo.crear(31); val b = SesionesNexo.crear(32)
        assertNotEquals(a, b); assertEquals(31, SesionesNexo.usuario(a)); assertEquals(32, SesionesNexo.usuario(b))
    }
    @Test fun rutasRequierenSesionYAislanCuentas() = testApplication {
        environment { config = MapApplicationConfig() }
        val datos = mutableMapOf<Int, MutableList<PresupuestoDto>>()
        val almacen = object: AlmacenPresupuestos {
            override suspend fun listar(usuario: Int) = datos[usuario].orEmpty()
            override suspend fun guardar(usuario: Int, presupuesto: PresupuestoDto): PresupuestoDto {
                val guardado = presupuesto.copy(version = 1)
                datos.getOrPut(usuario) { mutableListOf() }.add(guardado)
                return guardado
            }
        }
        application { configureSerialization(); configurePresupuestos(almacen) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/presupuestos").status)
        assertEquals(HttpStatusCode.Unauthorized, client.put("/presupuestos").status)
        val a = SesionesNexo.crear(100); val b = SesionesNexo.crear(200)
        val id = UUID.randomUUID().toString()
        assertEquals(HttpStatusCode.OK, client.put("/presupuestos") {
            bearerAuth(a); contentType(ContentType.Application.Json)
            setBody("""{"id":"$id","importe":"1000","inicio":"2020-01-01","pago":"2020-01-15","version":0,"idUsuario":200}""")
        }.status)
        assertEquals(1, datos[100]?.size); assertNull(datos[200])
        assertEquals(HttpStatusCode.OK, client.get("/presupuestos") { bearerAuth(b) }.status)
        assertEquals(HttpStatusCode.BadRequest, client.put("/presupuestos") { bearerAuth(a); contentType(ContentType.Application.Json); setBody("{}") }.status)
    }
}
