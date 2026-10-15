package com.nexo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.util.*

@Composable
fun GastosScreen(navController: NavController, movimientosVM: MovimientosViewModel) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf("Todos") }
    val cargando by movimientosVM.cargando.collectAsState()
    LaunchedEffect(Unit) { movimientosVM.cargarDatosDelServidor() }
    val lista = filtrarMovimientos(movimientosVM.listaDeMovimientos, filtro, busqueda)
    Scaffold(containerColor = NexoFondo, bottomBar = { MyBottomBar(navController) }, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { navController.navigate(Routes.ADD_MOVIMIENTO) }, containerColor = NexoAcento) { Text("+ Agregar", color = NexoFondo) }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { NexoEncabezado("Tus movimientos", "Encuentra en qué se fue tu dinero y de dónde llegó.") }
            item {
                OutlinedTextField(value = busqueda, onValueChange = { busqueda = it }, label = { Text("Buscar concepto, categoría o fecha") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedLabelColor = NexoAcento, unfocusedLabelColor = NexoTextoSecundario, cursorColor = NexoAcento,
                        focusedBorderColor = NexoAcento, unfocusedBorderColor = NexoTextoSecundario),
                    trailingIcon = { if (busqueda.isNotEmpty()) TextButton(onClick = { busqueda = "" }) { Text("Limpiar", color = NexoAcento) } })
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf("Todos", "Gastos", "Ingresos")) { tipo ->
                    FilterChip(selected = filtro == tipo, onClick = { filtro = tipo }, label = { Text(tipo) },
                        colors = FilterChipDefaults.filterChipColors(labelColor = NexoTextoSecundario, selectedContainerColor = NexoAcento, selectedLabelColor = NexoFondo))
                } }
            }
            item {
                TextButton(enabled = !cargando, onClick = { movimientosVM.cargarDatosDelServidor() }) { Text(if (cargando) "Actualizando…" else "Actualizar", color = NexoAcento) }
                if (cargando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NexoAcento)
                movimientosVM.errorCarga?.let { NexoAviso("No pudimos cargar el historial", it) }
            }
            if (movimientosVM.datosListos) {
                item {
                    Text("${lista.size} movimientos", color = NexoTextoSecundario)
                    Text("${if (filtro == "Todos") "Balance de resultados" else "Total de resultados"}: ${dineroNexo(if (filtro == "Gastos") totalNexo(lista).abs() else totalNexo(lista))}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (lista.isEmpty()) item {
                    NexoAviso(if (busqueda.isBlank()) "Todavía no hay movimientos" else "Sin coincidencias", "Prueba otro filtro o registra un movimiento nuevo.")
                }
                items(lista) { FilaMovimientoNexo(it) }
            }
        }
    }
}
