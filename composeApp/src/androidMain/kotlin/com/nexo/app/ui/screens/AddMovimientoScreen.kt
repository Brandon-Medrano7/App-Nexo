package com.nexo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel

@Composable fun AddMovimientoScreen(navController:NavController,movimientosVM:MovimientosViewModel) {
    val vm=movimientosVM;val ocupado by vm.cargando.collectAsState();val pendiente=vm.pendiente!=null
    var salir by remember{mutableStateOf(false)}
    fun volver(){if(ocupado)return;if(!pendiente && (vm.montoInput.isNotBlank() || vm.descripcionInput.isNotBlank()))salir=true else navController.popBackStack()}
    BackHandler {volver()}
    Scaffold(containerColor=NexoFondo,bottomBar={
        Surface(color=NexoFondo) {Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal=20.dp,vertical=12.dp)) {
            Button(onClick={vm.registrarMovimiento{navController.navigate(Routes.SUCCESS){popUpTo(Routes.ADD_MOVIMIENTO){inclusive=true}}}},enabled=!ocupado,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)) {Text(when{ocupado->"Confirmando guardado…";pendiente->"Reintentar el mismo guardado";else->"Guardar movimiento"})}
        }}
    }) {padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item{TextButton(onClick=::volver,enabled=!ocupado){Text("‹ Volver")};NexoEncabezado("Nuevo movimiento","Ingreso o gasto")}
            if(pendiente) item{NexoAviso("Guardado pendiente","Conservamos tus datos y la solicitud original. Reintenta para confirmar; no crearemos un duplicado.")}
            item{CajaNexo {
                Text("Importe",style=MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected=!vm.esIngresoInput,onClick={vm.onEsIngresoChange(false)},enabled=!ocupado && !pendiente,label={Text("− Gasto")})
                    FilterChip(selected=vm.esIngresoInput,onClick={vm.onEsIngresoChange(true)},enabled=!ocupado && !pendiente,label={Text("+ Ingreso")})
                }
                CampoNexo(vm.montoInput,"Importe en MXN",!ocupado && !pendiente,KeyboardType.Decimal,vm::onMontoChange)
            }}
            item{DetalleNexo("Registrar por voz"){GastoPorVozCard(habilitado=!ocupado && !pendiente){b->vm.onMontoChange(b.monto.orEmpty());vm.onCategoriaChange(b.categoria.orEmpty());vm.onFechaChange(b.fecha.orEmpty());vm.onDescripcionChanged(b.concepto);vm.onEsIngresoChange(false)}}}
            item{CajaNexo {
                Text("Detalles",style=MaterialTheme.typography.titleMedium)
                Text("Categoría",color=NexoTextoSecundario)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("Trabajo","Comida","Ocio","Trans.")){c->FilterChip(selected=vm.categoriaSeleccionada==c,onClick={vm.onCategoriaChange(c)},enabled=!ocupado && !pendiente,label={Text(if(c=="Trans.")"Transporte" else c)})}}
                CampoNexo(vm.descripcionInput,"Concepto · ¿en qué fue?",!ocupado && !pendiente,cambiar=vm::onDescripcionChanged)
                FechaNexo(vm.fechaInput,!ocupado && !pendiente,etiqueta="Fecha del movimiento",cambiar=vm::onFechaChange)
                Text("${vm.descripcionInput.length}/200 caracteres",color=NexoTextoSecundario,style=MaterialTheme.typography.labelSmall)
            }}
            vm.errorRegistro?.let{error->item{NexoAviso("Revisa antes de continuar",error)}}
            item{Text("Solo registra el movimiento; no realiza pagos.",color=NexoTextoSecundario,style=MaterialTheme.typography.bodySmall)}
        }
    }
    if(salir)AlertDialog(onDismissRequest={salir=false},title={Text("¿Descartar este borrador?")},text={Text("Este movimiento todavía no se ha guardado.")},confirmButton={TextButton(onClick={vm.resetFormulario();salir=false;navController.popBackStack()}){Text("Descartar")}},dismissButton={TextButton(onClick={salir=false}){Text("Seguir editando")}})
}
