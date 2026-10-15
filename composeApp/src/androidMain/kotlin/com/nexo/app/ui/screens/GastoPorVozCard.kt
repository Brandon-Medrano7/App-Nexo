package com.nexo.app.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexo.app.util.BorradorGastoVoz
import com.nexo.app.util.interpretarGastoVoz

@Composable
fun GastoPorVozCard(habilitado: Boolean, onBorrador: (BorradorGastoVoz) -> Unit) {
    var frase by rememberSaveable { mutableStateOf("") }
    var revisar by rememberSaveable { mutableStateOf(false) }
    var escuchando by rememberSaveable { mutableStateOf(false) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }
    val resultadoVoz = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        escuchando = false
        if (resultado.resultCode == Activity.RESULT_OK) {
            val texto = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (texto.isNullOrBlank()) aviso = "No se entendió la frase. Intenta otra vez o escríbela."
            else { frase = texto; revisar = true; aviso = null }
        } else aviso = "No se recibió un dictado. Puedes volver a intentar o escribir la frase."
    }
    Column(Modifier.fillMaxWidth().background(Color(0xFF161C28), RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text("Registra un gasto con tu voz", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text("Ejemplo: gasté 85 pesos en comida. Siempre revisarás antes de guardar.", color = Color.LightGray)
        Row {
            TextButton(enabled = habilitado && !escuchando, onClick = {
                aviso = null
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Di un gasto: gasté 85 pesos en comida")
                }
                try { escuchando = true; resultadoVoz.launch(intent) }
                catch (e: ActivityNotFoundException) { escuchando = false; aviso = "Este dispositivo no tiene reconocimiento de voz disponible. Usa Escribir frase." }
                catch (e: SecurityException) { escuchando = false; aviso = "El servicio de voz no tiene acceso al micrófono. Puedes escribir la frase." }
            }) { Text(if (escuchando) "Escuchando…" else "Dictar gasto", color = Color(0xFFFFA726)) }
            TextButton(enabled = habilitado && !escuchando, onClick = { frase = ""; revisar = true; aviso = null }) { Text("Escribir frase", color = Color(0xFFFFA726)) }
        }
        Text("Se abre el servicio de voz de Android; puede usar internet para transcribir. NEXO recibe el texto y no guarda el audio.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        aviso?.let { Text(it, color = Color(0xFFFFB4AB)) }
    }
    if (revisar) {
        val borrador = interpretarGastoVoz(frase)
        AlertDialog(onDismissRequest = { revisar = false },
            title = { Text("Revisa tu gasto") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = frase, onValueChange = { frase = it.take(500) }, label = { Text("Lo que entendimos") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Importe: ${borrador.monto?.let { "$it MXN" } ?: "Por corregir"}")
                Text("Categoría: ${borrador.categoria ?: "Por elegir"}")
                Text("Fecha: ${borrador.fecha ?: "Por elegir"}")
                borrador.avisos.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                Text("Usar borrador reemplaza los campos actuales. Podrás corregirlos y después pulsar Guardar.")
            } },
            confirmButton = { TextButton(enabled = habilitado && borrador.monto != null, onClick = {
                onBorrador(borrador); aviso = "Borrador aplicado. Revisa los campos y pulsa Guardar cuando estés listo."; revisar = false
            }) { Text("Usar borrador") } },
            dismissButton = { TextButton(onClick = { revisar = false }) { Text("Cancelar") } }
        )
    }
}
