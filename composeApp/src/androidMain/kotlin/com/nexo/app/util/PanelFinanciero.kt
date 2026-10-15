package com.nexo.app.util

import com.nexo.app.model.Movimiento
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun dineroNexo(monto: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(monto)
fun dineroNexo(monto: Double): String = if (monto.isFinite()) dineroNexo(BigDecimal.valueOf(monto)) else "Importe no válido"
fun categoriaNexo(id: Int) = when (id) { 1 -> "Trabajo"; 2 -> "Comida"; 3 -> "Ocio"; 4 -> "Transporte"; else -> "Otros" }
fun fechaNexo(fecha: String): String = runCatching { LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-MX"))) }.getOrDefault(fecha)
fun totalNexo(movimientos: List<Movimiento>): BigDecimal = movimientos.filter { it.monto.isFinite() }.fold(BigDecimal.ZERO) { total, m -> total + BigDecimal.valueOf(m.monto) }
fun movimientosDelMes(movimientos: List<Movimiento>, hoy: LocalDate = LocalDate.now()): List<Movimiento> = movimientos.filter {
    runCatching { val fecha = LocalDate.parse(it.fecha); fecha >= hoy.withDayOfMonth(1) && fecha <= hoy }.getOrDefault(false)
}
fun filtrarMovimientos(movimientos: List<Movimiento>, tipo: String, busqueda: String): List<Movimiento> = movimientos.filter {
    (tipo == "Todos" || tipo == "Gastos" && it.monto < 0 || tipo == "Ingresos" && it.monto > 0) &&
    (busqueda.isBlank() || listOf(it.descripcion.orEmpty(), categoriaNexo(it.idCategoria), it.fecha).any { campo -> campo.contains(busqueda.trim(), ignoreCase = true) })
}.sortedWith(compareByDescending<Movimiento> { it.fecha }.thenByDescending { it.idMovimiento })
