package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexo.app.data.local.SessionManager
import com.nexo.app.ui.viewModel.CompromisosViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nexo.app.navigation.BottomBarItem
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.util.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun MainHome(navController: NavController, movimientosVM: MovimientosViewModel) {
    val cargando by movimientosVM.cargando.collectAsState()
    LaunchedEffect(Unit) { movimientosVM.cargarDatosDelServidor() }
    val registros = movimientosVM.listaDeMovimientos.toList()
    val delMes = movimientosDelMes(registros)
    val context = LocalContext.current
    val usuario = movimientosVM.usuarioActual
    val pagos: CompromisosViewModel = viewModel(key="compromisos_$usuario",factory=object:ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T:ViewModel> create(modelClass:Class<T>):T = CompromisosViewModel(SessionManager(context.applicationContext),usuario) as T
    })
    LaunchedEffect(usuario, cargando) { if(usuario > 0 && !cargando) pagos.actualizar() }
    Scaffold(containerColor = NexoFondo, bottomBar = { MyBottomBar(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { navController.navigate(Routes.ADD_MOVIMIENTO) }, containerColor = NexoAcento, contentColor = NexoFondo) {
                Text("+  Agregar movimiento", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { NexoEncabezado("Inicio", "Tu plan hasta la próxima paga") }
                    TextButton(enabled=!cargando, onClick={movimientosVM.cargarDatosDelServidor()}) { Text("Actualizar",color=NexoAcento) }
                }
                if(cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                movimientosVM.errorCarga?.let { NexoAviso("No pudimos actualizar",it,"Reintentar"){movimientosVM.cargarDatosDelServidor()} }
            }
            if (movimientosVM.pendiente != null) item {
                NexoAviso("Hay un movimiento pendiente", "Retoma el guardado para confirmar que quedó registrado.", "Retomar") { navController.navigate(Routes.ADD_MOVIMIENTO) }
            }
            item { PresupuestoCard(movimientosVM.usuarioActual, registros, movimientosVM.datosListos && !cargando && movimientosVM.errorCarga == null, pagos.lista, pagos.listo && !pagos.ocupado) }
            item { CompromisosCard(pagos, registros, movimientosVM.datosListos && !cargando && movimientosVM.errorCarga == null) { navController.navigate(Routes.ADD_MOVIMIENTO) } }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Actividad reciente", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { navController.navigate(Routes.MOVIMIENTOS) }) { Text("Ver todo", color = NexoAcento) }
                }
            }
            if (movimientosVM.datosListos && registros.isEmpty()) item {
                NexoAviso("Sin movimientos", "Agrega tu primer ingreso o gasto.", "Agregar movimiento") { navController.navigate(Routes.ADD_MOVIMIENTO) }
            }
            if (movimientosVM.datosListos) {
                val recientes = filtrarMovimientos(registros, "Todos", "").take(3)
                items(recientes.size) { index -> FilaMovimientoNexo(recientes[index]) }
            }
            item {
                DetalleNexo("Resumen del mes") {
                    CajaNexo {
                        Text("Saldo registrado",color=NexoTextoSecundario)
                        Text(if(movimientosVM.datosListos) dineroNexo(totalNexo(registros)) else "—",style=MaterialTheme.typography.headlineSmall)
                        Text("Según tus registros; no es saldo bancario.",color=NexoTextoSecundario,style=MaterialTheme.typography.bodySmall)
                        Text("Ingresos: ${if(movimientosVM.datosListos) dineroNexo(totalNexo(delMes.filter{it.monto>0})) else "—"}",color=NexoIngreso)
                        Text("Gastos: ${if(movimientosVM.datosListos) dineroNexo(totalNexo(delMes.filter{it.monto<0}).abs()) else "—"}",color=NexoGasto)
                        if(movimientosVM.datosListos) delMes.filter{it.monto<0}.groupBy{it.idCategoria}.forEach{(categoria, lista)->
                            Text("${categoriaNexo(categoria)} · ${dineroNexo(totalNexo(lista).abs())}",color=NexoTextoSecundario)
                        }
                    }
                }
                OutlinedButton(onClick={navController.navigate(Routes.ASISTENTE)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Guía y herramientas →")}
            }
        }
    }
}

@Composable
fun MyBottomBar(navController: NavController) {
    val opciones = listOf(BottomBarItem.Inicio, BottomBarItem.Movimientos, BottomBarItem.Metas, BottomBarItem.Perfil)
    val entrada by navController.currentBackStackEntryAsState()
    NavigationBar(containerColor = NexoFondo, contentColor = Color.White) {
        opciones.forEach { item ->
            NavigationBarItem(selected = entrada?.destination?.route == item.route,
                onClick = { if (entrada?.destination?.route != item.route) navController.navigate(item.route) {
                    popUpTo(Routes.MAIN_HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                } },
                icon = { Icon(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(if (item.route == Routes.MAIN_HOME) "Inicio" else item.title, fontSize = 11.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = NexoAcento, selectedTextColor = NexoAcento,
                    indicatorColor = NexoSuperficie, unselectedIconColor = NexoTextoSecundario, unselectedTextColor = NexoTextoSecundario))
        }
    }
}
