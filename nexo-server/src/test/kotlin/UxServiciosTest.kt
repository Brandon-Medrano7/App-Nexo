package com.example

import com.example.com.nexo.app.model.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*

class UxServiciosTest {
    @Test fun clavesSonDistintasYVerificables() {
        val clave="frase-larga-de-prueba"
        val a=ClavesNexo.hash(clave);val b=ClavesNexo.hash(clave)
        assertNotEquals(a,b);assertTrue(a.length<=100);assertFalse(a.contains(clave));assertTrue(ClavesNexo.verificar(clave,a));assertFalse(ClavesNexo.verificar("incorrecta",a));assertFalse(ClavesNexo.verificar(clave,"pbkdf2\$malformado"))
        assertTrue(ClavesNexo.verificar("Anterior","Anterior"));assertFalse(ClavesNexo.verificar("anterior","Anterior"))
    }
    @Test fun movimientoRechazaSignoFechaEImporteInvalidos() {
        val hoy=LocalDate.of(2026,10,12)
        val m=Movimiento(0,1,2,-12.50,hoy.toString(),"Comida","Gasto")
        validarMovimiento(m,hoy)
        listOf(Double.NaN,Double.POSITIVE_INFINITY,0.0,-1.001,-1000000000.0).forEach{assertFailsWith<IllegalArgumentException>{validarMovimiento(m.copy(monto=it),hoy)}}
        assertFailsWith<IllegalArgumentException>{validarMovimiento(m.copy(tipo="Ingreso"),hoy)}
        assertFailsWith<IllegalArgumentException>{validarMovimiento(m.copy(fecha="2026-10-13"),hoy)}
        assertFailsWith<IllegalArgumentException>{validarMovimiento(m.copy(descripcion="  "),hoy)}
    }
    @Test fun guiaUsaDatosYNoPermiteTemasInventados(): Unit =runBlocking {
        val c=ContextoGuia("2026-10-12",BigDecimal("100"),BigDecimal("30"),1,0,BigDecimal("500"),3,BigDecimal("1000"),BigDecimal("200"),4)
        val motor=MotorGuiaLocal()
        val r=motor.responder("presupuesto",c)
        assertTrue(r.texto.contains("800.00"));assertTrue(r.texto.contains("200.00"));assertEquals("Resumen local",r.fuente)
        assertTrue(motor.responder("presupuesto",c.copy(gastosPeriodo=BigDecimal("1200"))).texto.contains("superan"))
        assertTrue(motor.responder("presupuesto",c.copy(presupuesto=null)).texto.contains("Configura"))
        assertFailsWith<IllegalArgumentException>{motor.responder("envía dinero",c)}
    }
    @Test fun movimientosNoPermitenUsarOtraCuenta()=testApplication {
        environment{config=MapApplicationConfig()}
        var consultados=0
        val cuentas=object:CuentasNexo{override suspend fun registrar(u:Usuario){};override suspend fun autenticar(correo:String,password:String):Int?=null}
        val movimientos=object:RegistroMovimientos{override suspend fun listar(usuario:Int):List<Movimiento>{consultados++;return emptyList()};override suspend fun guardar(usuario:Int,solicitud:String,m:Movimiento)=m}
        application{configureSerialization();configureRouting(cuentas,movimientos)}
        assertEquals(HttpStatusCode.Unauthorized,client.get("/movimientos/usuario/3").status)
        val token=SesionesNexo.crear(3)
        assertEquals(HttpStatusCode.Forbidden,client.get("/movimientos/usuario/4"){bearerAuth(token)}.status)
        assertEquals(HttpStatusCode.OK,client.get("/movimientos/usuario/3"){bearerAuth(token)}.status)
        assertEquals(1,consultados)
        assertEquals(HttpStatusCode.Unauthorized,client.get("/perfil/usuario/3").status)
        assertEquals(HttpStatusCode.Forbidden,client.get("/metas/usuario/4"){bearerAuth(token)}.status)
    }
    @Test fun proveedorExternoNoRecibeContextoSinAutorizacion()=testApplication {
        environment{config=MapApplicationConfig()}
        var consultado=false
        val fuente=object:FuenteGuia{override suspend fun contexto(usuario:Int):ContextoGuia{consultado=true;error("No debe consultar datos")}}
        val motor=object:MotorGuia{override val externo=true;override suspend fun responder(tema:String,contexto:ContextoGuia):RespuestaGuia=error("No debe enviar datos")}
        application{configureSerialization();configureGuia(fuente,motor)}
        assertEquals(HttpStatusCode.Unauthorized,client.post("/asistente/consulta").status)
        val token=SesionesNexo.crear(3)
        assertEquals(HttpStatusCode.Forbidden,client.post("/asistente/consulta"){bearerAuth(token);contentType(ContentType.Application.Json);setBody("""{"tema":"gastos","consentimientoExterno":true}""")}.status)
        assertFalse(consultado)
    }
}
