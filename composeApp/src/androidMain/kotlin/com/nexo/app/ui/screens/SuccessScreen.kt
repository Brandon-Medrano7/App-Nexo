package com.nexo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.util.*

@Composable fun SuccessScreen(navController:NavController,movimientosVM:MovimientosViewModel) {
    fun inicio(){navController.navigate(Routes.MAIN_HOME){popUpTo(Routes.MAIN_HOME){inclusive=true};launchSingleTop=true}}
    BackHandler{inicio()}
    Column(Modifier.fillMaxSize().background(NexoFondo).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("✓",style=MaterialTheme.typography.displayLarge,color=NexoIngreso)
        Text("Todo en orden.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
        Text("El servidor confirmó tu movimiento.",color=NexoTextoSecundario)
        movimientosVM.ultimoGuardado?.let{FilaMovimientoNexo(it)}
        CajaNexo{Text("Un paso más para entender tu dinero.",style=MaterialTheme.typography.titleMedium);Text("Los puntos de constancia se ganan al revisar tu día desde Perfil, una vez al día.",color=NexoTextoSecundario)}
        Button(onClick=::inicio,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){Text("Volver al inicio")}
        OutlinedButton(onClick={navController.navigate(Routes.ADD_MOVIMIENTO){popUpTo(Routes.SUCCESS){inclusive=true}}},modifier=Modifier.fillMaxWidth()){Text("Registrar otro movimiento")}
    }
}
