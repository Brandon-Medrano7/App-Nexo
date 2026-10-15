package com.nexo.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexo.app.ui.viewModel.BienestarViewModel
import java.time.LocalDate

val LocalEstiloNexo=staticCompositionLocalOf { "clasico" }
@Composable fun EstadoBienestar(vm:BienestarViewModel) {
    if(vm.ocupado) LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=NexoAcento)
    vm.mensaje?.let { Text(it,color=NexoTextoSecundario) }
    Row {
        TextButton(onClick=vm::actualizar,enabled=!vm.ocupado) { Text("Actualizar",color=NexoAcento) }
        if(vm.pendiente!=null) TextButton(onClick=vm::reintentar,enabled=!vm.ocupado) { Text("Reintentar cambio",color=NexoAcento) }
    }
}
@Composable fun CajaNexo(contenido:@Composable ColumnScope.()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=NexoSuperficie),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=contenido)
    }
}
@Composable fun CampoNexo(valor:String,titulo:String,enabled:Boolean=true,tipo:KeyboardType=KeyboardType.Text,cambiar:(String)->Unit) {
    OutlinedTextField(value=valor,onValueChange=cambiar,label={Text(titulo)},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=tipo))
}
@Composable fun FechaNexo(valor:String,enabled:Boolean,etiqueta:String="Fecha objetivo",cambiar:(String)->Unit) {
    val context=LocalContext.current
    OutlinedButton(enabled=enabled,onClick={
        val d=runCatching{LocalDate.parse(valor)}.getOrDefault(LocalDate.now().plusMonths(1))
        DatePickerDialog(context,{_,a,m,dia->cambiar(LocalDate.of(a,m+1,dia).toString())},d.year,d.monthValue-1,d.dayOfMonth).show()
    },modifier=Modifier.fillMaxWidth()) { Text("$etiqueta: ${com.nexo.app.util.fechaNexo(valor)}") }
}
@Composable fun DialogoNexo(titulo:String,vm:BienestarViewModel,cerrar:()->Unit,guardar:(()->Unit)?=null,permitirPendiente:Boolean=false,contenido:@Composable ColumnScope.()->Unit) {
    AlertDialog(onDismissRequest={if(!vm.ocupado) cerrar()},title={Text(titulo)},containerColor=NexoSuperficie,
        text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            contenido()
            vm.mensaje?.let { Text(it,color=NexoTextoSecundario) }
            if(vm.pendiente!=null) TextButton(onClick=vm::reintentar,enabled=!vm.ocupado){Text("Reintentar cambio")}
        }},confirmButton={if(guardar!=null) TextButton(onClick=guardar,enabled=!vm.ocupado && (vm.pendiente==null || permitirPendiente)){Text(if(vm.ocupado) "Guardando…" else "Confirmar")} },dismissButton={TextButton(onClick=cerrar,enabled=!vm.ocupado){Text(if(guardar==null) "Cerrar" else "Cancelar")}})
}
// Avatar vectorial provisional: los accesorios son parte del dibujo y no requieren permisos ni descargas.
@Composable fun AvatarNexo(accesorio:String,modifier:Modifier=Modifier) {
    val acento=NexoAcento
    Canvas(modifier.size(112.dp)) {
        val w=size.width; val h=size.height
        val cabeza=Path().apply {moveTo(w*.18f,h*.16f);lineTo(w*.42f,h*.30f);lineTo(w*.58f,h*.30f);lineTo(w*.82f,h*.16f);lineTo(w*.85f,h*.61f);lineTo(w*.5f,h*.90f);lineTo(w*.15f,h*.61f);close()}
        drawPath(cabeza,acento)
        val hocico=Path().apply {moveTo(w*.25f,h*.54f);lineTo(w*.5f,h*.67f);lineTo(w*.75f,h*.54f);lineTo(w*.5f,h*.88f);close()}
        drawPath(hocico,Color(0xFFFFEACB))
        drawCircle(Color(0xFF101827),w*.045f,Offset(w*.35f,h*.48f));drawCircle(Color(0xFF101827),w*.045f,Offset(w*.65f,h*.48f));drawCircle(Color(0xFF101827),w*.035f,Offset(w*.5f,h*.72f))
        when(accesorio) {
            "bufanda" -> {drawLine(Color(0xFFFF729A),Offset(w*.25f,h*.82f),Offset(w*.75f,h*.82f),h*.09f);drawLine(Color(0xFFFF729A),Offset(w*.65f,h*.82f),Offset(w*.7f,h*.98f),w*.09f)}
            "lentes" -> {drawLine(Color(0xFF1B2233),Offset(w*.22f,h*.48f),Offset(w*.78f,h*.48f),h*.12f);drawLine(Color(0xFF79DCEA),Offset(w*.25f,h*.45f),Offset(w*.39f,h*.45f),h*.02f)}
            "corona" -> {val p=Path().apply{moveTo(w*.33f,h*.27f);lineTo(w*.27f,h*.05f);lineTo(w*.43f,h*.14f);lineTo(w*.5f,0f);lineTo(w*.57f,h*.14f);lineTo(w*.73f,h*.05f);lineTo(w*.67f,h*.27f);close()};drawPath(p,Color(0xFFFFDA65))}
        }
    }
}
