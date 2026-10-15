package com.nexo.app.ui.screens

import android.app.DatePickerDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexo.app.data.local.SessionManager
import com.nexo.app.ui.viewModel.PresupuestoViewModel
import com.nexo.app.ui.viewModel.PresupuestoRemoto
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.util.UUID
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexo.app.model.Movimiento
import com.nexo.app.util.resumirPresupuesto
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PresupuestoCard(userId: Int, movimientos: List<Movimiento>, datosListos: Boolean, compromisos: List<com.nexo.app.ui.viewModel.CompromisoRemoto>, pagosListos: Boolean) {
    if (userId <= 0) return
    val context = LocalContext.current
    val prefs = remember(userId) { context.getSharedPreferences("presupuesto_$userId", 0) }
    val vm: PresupuestoViewModel = viewModel(key = "presupuesto_$userId", factory = object: ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T = PresupuestoViewModel(SessionManager(context.applicationContext), userId) as T
    })
    val actual = vm.periodos.firstOrNull()
    val localPendiente = vm.cargado && actual == null && !prefs.getString("importe", null).isNullOrBlank()
    val importe = actual?.importe ?: if (localPendiente) prefs.getString("importe", "").orEmpty() else ""
    val inicio = actual?.inicio ?: if (localPendiente) prefs.getString("inicio", "").orEmpty() else ""
    val pago = actual?.pago ?: if (localPendiente) prefs.getString("pago", "").orEmpty() else ""
    var historial by remember(userId) { mutableStateOf(false) }
    var idEdicion by remember(userId) { mutableStateOf("") }
    var versionEdicion by remember(userId) { mutableStateOf(0) }
    var editar by remember(userId) { mutableStateOf(false) }
    var borrador by remember(userId) { mutableStateOf("") }
    var desde by remember(userId) { mutableStateOf(LocalDate.now()) }
    var hasta by remember(userId) { mutableStateOf(LocalDate.now().plusDays(14)) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    val fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val moneda = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")) }
    val hoy = LocalDate.now()
    val configurado = importe.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true &&
        runCatching { LocalDate.parse(pago) > LocalDate.parse(inicio) }.getOrDefault(false)
    Column(Modifier.fillMaxWidth().background(NexoSuperficie, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Text("Hasta mi próxima paga", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (vm.ocupado) Text("Sincronizando…", color = Color.LightGray)
        vm.mensaje?.let { Text(it, color = Color(0xFFFFB4AB)) }
        if (localPendiente) Text("Encontramos tu presupuesto local. Revísalo y pulsa Guardar para sincronizarlo.", color = Color(0xFFFFA726))
        if (configurado && vm.cargado) {
            val fin = LocalDate.parse(pago)
            
            if (!datosListos) {
                Text("Actualiza tus movimientos para calcular el presupuesto.", color = Color(0xFFFFB4AB))
            } else if (hoy >= fin) {
                Text("Terminó este periodo. Configura tus nuevas fechas de pago.", color = Color(0xFFFFA726))
            } else {
                val movimientosValidos = movimientos.filter { it.monto.isFinite() }.mapNotNull { m ->
                    runCatching { LocalDate.parse(m.fecha) to BigDecimal.valueOf(m.monto) }.getOrNull()
                }
                if (movimientosValidos.size != movimientos.size) {
                    Text("Hay movimientos con datos inválidos. No podemos calcular el disponible.", color = Color(0xFFFFB4AB))
                } else {
                    val resumen = resumirPresupuesto(BigDecimal(importe), LocalDate.parse(inicio), fin, movimientosValidos, hoy)
                    if (pagosListos) {
                        val plan = com.nexo.app.util.disponiblePlanificado(resumen.disponible, resumen.dias, fin, compromisos)
                        Text(moneda.format(plan.libre.abs()), color=if(plan.libre.signum()<0) NexoGasto else NexoAcento,style=MaterialTheme.typography.headlineLarge)
                        Text(if(plan.libre.signum()<0) "Faltan para cubrir tu plan" else "Libres para gastar",color=Color.White)
                        Text("${resumen.dias} días para tu paga · ${fin.format(fecha)}",color=NexoTextoSecundario)
                        DetalleNexo("Ver cálculo") {
                            Text("Presupuesto: ${moneda.format(BigDecimal(importe))}",color=Color.White)
                            Text("− Gastos: ${moneda.format(resumen.gastado)}",color=Color.White)
                            Text("− Pagos pendientes: ${moneda.format(plan.reservado)}",color=Color.White)
                            Text("Referencia diaria: ${moneda.format(plan.diario)}",color=Color.White)
                            Text("Periodo: ${LocalDate.parse(inicio).format(fecha)} → ${fin.format(fecha)}",color=NexoTextoSecundario)
                            Text("Incluye pagos vencidos y anteriores a la próxima paga. Los ingresos no aumentan el límite. No es tu saldo bancario.",color=NexoTextoSecundario,style=MaterialTheme.typography.bodySmall)
                        }
                    } else Text("Actualiza tus pagos para conocer el dinero libre.",color=NexoTextoSecundario)
                }
            }
        } else Text("Define cuánto puedes gastar desde tu última paga hasta la siguiente.", color = Color.LightGray)
        TextButton(enabled = vm.cargado && !vm.ocupado, onClick = {
            val terminado = actual != null && LocalDate.parse(actual.pago) <= hoy
            idEdicion = if (terminado || actual == null) UUID.randomUUID().toString() else actual.id
            versionEdicion = if (terminado || actual == null) 0 else actual.version
            borrador = if (terminado) "" else importe
            desde = if (terminado) hoy else runCatching { LocalDate.parse(inicio) }.getOrDefault(hoy)
            hasta = if (terminado) hoy.plusDays(14) else runCatching { LocalDate.parse(pago) }.getOrDefault(hoy.plusDays(14))
            error = null; editar = true
        }) { Text(if (actual != null && LocalDate.parse(actual.pago) <= hoy) "Nuevo periodo" else if (configurado) "Editar presupuesto" else "Configurar presupuesto", color = Color(0xFFFFA726)) }
        DetalleNexo("Más opciones") {
            TextButton(enabled=!vm.ocupado,onClick={vm.actualizar()}){Text("Actualizar presupuesto")}
            TextButton(enabled=vm.cargado && vm.periodos.isNotEmpty(),onClick={historial=true}){Text("Periodos anteriores")}
        }
    }
    if (editar) AlertDialog(
        onDismissRequest = { if (!vm.ocupado) editar = false },
        title = { Text("Mi periodo de pago") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Indica tu presupuesto total para el periodo, antes de descontar los gastos ya registrados.")
                OutlinedTextField(value = borrador, onValueChange = { borrador = it }, label = { Text("Presupuesto en MXN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                TextButton(onClick = { DatePickerDialog(context, { _, y, m, d -> desde = LocalDate.of(y, m + 1, d) }, desde.year, desde.monthValue - 1, desde.dayOfMonth).show() }) {
                    Text("Última paga: ${desde.format(fecha)}")
                }
                TextButton(onClick = { DatePickerDialog(context, { _, y, m, d -> hasta = LocalDate.of(y, m + 1, d) }, hasta.year, hasta.monthValue - 1, hasta.dayOfMonth).show() }) {
                    Text("Próxima paga: ${hasta.format(fecha)}")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                vm.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !vm.ocupado && vm.cargado, onClick = {
            val cantidad = borrador.trim().replace(',', '.').toBigDecimalOrNull()
            when {
                cantidad == null || cantidad <= BigDecimal.ZERO || cantidad > BigDecimal("999999999.99") || cantidad.scale() > 2 -> error = "Escribe un importe positivo con máximo dos decimales."
                desde > hoy -> error = "La última paga no puede ser posterior a hoy."
                hasta <= desde || (hasta <= hoy && !localPendiente) -> error = "La próxima paga debe ser posterior a hoy y a la última paga."
                else -> {
                    vm.guardar(PresupuestoRemoto(idEdicion, cantidad.toPlainString(), desde.toString(), hasta.toString(), versionEdicion)) {
                        if (localPendiente) prefs.edit().remove("importe").remove("inicio").remove("pago").apply()
                        editar = false
                    }
                }
            }
        }) { Text(if (vm.ocupado) "Guardando…" else "Guardar") } },
        dismissButton = { TextButton(enabled = !vm.ocupado, onClick = { editar = false }) { Text("Cancelar") } }
    )
    if (historial) AlertDialog(
        onDismissRequest = { historial = false }, title = { Text("Mis periodos") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            vm.periodos.forEach { periodo ->
                Text("${LocalDate.parse(periodo.inicio).format(fecha)} → ${LocalDate.parse(periodo.pago).format(fecha)}")
                Text("Presupuesto: ${moneda.format(BigDecimal(periodo.importe))}")
                Text(if (LocalDate.parse(periodo.pago) <= hoy) "Finalizado" else "Vigente")
                Spacer(Modifier.height(16.dp))
            }
        } }, confirmButton = { TextButton(onClick = { historial = false }) { Text("Cerrar") } }
    )

}
