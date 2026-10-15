package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

class ContratoConsultasTest {
    @Test fun respuestasHttpIncluyenCamposPredeterminados() = testApplication {
        environment { config = MapApplicationConfig() }
        application {
            configureSerialization()
            routing {
                get("/guia") { call.respond(RespuestaGuia("Metas", "Sin metas", fecha="2026-10-14")) }
                get("/divisas") { call.respond(TipoCambio("USD", "18.5", "2026-10-14")) }
            }
        }
        val guia = Json.parseToJsonElement(client.get("/guia").bodyAsText()).jsonObject
        assertEquals("Resumen local", guia.getValue("fuente").jsonPrimitive.content)
        assertEquals("Sin metas", guia.getValue("texto").jsonPrimitive.content)
        val divisas = Json.parseToJsonElement(client.get("/divisas").bodyAsText()).jsonObject
        assertEquals("Frankfurter", divisas.getValue("fuente").jsonPrimitive.content)
        assertEquals("MXN", divisas.getValue("destino").jsonPrimitive.content)
    }
}
