package com.nexo.app.util

import com.nexo.app.ui.screens.RespuestaGuia
import com.nexo.app.ui.screens.CambioDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.test.*

class ContratoConsultasTest {
    @Test fun guiaAceptaServidorAnteriorSinFuente() {
        val respuesta = Json.decodeFromString<RespuestaGuia>("""{"titulo":"Metas","texto":"Sin metas","fecha":"2026-10-14"}""")
        assertEquals("Resumen local", respuesta.fuente)
    }
    @Test fun divisasAceptaServidorAnteriorSinValoresPredeterminados() {
        val respuesta = Json.decodeFromString<CambioDto>("""{"moneda":"USD","tasa":"18.5","fecha":"2026-10-14"}""")
        assertEquals("Frankfurter", respuesta.fuente)
        assertEquals("MXN", respuesta.destino)
    }
    @Test fun conservaFuenteExplicita() {
        val respuesta = Json.decodeFromString<RespuestaGuia>("""{"titulo":"Metas","texto":"Sin metas","fuente":"Otro proveedor","fecha":"2026-10-14"}""")
        assertEquals("Otro proveedor", respuesta.fuente)
    }
    @Test fun camposEsencialesSiguenSiendoObligatorios() {
        assertFailsWith<SerializationException> { Json.decodeFromString<RespuestaGuia>("""{"titulo":"Metas","fecha":"2026-10-14"}""") }
        assertFailsWith<SerializationException> { Json.decodeFromString<CambioDto>("""{"moneda":"USD","fecha":"2026-10-14"}""") }
    }
    @Test fun errorDeFormatoNoSePresentaComoProblemaDeConexion() {
        val error = IllegalStateException("Conversión", SerializationException("Falta campo"))
        assertTrue(mensajeErrorConsulta(error).contains("no se pudo leer"))
        assertTrue(mensajeErrorConsulta(IOException()).contains("conectar"))
        assertTrue(mensajeErrorConsulta(java.net.SocketTimeoutException()).contains("tardó"))
        assertTrue(mensajeErrorConsulta(IllegalStateException()).contains("completar"))
    }
}
