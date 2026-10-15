package com.nexo.app.util

import java.time.LocalDate
import kotlin.test.*

class GastoPorVozTest {
    private val hoy = LocalDate.of(2026, 10, 7)
    @Test fun gastoSimple() {
        val b = interpretarGastoVoz("Gasté 85 pesos en comida", hoy)
        assertEquals("85.00", b.monto); assertEquals("Comida", b.categoria); assertEquals("2026-10-07", b.fecha)
    }
    @Test fun cantidadEnPalabrasYAyer() {
        val b = interpretarGastoVoz("Ayer pagué ochenta y cinco pesos en taxi", hoy)
        assertEquals("85.00", b.monto); assertEquals("Trans.", b.categoria); assertEquals("2026-10-06", b.fecha)
    }
    @Test fun decimalesEnPesos() {
        assertEquals("85.50", interpretarGastoVoz("Gasté 85,50 pesos en café", hoy).monto)
        assertEquals("85.50", interpretarGastoVoz("Gasté 85.50 en cine", hoy).monto)
    }
    @Test fun cantidadesAmbiguasNoSeAplican() {
        listOf("Gasté 50 y 20 en comida", "Gasté 1,200 pesos", "Gasté 12.345 pesos", "Gasté dos mil pesos", "No gasté 85 pesos", "Gasté -85 pesos", "Gasté 20 dólares", "Recibí 500 pesos", "Me pagaron 1000 pesos", "Gasté cero pesos", "Gasté 50 pesos y veinte en taxi").forEach {
            assertNull(interpretarGastoVoz(it, hoy).monto, it)
        }
    }
    @Test fun categoriaDesconocidaOAmbiguaRequiereSeleccion() {
        assertNull(interpretarGastoVoz("Gasté 85 en farmacia", hoy).categoria)
        assertNull(interpretarGastoVoz("Gasté 85 en comida y transporte", hoy).categoria)
    }
    @Test fun fechasNoSoportadasNoSeInventan() {
        listOf("Gasté 85 pesos el lunes", "Gasté 85 pesos anteayer", "Gasté 85 mañana", "Gasté 85 ayer y hoy", "Gasté 85 pesos en septiembre").forEach {
            assertNull(interpretarGastoVoz(it, hoy).fecha, it)
        }
    }
    @Test fun vacioYConceptoLargo() {
        assertNull(interpretarGastoVoz("", hoy).monto)
        assertEquals(200, interpretarGastoVoz("Gasté 85 pesos " + "a".repeat(300), hoy).concepto.length)
    }
    @Test fun cantidadesEnPalabrasSinConfundirConcepto() {
        assertEquals("120.00", interpretarGastoVoz("Pagué ciento veinte pesos en el cine", hoy).monto)
        assertEquals("999.00", interpretarGastoVoz("Gasté novecientos noventa y nueve pesos", hoy).monto)
    }
}
