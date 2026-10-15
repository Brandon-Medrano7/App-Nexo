package com.nexo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexo.app.model.Movimiento
import com.nexo.app.util.*
import nexo.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val NexoFondo: Color @Composable get() = when(LocalEstiloNexo.current) { "oceano" -> Color(0xFF081D2A); "bosque" -> Color(0xFF10231F); else -> Color(0xFF0B1019) }
val NexoSuperficie: Color @Composable get() = when(LocalEstiloNexo.current) { "oceano" -> Color(0xFF123242); "bosque" -> Color(0xFF1E3930); else -> Color(0xFF161E2D) }
val NexoTextoSecundario = Color(0xFFB6C3D7)
val NexoAcento: Color @Composable get() = when(LocalEstiloNexo.current) { "oceano" -> Color(0xFF79DCEA); "bosque" -> Color(0xFFB4DF88); else -> Color(0xFFFFAD4D) }
val NexoGasto = Color(0xFFFF8BAE)
val NexoIngreso = Color(0xFF74DFB4)

@Composable fun NexoAviso(titulo: String, detalle: String, accion: String? = null, onClick: () -> Unit = {}) {
    Card(colors = CardDefaults.cardColors(containerColor = NexoSuperficie), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(detalle, color = NexoTextoSecundario)
            if (accion != null) TextButton(onClick = onClick) { Text(accion, color = NexoAcento) }
        }
    }
}
@Composable fun NexoEncabezado(titulo: String, subtitulo: String) {
    Column(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titulo, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(subtitulo, color = NexoTextoSecundario, fontSize = 14.sp)
    }
}
@Composable fun FilaMovimientoNexo(m: Movimiento) {
    ItemMovimiento(m.descripcion?.takeIf { it.isNotBlank() } ?: "Sin concepto", "${categoriaNexo(m.idCategoria)} · ${fechaNexo(m.fecha)}",
        (if (m.monto < 0) "−" else "+") + dineroNexo(kotlin.math.abs(m.monto)), if (m.monto < 0) NexoGasto else NexoIngreso,
        when(m.idCategoria) { 1 -> Res.drawable.icon_trabajo; 2 -> Res.drawable.icon_comida; 3 -> Res.drawable.icon_ocio; 4 -> Res.drawable.icon_transporte; else -> Res.drawable.icon_gastos })
}
@Composable fun ItemMovimiento(titulo: String, sub: String, monto: String, colorMonto: Color, icono: DrawableResource) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NexoSuperficie), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(icono), contentDescription = null, modifier = Modifier.size(34.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(titulo, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(sub, color = NexoTextoSecundario, fontSize = 12.sp)
                Text(monto, color = colorMonto, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

/** Detalle opcional: conserva las explicaciones sin ocupar la vista principal. */
@Composable fun DetalleNexo(titulo: String, contenido: @Composable () -> Unit) {
    var abierto by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Column {
        TextButton(onClick = { abierto = !abierto }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (abierto) "− $titulo" else "+ $titulo", color = NexoAcento)
        }
        if (abierto) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { contenido() }
    }
}
