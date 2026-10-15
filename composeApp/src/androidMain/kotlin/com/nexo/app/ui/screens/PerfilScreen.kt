package com.nexo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexo.app.model.AccionNexo
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.BienestarViewModel
import com.nexo.app.util.dineroNexo
import com.nexo.app.util.fechaNexo
import java.util.UUID

@Composable fun PerfilScreen(navController:NavController,viewModel:BienestarViewModel) {
    val vm=viewModel
    var seccion by rememberSaveable { mutableStateOf("Cuenta") }
    var dialogo by remember { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var edad by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.actualizar() }
    LaunchedEffect(vm.guardados) { dialogo="" }
    fun accion(tipo:String,opcion:String="")=vm.enviar(AccionNexo(UUID.randomUUID().toString(),tipo,opcion=opcion))
    Scaffold(containerColor=NexoFondo,bottomBar={MyBottomBar(navController)}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item { NexoEncabezado("Perfil","Tu cuenta y tus premios") }
            item { EstadoBienestar(vm) }
            item { LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(listOf("Cuenta","Constancia","Premios")) { opcion -> FilterChip(selected=seccion==opcion,onClick={seccion=opcion},label={Text(opcion)}) } } }

            vm.estado?.let { estado ->
                val p=estado.perfil;val h=estado.habitos
                if (seccion == "Cuenta") item { CajaNexo {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        AvatarNexo(h.accesorio)
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre,color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        }
                    }
                    Text(p.correo,color=NexoTextoSecundario)
                    Text("${p.edad} años · Desde ${fechaNexo(p.registro)}",color=NexoTextoSecundario)
                    OutlinedButton(onClick={nombre=p.nombre;edad=p.edad.toString();dialogo="perfil"},enabled=!vm.ocupado && vm.pendiente==null){Text("Editar mis datos")}
                } }
                if (seccion == "Constancia") item { CajaNexo {
                    Text("Tu revisión de hoy",style=MaterialTheme.typography.titleLarge,color=Color.White)
                    Text("${fechaNexo(h.fechaServidor)} · Horario de Monterrey",color=NexoTextoSecundario)
                    Text("Saldo registrado: ${dineroNexo(h.saldo.toBigDecimal())}",color=Color.White)
                    Text("Gastos del mes: ${dineroNexo(h.gastosMes.toBigDecimal())}",color=Color.White)
                    Text("También cuenta un día sin gastos.",color=NexoTextoSecundario)
                    TextButton(onClick={navController.navigate(Routes.MOVIMIENTOS)}){Text("Revisar mis movimientos")}
                    Button(onClick={dialogo="revisar"},enabled=!h.revisadoHoy && !vm.ocupado && vm.pendiente==null,modifier=Modifier.fillMaxWidth()){Text(if(h.revisadoHoy) "Revisión de hoy completada" else "Ya revisé mi día")}
                } }
                if (seccion == "Constancia") item { CajaNexo {
                    Text("Constancia que suma",style=MaterialTheme.typography.titleLarge,color=Color.White)
                    Text("${h.dias*10} puntos de constancia",color=NexoAcento,style=MaterialTheme.typography.headlineSmall)
                    Text("${h.dias} días revisados · Racha actual: ${h.racha} días",color=Color.White)
                    Text("Esta semana: ${h.semana.coerceAtMost(3)}/3 revisiones",color=NexoTextoSecundario)
                    LinearProgressIndicator(progress={(h.semana/3f).coerceIn(0f,1f)},color=NexoAcento,modifier=Modifier.fillMaxWidth())
                    Text(if(h.semana>=3) "¡Objetivo semanal logrado!" else "Intenta revisar tres días esta semana, aunque no sean seguidos.",color=NexoTextoSecundario)
                    DetalleNexo("Cómo sumas puntos") { Text("10 puntos por día revisado, una sola vez. Conservas tus premios aunque se corte la racha. No tienen valor monetario.",color=NexoTextoSecundario) }
                } }
                if (seccion == "Constancia") item { CajaNexo {
                    DetalleNexo("Ver mis logros") {
                    listOf(1 to "Primer paso",3 to "Ritmo propio",7 to "Una semana de atención",30 to "Hábito en marcha").forEach { (dias,titulo)->
                        Text("${if(h.dias>=dias) "✓" else "○"} $titulo · $dias días revisados",color=if(h.dias>=dias) NexoIngreso else NexoTextoSecundario)
                    }
                    val cumplidas=estado.metas.count{it.ahorrado.toBigDecimal()>=it.objetivo.toBigDecimal()}
                    Text("${if(cumplidas>0) "✓" else "○"} Objetivo alcanzado · $cumplidas metas completas",color=if(cumplidas>0) NexoIngreso else NexoTextoSecundario)
                    }
                } }
                if (seccion == "Premios") item { CajaNexo {
                    Text("Elige tu estilo",style=MaterialTheme.typography.titleLarge,color=Color.White)
                    Text("Desbloquea premios con días revisados.",color=NexoTextoSecundario)
                    listOf(Triple("clasico","Atardecer",0),Triple("oceano","Océano",3),Triple("bosque","Bosque",7)).forEach { (id,titulo,dias)->
                        PremioNexo(titulo,dias,h.dias,h.tema==id,!vm.ocupado && vm.pendiente==null){accion("tema",id)}
                    }
                    DetalleNexo("Accesorios del avatar") {
                    listOf(Triple("ninguno","Original",0),Triple("bufanda","Bufanda coral",3),Triple("lentes","Lentes explorador",7),Triple("corona","Corona de constancia",30)).forEach { (id,titulo,dias)->
                        Row(verticalAlignment=Alignment.CenterVertically) { AvatarNexo(id,Modifier.size(52.dp));Column(Modifier.weight(1f)){PremioNexo(titulo,dias,h.dias,h.accesorio==id,!vm.ocupado && vm.pendiente==null){accion("accesorio",id)}} }
                    }
                    }
                } }
            }
            if (seccion == "Cuenta") item { CajaNexo {
                Text("Ayuda y tu cuenta",color=Color.White,style=MaterialTheme.typography.titleLarge)
                TextButton(onClick={dialogo="ayuda"}){Text("Cómo funcionan las metas y los premios")}
                TextButton(onClick={dialogo="datos"}){Text("Acerca de tus datos")}
                OutlinedButton(onClick={dialogo="salir"},enabled=!vm.ocupado,modifier=Modifier.fillMaxWidth()){Text("Cerrar sesión")}
            } }
        }
    }
    if(dialogo.isNotEmpty()) {
        val tipo=dialogo
        DialogoNexo(when(tipo){"perfil"->"Editar mis datos";"revisar"->"Confirmar revisión diaria";"salir"->"Cerrar sesión";"datos"->"Tus datos en NEXO";else->"Usa NEXO a tu ritmo"},vm,{dialogo=""},
            if(tipo in listOf("perfil","revisar","salir")) ({when(tipo){
                "perfil"->vm.enviar(AccionNexo(UUID.randomUUID().toString(),"perfil",version=vm.estado?.perfil?.version?:0,nombre=nombre.trim(),edad=edad.toIntOrNull()?:0))
                "revisar"->accion("revisar")
                else->vm.salir { navController.navigate(Routes.LOGIN){popUpTo(navController.graph.id){inclusive=true};launchSingleTop=true} }
            };Unit}) else null,permitirPendiente=tipo=="salir") {
            when(tipo) {
                "perfil"->{CampoNexo(nombre,"Nombre",!vm.ocupado){nombre=it.take(100)};CampoNexo(edad,"Edad",!vm.ocupado,KeyboardType.Number){edad=it.take(3)};Text("El correo identifica tu cuenta y no se modifica desde esta pantalla.")}
                "revisar"->Text("Confirma que revisaste tus movimientos y el resumen de hoy. Esta revisión suma como máximo una vez por día, aunque la repitas desde otro dispositivo.")
                "salir"->Text("Tu progreso se conserva en el servidor. ${if(vm.pendiente!=null) "El cambio pendiente se conservará para confirmarlo cuando vuelvas a entrar." else "Para volver a entrar necesitarás tu correo y contraseña."}")
                "datos"->Text("Tu perfil, metas, revisiones y estilos se guardan en el servidor configurado de NEXO. Los importes son registros manuales; esta versión no consulta BBVA ni Mercado Pago. El dictado usa el servicio de voz de Android. Los premios son digitales y no se canjean por dinero.")
                else->Text("1. Crea una meta con monto y fecha.\n\n2. Registra solo ahorro que ya separaste. Si lo utilizas, registra un retiro. Archivar conserva el historial.\n\n3. Revisa tus movimientos y confirma tu día desde Perfil. Puedes participar sin gastar.\n\n4. Desbloquea estilos y accesorios con 3, 7 y 30 días acumulados. Si descansas, tus premios siguen disponibles.")
            }
        }
    }
}
@Composable private fun PremioNexo(titulo:String,requeridos:Int,dias:Int,activo:Boolean,habilitado:Boolean,elegir:()->Unit) {
    val libre=dias>=requeridos
    OutlinedButton(onClick=elegir,enabled=libre && !activo && habilitado,modifier=Modifier.fillMaxWidth()) {
        Text(when {activo->"✓ $titulo · En uso";libre->"Usar $titulo";else->"$titulo · Faltan ${requeridos-dias} días"})
    }
}
