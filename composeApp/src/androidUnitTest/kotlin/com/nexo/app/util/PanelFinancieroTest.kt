package com.nexo.app.util
import com.nexo.app.model.Movimiento
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.*
class PanelFinancieroTest {
    private fun m(id:Int, monto:Double, fecha:String, categoria:Int=2, concepto:String="Comida") = Movimiento(id, 1, categoria, monto, fecha, concepto, if(monto<0) "Gasto" else "Ingreso")
    @Test fun sumaSinErrorBinario() { assertEquals("0.3", totalNexo(listOf(m(1,0.1,"2026-10-01"),m(2,0.2,"2026-10-01"))).toPlainString()) }
    @Test fun mesExcluyePasadoFuturoYFechaInvalida() {
        val lista = listOf(m(1,-10.0,"2026-09-30"),m(2,-20.0,"2026-10-01"),m(3,30.0,"2026-10-07"),m(4,-40.0,"2026-10-08"),m(5,5.0,"error"))
        assertEquals(listOf(2,3), movimientosDelMes(lista, LocalDate.of(2026,10,7)).map { it.idMovimiento })
    }
    @Test fun combinaTipoBusquedaYOrden() {
        val lista = listOf(m(1,-10.0,"2026-10-01",4,"Camión"),m(2,20.0,"2026-10-02",4,"Reembolso"),m(3,-30.0,"2026-10-03",4,"Taxi"))
        assertEquals(listOf(3,1),filtrarMovimientos(lista,"Gastos"," transporte ").map { it.idMovimiento })
        assertEquals(listOf(2),filtrarMovimientos(lista,"Ingresos","reembolso").map { it.idMovimiento })
        assertTrue(filtrarMovimientos(lista,"Todos","renta").isEmpty())
    }
}
