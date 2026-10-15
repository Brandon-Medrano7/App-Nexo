package com.nexo.app.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.math.BigDecimal
import java.time.LocalDate

class PresupuestoTest {
    private fun d(s: String) = LocalDate.parse(s)
    @Test fun periodoCruzaMesYExcluyeIngresosYFechaDePago() {
        val r = resumirPresupuesto(BigDecimal("1000"), d("2026-09-25"), d("2026-10-09"), listOf(
            d("2026-09-24") to BigDecimal("-900"),
            d("2026-09-25") to BigDecimal("-100"),
            d("2026-10-05") to BigDecimal("-50"),
            d("2026-10-05") to BigDecimal("3000"),
            d("2026-10-06") to BigDecimal("-90"),
            d("2026-10-09") to BigDecimal("-700")
        ), d("2026-10-05"))
        assertEquals(0, r.gastado.compareTo(BigDecimal("150")))
        assertEquals(0, r.disponible.compareTo(BigDecimal("850")))
        assertEquals(4L, r.dias)
        assertEquals(BigDecimal("212.50"), r.diario)
    }
    @Test fun excesoNoRecomiendaUnImporteNegativo() {
        val r = resumirPresupuesto(BigDecimal("100"), d("2026-10-01"), d("2026-10-15"),
            listOf(d("2026-10-02") to BigDecimal("-120")), d("2026-10-05"))
        assertEquals(0, r.disponible.compareTo(BigDecimal("-20")))
        assertEquals(0, r.diario.signum())
    }
    @Test fun diaDePagoNoDivideEntreCero() {
        val r = resumirPresupuesto(BigDecimal("100"), d("2026-10-01"), d("2026-10-15"),
            listOf(d("2026-10-15") to BigDecimal("-50")), d("2026-10-15"))
        assertEquals(0L, r.dias)
        assertEquals(0, r.gastado.signum())
        assertEquals(0, r.diario.signum())
    }
    @Test fun centavosSeRedondeanHaciaAbajo() {
        val r = resumirPresupuesto(BigDecimal("100"), d("2026-10-01"), d("2026-10-04"), emptyList(), d("2026-10-01"))
        assertEquals(BigDecimal("33.33"), r.diario)
    }
    @Test fun rechazaPeriodosInvalidos() {
        assertFailsWith<IllegalArgumentException> {
            resumirPresupuesto(BigDecimal("100"), d("2026-10-05"), d("2026-10-05"), emptyList())
        }
    }
}
