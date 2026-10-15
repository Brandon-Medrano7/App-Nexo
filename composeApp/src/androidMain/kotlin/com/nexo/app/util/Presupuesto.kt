package com.nexo.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ResumenPresupuesto(val gastado: BigDecimal, val disponible: BigDecimal,
    val dias: Long, val diario: BigDecimal)

// El próximo día de pago pertenece al siguiente periodo, no al actual.
fun resumirPresupuesto(limite: BigDecimal, inicio: LocalDate, proximoPago: LocalDate,
    movimientos: List<Pair<LocalDate, BigDecimal>>, hoy: LocalDate = LocalDate.now()): ResumenPresupuesto {
    require(limite > BigDecimal.ZERO && proximoPago > inicio)
    val gastos = movimientos.filter { (fecha, monto) ->
        !fecha.isBefore(inicio) && fecha.isBefore(proximoPago) && !fecha.isAfter(hoy) && monto.signum() < 0
    }.fold(BigDecimal.ZERO) { total, (_, monto) -> total + monto.abs() }
    val disponible = limite - gastos
    val dias = ChronoUnit.DAYS.between(maxOf(hoy, inicio), proximoPago).coerceAtLeast(0)
    val diario = if (dias > 0) disponible.max(BigDecimal.ZERO).divide(BigDecimal.valueOf(dias), 2, RoundingMode.DOWN) else BigDecimal.ZERO
    return ResumenPresupuesto(gastos, disponible, dias, diario)
}
