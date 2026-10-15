package com.nexo.app.util
import com.nexo.app.ui.viewModel.CompromisoRemoto
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*
class CompromisosTest {
    private val pago=LocalDate.of(2026,10,15)
    private fun pendiente(fecha:String="2026-10-14")=CompromisoRemoto("prueba","Internet","200.00",fecha)
    @Test fun reservaVencidosYExcluyeDiaDePaga() {
        val r=disponiblePlanificado(BigDecimal("1000"),2,pago,listOf(pendiente("2026-09-10"),pendiente(),pendiente("2026-10-15")))
        assertEquals(BigDecimal("400.00"),r.reservado);assertEquals(BigDecimal("600.00"),r.libre);assertEquals(BigDecimal("300.00"),r.diario)
    }
    @Test fun vincularGastoNoLoDescuentaDosVeces() {
        val antes=disponiblePlanificado(BigDecimal("1000"),2,pago,listOf(pendiente()))
        val despues=disponiblePlanificado(BigDecimal("800"),2,pago,listOf(pendiente().copy(estado="PAGADO",movimiento=3)))
        assertEquals(0,antes.libre.compareTo(despues.libre))
        assertEquals(0,disponiblePlanificado(BigDecimal("1000"),2,pago,listOf(pendiente().copy(estado="CANCELADO"))).reservado.signum())
    }
    @Test fun noSugiereGastarSiFaltaDineroNiDividePorCero() {
        val r=disponiblePlanificado(BigDecimal("100"),2,pago,listOf(pendiente()))
        assertTrue(r.libre.signum()<0);assertEquals(0,r.diario.signum())
        assertEquals(0,disponiblePlanificado(BigDecimal("1000"),0,pago,emptyList()).diario.signum())
    }
}
