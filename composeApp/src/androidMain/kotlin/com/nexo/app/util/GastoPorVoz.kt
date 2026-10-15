package com.nexo.app.util

import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

data class BorradorGastoVoz(val monto: String?, val categoria: String?, val fecha: String?, val concepto: String, val avisos: List<String>)

private fun normalizarVoz(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
private val unidades = listOf("cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidos", "veintitres", "veinticuatro", "veinticinco", "veintiseis", "veintisiete", "veintiocho", "veintinueve")
private val decenas = listOf("", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa")
private val centenas = listOf("", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos")
private fun numeroEnPalabras(n: Int): String = when {
    n < 30 -> unidades[n]
    n < 100 -> decenas[n / 10] + if (n % 10 == 0) "" else " y " + unidades[n % 10]
    n == 100 -> "cien"
    else -> centenas[n / 100] + if (n % 100 == 0) "" else " " + numeroEnPalabras(n % 100)
}
private val numerosVoz = (0..999).associateBy { numeroEnPalabras(it) }
private val patronNumero = Regex("(?<![\\p{L}\\d.,])(?:\\d+(?:[.,]\\d+)*|" + numerosVoz.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } + ")(?![\\p{L}\\d.,])")

/** Analizador local conservador: produce un borrador, nunca escribe movimientos. */
fun interpretarGastoVoz(frase: String, hoy: LocalDate = LocalDate.now()): BorradorGastoVoz {
    val texto = normalizarVoz(frase.trim()).replace(Regex("\\s+"), " ")
    val avisos = mutableListOf<String>()
    val ingreso = Regex("\\b(ingreso|ingrese|recibi|cobre|nomina|depositaron|pagaron|sueldo|salario|gane)\\b").containsMatchIn(texto)
    val negacion = Regex("\\b(no|menos|negativo|dev[ou]lv\\w*)\\b|[-−]\\s*\\d").containsMatchIn(texto)
    val otraMoneda = Regex("\\b(dolares|euros|usd|eur)\\b").containsMatchIn(texto)
    val milesEnPalabras = Regex("\\b(mil|miles|millon|millones)\\b").containsMatchIn(texto)
    val encontrados = patronNumero.findAll(texto).toList()
    var monto: String? = null
    if (ingreso) avisos += "Esta opción prepara gastos. Para ingresos, usa el formulario manual."
    else if (negacion || otraMoneda || milesEnPalabras) avisos += "No puedo interpretar ese importe con seguridad. Usa pesos MXN y una cantidad positiva; para miles, escribe las cifras sin separador."
    else if (encontrados.size != 1) avisos += "Indica un solo importe, por ejemplo: gasté 85 pesos en comida."
    else {
        val literal = encontrados.single().value
        val cantidad = numerosVoz[literal]?.let { BigDecimal(it) } ?: literal.replace(',', '.').toBigDecimalOrNull()
        if (cantidad == null || cantidad <= BigDecimal.ZERO || cantidad > BigDecimal("999999999.99") || cantidad.scale() > 2)
            avisos += "Revisa el importe. Usa hasta dos decimales y ningún separador de miles."
        else monto = cantidad.setScale(2).toPlainString()
    }
    val categorias = mapOf(
        "Comida" to "comida|comer|cena|desayuno|almuerzo|tacos|cafe|restaurante",
        "Trans." to "transporte|camion|autobus|metro|taxi|uber|didi|gasolina",
        "Ocio" to "ocio|cine|juego|videojuego|concierto|entretenimiento"
    ).filterValues { Regex("\\b(?:$it)\\b").containsMatchIn(texto) }.keys
    val categoria = categorias.singleOrNull()
    if (categoria == null) avisos += "Elige la categoría en el formulario."
    val fechaAmbigua = Regex("\\b(manana|anteayer|pasado|pasada|semana|mes|enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre|lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b|\\d[/-]\\d").containsMatchIn(texto) || ("ayer" in texto && Regex("\\bhoy\\b").containsMatchIn(texto))
    val fecha = when {
        fechaAmbigua -> null
        Regex("\\bayer\\b").containsMatchIn(texto) -> hoy.minusDays(1).toString()
        else -> hoy.toString()
    }
    if (fecha == null) avisos += "Selecciona la fecha en el formulario; solo interpreto hoy o ayer."
    if (frase.trim().length > 200) avisos += "El concepto se recortó a 200 caracteres. Revísalo."
    return BorradorGastoVoz(monto, categoria, fecha, frase.trim().take(200), avisos)
}
