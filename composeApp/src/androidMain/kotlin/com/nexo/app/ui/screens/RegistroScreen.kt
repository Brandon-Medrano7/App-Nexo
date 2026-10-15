package com.nexo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.components.InputField
import com.nexo.app.viewmodel.RegistroViewModel

@Composable fun RegistroScreen(navController:NavController,viewModel:RegistroViewModel=viewModel(),onVolverLogin:()->Unit) {
    val vm=viewModel;val exito by vm.registroExitoso.collectAsState();val ocupado by vm.cargando.collectAsState();val mensaje by vm.mensajeUI.collectAsState()
    LaunchedEffect(exito){if(exito){navController.previousBackStackEntry?.savedStateHandle?.set("cuentaCreada",true);navController.popBackStack(Routes.LOGIN,false);vm.resetRegistroEstado()}}
    AccesoNexo("Tu próximo paso\nempieza aquí.","Crea tu cuenta y empieza a darle un destino a tu dinero.") {
        CajaNexo {
            InputField(vm.nombre.value,"Nombre completo",enabled=!ocupado,onValueChange=vm::onNombreChange)
            InputField(vm.edad.value,"Edad",enabled=!ocupado,onValueChange=vm::onEdadChange)
            InputField(vm.correo.value,"Correo electrónico",enabled=!ocupado,onValueChange=vm::onCorreoChange)
            InputField(vm.password.value,"Contraseña",true,enabled=!ocupado,onValueChange=vm::onPasswordChange)
            Text("Usa una frase de al menos 12 caracteres que puedas recordar.",style=MaterialTheme.typography.bodySmall,color=NexoTextoSecundario)
            InputField(vm.confirmar.value,"Confirmar contraseña",true,enabled=!ocupado,onValueChange=vm::onConfirmarChange)
            mensaje?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Button(onClick=vm::registrar,enabled=!ocupado,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){Text(if(ocupado)"Creando tu cuenta…" else "Crear mi cuenta")}
        }
        TextButton(onClick=onVolverLogin,enabled=!ocupado,modifier=Modifier.fillMaxWidth()){Text("Ya tengo cuenta · Volver")}
    }
}
