package com.nexo.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexo.app.ui.viewModel.*
import com.nexo.app.model.Movimiento
import com.nexo.app.util.dineroNexo
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Composable fun CompromisosCard(vm:CompromisosViewModel,movimientos:List<Movimiento>,datosListos:Boolean,onRegistrar:()->Unit) {
    val context=LocalContext.current
    var editor by remember{mutableStateOf<CompromisoRemoto?>(null)}
    var vincular by remember{mutableStateOf<CompromisoRemoto?>(null)}
    var cancelar by remember{mutableStateOf<CompromisoRemoto?>(null)}
    var historial by remember{mutableStateOf(false)}
    var verTodos by remember{mutableStateOf(false)}
    var nombre by remember{mutableStateOf("")}
    var importe by remember{mutableStateOf("")}
    var fecha by remember{mutableStateOf(LocalDate.now())}
    var errorLocal by remember{mutableStateOf<String?>(null)}
    fun editar(c:CompromisoRemoto){editor=c;nombre=c.nombre;importe=c.importe;fecha=LocalDate.parse(c.fecha);errorLocal=null}
    val pendientes=vm.lista.filter{it.estado=="PENDIENTE"}.sortedBy{it.fecha}
    CajaNexo {
        Text("Próximo pago",style=MaterialTheme.typography.titleMedium)

        if(vm.ocupado)LinearProgressIndicator(Modifier.fillMaxWidth())
        vm.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        if(vm.listo && pendientes.isNotEmpty()) {
            val proximo=pendientes.first()
            Text(proximo.nombre,style=MaterialTheme.typography.titleLarge)
            Text("${dineroNexo(proximo.importe.toBigDecimal())} · ${proximo.fecha}",color=NexoAcento)
            if(LocalDate.parse(proximo.fecha)<=LocalDate.now()) Text(if(LocalDate.parse(proximo.fecha)<LocalDate.now()) "Vencido" else "Vence hoy",color=NexoGasto)
        }
        if(vm.listo && pendientes.isEmpty()) Text("Sin pagos pendientes",color=NexoTextoSecundario)
        DetalleNexo("Administrar pagos${if(pendientes.isNotEmpty()) " (${pendientes.size})" else ""}") {
        if(vm.listo) {
            if(pendientes.isEmpty())Text("No tienes pagos pendientes. Agrega el primero cuando lo necesites.",color=NexoTextoSecundario)
            (if(verTodos)pendientes else pendientes.take(3)).forEach{c->
                HorizontalDivider()
                Text(c.nombre,style=MaterialTheme.typography.titleMedium)
                Text("${dineroNexo(c.importe.toBigDecimal())} · ${c.fecha}")
                Text(if(LocalDate.parse(c.fecha)<LocalDate.now())"Vencido · sigue reservado" else if(LocalDate.parse(c.fecha)==LocalDate.now())"Vence hoy" else "Pendiente",color=NexoAcento)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    TextButton(enabled=!vm.ocupado,onClick={editar(c)}){Text("Editar")}
                    TextButton(enabled=!vm.ocupado && datosListos,onClick={vincular=c}){Text("Ya lo pagué")}
                    TextButton(enabled=!vm.ocupado,onClick={cancelar=c}){Text("Cancelar")}
                }
            }
            if(pendientes.size>3)TextButton(onClick={verTodos=!verTodos}){Text(if(verTodos)"Mostrar menos" else "Ver los ${pendientes.size} pagos")}
        }
        Button(enabled=vm.listo && !vm.ocupado,onClick={editar(CompromisoRemoto(UUID.randomUUID().toString(),"","",LocalDate.now().toString()))},modifier=Modifier.fillMaxWidth()){Text("+ Planear un pago")}
        Row {
            TextButton(enabled=!vm.ocupado,onClick={vm.actualizar()}){Text("Actualizar pagos")}
            TextButton(enabled=vm.listo && !vm.ocupado,onClick={historial=true}){Text("Historial")}
        }
        Text("Planear no mueve dinero. Confirma cada pago vinculando el gasto real.",style=MaterialTheme.typography.bodySmall,color=NexoTextoSecundario)
        }
    }
    editor?.let{original->AlertDialog(onDismissRequest={if(!vm.ocupado)editor=null},title={Text(if(original.version==0)"Planear un pago" else "Editar pago")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value=nombre,onValueChange={nombre=it.take(80)},label={Text("Concepto")},singleLine=true,enabled=!vm.ocupado)
        OutlinedTextField(value=importe,onValueChange={importe=it.take(20)},label={Text("Importe en MXN")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,enabled=!vm.ocupado)
        TextButton(enabled=!vm.ocupado,onClick={DatePickerDialog(context,{_,y,m,d->fecha=LocalDate.of(y,m+1,d)},fecha.year,fecha.monthValue-1,fecha.dayOfMonth).show()}){Text("Vencimiento: $fecha")}
        Text("Cada registro representa un solo vencimiento. Puedes repetirlo el próximo mes desde el historial.")
        (errorLocal?:vm.error)?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=vm.listo && !vm.ocupado,onClick={
        val monto=importe.trim().replace(',','.')
        if(nombre.isBlank() || !Regex("[0-9]{1,9}(\\.[0-9]{1,2})?").matches(monto) || monto.toBigDecimalOrNull()?.let{it>BigDecimal.ZERO}!=true)errorLocal="Agrega un concepto y un importe positivo con máximo dos decimales."
        else vm.guardar(original.copy(nombre=nombre.trim(),importe=monto,fecha=fecha.toString())){editor=null}
    }){Text(if(vm.ocupado)"Guardando…" else "Guardar")}},dismissButton={TextButton(enabled=!vm.ocupado,onClick={editor=null}){Text("Cerrar")}})}
    cancelar?.let{c->AlertDialog(onDismissRequest={if(!vm.ocupado)cancelar=null},title={Text("¿Cancelar ${c.nombre}?")},text={Column{Text("Dejará de reservarse en tu presupuesto. Se conserva en el historial y puedes restaurarlo.");vm.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={TextButton(enabled=vm.listo && !vm.ocupado,onClick={vm.guardar(c.copy(estado="CANCELADO")){cancelar=null}}){Text("Cancelar pago")}},dismissButton={TextButton(enabled=!vm.ocupado,onClick={cancelar=null}){Text("Volver")}})}
    vincular?.let{c->
        val usados=vm.lista.mapNotNull{it.movimiento}.toSet()
        val candidatos=movimientos.filter{it.monto.isFinite() && it.monto<0 && BigDecimal.valueOf(it.monto).abs().compareTo(c.importe.toBigDecimal())==0 && it.idMovimiento !in usados}.sortedByDescending{it.fecha}
        AlertDialog(onDismissRequest={if(!vm.ocupado)vincular=null},title={Text("Vincular gasto real")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("${c.nombre} · ${dineroNexo(c.importe.toBigDecimal())}")
            Text("Selecciona el gasto que corresponde. No se creará otro movimiento.")
            if(candidatos.isEmpty())Text("No hay un gasto disponible por este importe. Regístralo primero; si pagaste otra cantidad, edita el pago previsto.")
            candidatos.forEach{m->OutlinedButton(enabled=vm.listo && !vm.ocupado,onClick={vm.guardar(c.copy(estado="PAGADO",movimiento=m.idMovimiento)){vincular=null}}){Text("${m.fecha} · ${m.descripcion.orEmpty()}")}}
            vm.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }},confirmButton={TextButton(enabled=!vm.ocupado,onClick={vincular=null;onRegistrar()}){Text("Registrar gasto")}},dismissButton={TextButton(enabled=!vm.ocupado,onClick={vincular=null}){Text("Cerrar")}})
    }
    if(historial)AlertDialog(onDismissRequest={if(!vm.ocupado)historial=false},title={Text("Pagos resueltos")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        val resueltos=vm.lista.filter{it.estado!="PENDIENTE"}.sortedByDescending{it.fecha}
        if(resueltos.isEmpty())Text("Aquí aparecerán los pagos confirmados y cancelados.")
        resueltos.forEach{c->
            Text("${c.nombre} · ${dineroNexo(c.importe.toBigDecimal())}")
            Text("${c.fecha} · ${if(c.estado=="PAGADO")"Pagado" else "Cancelado"}")
            if(c.estado=="CANCELADO")TextButton(enabled=vm.listo && !vm.ocupado,onClick={vm.guardar(c.copy(estado="PENDIENTE")){}}){Text("Restaurar pendiente")}
            else TextButton(enabled=vm.listo && !vm.ocupado,onClick={vm.guardar(c.copy(estado="PENDIENTE",movimiento=null)){}}){Text("Desvincular gasto")}
            TextButton(enabled=vm.listo && !vm.ocupado,onClick={historial=false;editar(c.copy(id=UUID.randomUUID().toString(),fecha=LocalDate.parse(c.fecha).plusMonths(1).toString(),estado="PENDIENTE",movimiento=null,version=0))}){Text("Repetir próximo mes")}
            HorizontalDivider()
        }
        Text("Desvincular no elimina el gasto: el pago volverá a reservarse hasta que lo resuelvas.")
        vm.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=!vm.ocupado,onClick={historial=false}){Text("Cerrar")}})
}
