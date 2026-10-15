package com.nexo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexo.app.model.*
import com.nexo.app.ui.viewModel.BienestarViewModel
import com.nexo.app.util.dineroNexo
import com.nexo.app.util.fechaNexo
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Composable fun MetasScreen(navController:NavController,viewModel:BienestarViewModel) {
    val vm=viewModel
    var filtro by rememberSaveable { mutableStateOf("Activas") }
    var dialogo by remember { mutableStateOf("") }
    var seleccion by remember { mutableStateOf<MetaNexo?>(null) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var importe by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf(LocalDate.now().plusMonths(1).toString()) }
    LaunchedEffect(Unit) { vm.actualizar() }
    LaunchedEffect(vm.guardados) { dialogo="" }
    val metas=vm.estado?.metas.orEmpty()
    val completas=metas.filter { it.activa && it.ahorrado.toBigDecimal()>=it.objetivo.toBigDecimal() }
    val lista=metas.filter { when(filtro) {"Archivadas"->!it.activa; "Completadas"->it in completas; else->it.activa && it !in completas} }
    fun abrir(m:MetaNexo?,accion:String) { seleccion=m; nombre=m?.nombre.orEmpty(); importe=if(accion=="editar") m?.objetivo.orEmpty() else ""; fecha=m?.fecha?:LocalDate.now().plusMonths(1).toString(); dialogo=accion }
    fun enviar(accion:String)=vm.enviar(AccionNexo(UUID.randomUUID().toString(),accion,seleccion?.id?:0,seleccion?.version?:0,nombre.trim(),importe.trim().replace(',','.'),fecha))
    Scaffold(containerColor=NexoFondo,bottomBar={MyBottomBar(navController)},floatingActionButton={if(vm.estado!=null && vm.pendiente==null) ExtendedFloatingActionButton(onClick={abrir(null,"crear")},containerColor=NexoAcento,contentColor=NexoFondo){Text("+ Nueva meta")}}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=110.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item{NexoEncabezado("Metas","Tu ahorro, con un objetivo")}
            item{EstadoBienestar(vm)}
            if(vm.estado!=null) {
                item{CajaNexo {
                    Text("${completas.size} metas completadas",style=MaterialTheme.typography.titleLarge,color=Color.White)
                    Text("Ahorro registrado en metas activas",color=NexoTextoSecundario)
                    Text(dineroNexo(metas.filter { it.activa }.fold(BigDecimal.ZERO){t,m->t+m.ahorrado.toBigDecimal()}),style=MaterialTheme.typography.headlineMedium,color=NexoIngreso)
                    DetalleNexo("Acerca de tu ahorro") { Text("Registra dinero que ya separaste. No realiza transferencias ni modifica tus movimientos.",color=NexoTextoSecundario) }
                }}
                item{LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("Activas","Completadas","Archivadas")){ f->FilterChip(selected=filtro==f,onClick={filtro=f},label={Text(f)})}}}
                if(lista.isEmpty()) item{NexoAviso("${if(filtro=="Activas") "Tu próxima meta empieza aquí" else "Sin metas en esta sección"}","Crea una meta con importe y fecha.")}
                items(lista,key={it.id}) { m ->
                    val ahorro=m.ahorrado.toBigDecimal();val objetivo=m.objetivo.toBigDecimal();val falta=(objetivo-ahorro).max(BigDecimal.ZERO)
                    val hoy=runCatching { LocalDate.parse(vm.estado!!.habitos.fechaServidor) }.getOrDefault(LocalDate.now())
                    val dias=ChronoUnit.DAYS.between(hoy,LocalDate.parse(m.fecha))
                    CajaNexo {
                        Text(m.nombre,color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        Text(when { !m.activa->"Archivada · puedes restaurarla"; falta==BigDecimal.ZERO->"¡Meta completada!"; dias<0->"Fecha alcanzada · puedes ajustar tu plan"; else->"Objetivo: ${fechaNexo(m.fecha)}"},color=if(falta==BigDecimal.ZERO) NexoIngreso else NexoTextoSecundario)
                        Text("${dineroNexo(ahorro)} de ${dineroNexo(objetivo)}",color=Color.White)
                        val progreso=ahorro.divide(objetivo,4,RoundingMode.HALF_UP).toFloat().coerceIn(0f,1f)
                        LinearProgressIndicator(progress={progreso},modifier=Modifier.fillMaxWidth(),color=NexoAcento)
                        Text("${(progreso*100).toInt()}% · Faltan ${dineroNexo(falta)}",color=NexoTextoSecundario)
                        if(m.activa && falta>BigDecimal.ZERO) Button(onClick={abrir(m,"aportar")},enabled=!vm.ocupado && vm.pendiente==null,modifier=Modifier.fillMaxWidth()){Text("Aportar")}
                        DetalleNexo("Detalles y opciones") {
                        if(m.activa && falta>BigDecimal.ZERO && dias>=0) {
                            val semanas=((dias+6)/7).coerceAtLeast(1)
                            Text("Referencia: ${dineroNexo(falta.divide(BigDecimal(semanas),2,RoundingMode.UP))} por semana. Ajústalo a tus posibilidades.",color=NexoTextoSecundario)
                        }
                        if(m.activa) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(ahorro>BigDecimal.ZERO) OutlinedButton(onClick={abrir(m,"retirar")},enabled=!vm.ocupado && vm.pendiente==null){Text("Retirar")}
                        }
                        LazyRow { item{TextButton(onClick={abrir(m,"historial")}){Text("Historial")}};item{TextButton(onClick={abrir(m,"editar")},enabled=!vm.ocupado && vm.pendiente==null){Text("Editar")}};item{TextButton(onClick={abrir(m,if(m.activa) "archivar" else "restaurar")},enabled=!vm.ocupado && vm.pendiente==null){Text(if(m.activa) "Archivar" else "Restaurar")}} }
                        }
                    }
                }
            } else if(!vm.ocupado) item { NexoAviso("Tus metas están a salvo","Actualiza para recuperarlas. Si la sesión venció, vuelve a entrar desde Perfil.") }
        }
    }
    if(dialogo.isNotEmpty()) {
        val titulo=when(dialogo){"crear"->"Nueva meta";"editar"->"Editar meta";"aportar"->"Registrar aportación";"retirar"->"Registrar retiro";"archivar"->"Archivar meta";"restaurar"->"Restaurar meta";else->"Historial de ahorro"}
        DialogoNexo(titulo,vm,{dialogo=""},if(dialogo=="historial") null else {{enviar(dialogo)}}) {
            when(dialogo) {
                "crear","editar" -> {
                    CampoNexo(nombre,"¿Para qué quieres ahorrar?",!vm.ocupado){nombre=it.take(100)}
                    CampoNexo(importe,"Objetivo en MXN",!vm.ocupado,KeyboardType.Decimal){importe=it}
                    FechaNexo(fecha,!vm.ocupado){fecha=it}
                }
                "aportar","retirar" -> {
                    Text(seleccion?.nombre.orEmpty())
                    CampoNexo(importe,"Importe en MXN",!vm.ocupado,KeyboardType.Decimal){importe=it}
                    Text("Confirma únicamente dinero que ya ${if(dialogo=="aportar") "separaste" else "retiraste"}. Esta acción solo actualiza el registro de la meta.")
                }
                "archivar" -> Text("La meta saldrá de tus metas activas. Conservaremos su ahorro e historial y podrás restaurarla.")
                "restaurar" -> Text("La meta volverá a tus metas activas con todo su progreso.")
                else -> {
                    if(seleccion?.historial.isNullOrEmpty()) Text("Aún no hay aportaciones.")
                    seleccion?.historial?.forEach { a->Text("${fechaNexo(a.fecha)} · ${if(a.retiro) "Retiro −" else "Aporte +"}${dineroNexo(a.importe.toBigDecimal())}") }
                }
            }
        }
    }
}
