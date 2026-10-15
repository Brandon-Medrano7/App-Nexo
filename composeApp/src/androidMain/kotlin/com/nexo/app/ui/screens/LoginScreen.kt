package com.nexo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nexo.app.data.local.SessionManager
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.components.InputField
import com.nexo.app.ui.viewModel.LoginViewModel
import com.nexo.app.ui.viewModel.MovimientosViewModel
import org.jetbrains.compose.resources.painterResource
import nexo.composeapp.generated.resources.*

@Composable fun AccesoNexo(titulo:String,detalle:String,contenido:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF162339),NexoFondo))).safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Image(painterResource(Res.drawable.logo_nexo),contentDescription=null,modifier=Modifier.size(70.dp))
            Column {Text("NEXO",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=Color.White);Text("TU DINERO, A TU RITMO",style=MaterialTheme.typography.labelSmall,color=NexoAcento)}
        }
        Spacer(Modifier.height(8.dp))
        Text(titulo,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,color=Color.White)
        Text(detalle,color=NexoTextoSecundario)
        contenido()
        Spacer(Modifier.height(12.dp))
    }
}
@Composable fun LoginScreen(navController:NavController,onIrARegistro:()->Unit,sessionManager:SessionManager,movimientosVM:MovimientosViewModel) {
    val vm:LoginViewModel=viewModel(factory=object:ViewModelProvider.Factory{override fun <T:ViewModel> create(modelClass:Class<T>):T=LoginViewModel(sessionManager) as T})
    val exito by vm.loginExitoso.collectAsState();val mensaje by vm.mensajeUI.collectAsState();val ocupado by vm.cargando.collectAsState()
    var ayuda by remember {mutableStateOf(false)}
    LaunchedEffect(exito){if(exito){navController.navigate(Routes.MAIN_HOME){popUpTo(Routes.LOGIN){inclusive=true};launchSingleTop=true};vm.resetLoginState()}}
    AccesoNexo("Qué bueno verte\nde nuevo.","Organiza tus gastos, cuida tu próxima paga y avanza hacia tus metas.") {
        if(navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("cuentaCreada")==true) NexoAviso("Tu cuenta está lista","Entra con tu correo y contraseña.")
        CajaNexo {
            Text("Entra a tu cuenta",style=MaterialTheme.typography.titleLarge)
            InputField(vm.correo,"Correo electrónico",enabled=!ocupado,onValueChange=vm::onCorreoChange)
            InputField(vm.password,"Contraseña",true,enabled=!ocupado,onValueChange=vm::onPasswordChange)
            mensaje?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Button(onClick=vm::login,enabled=!ocupado,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){Text(if(ocupado)"Entrando…" else "Iniciar sesión")}
            TextButton(onClick={ayuda=true},enabled=!ocupado){Text("Ayuda para entrar")}
        }
        OutlinedButton(onClick=onIrARegistro,enabled=!ocupado,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Soy nuevo · Crear cuenta")}
        Text("Un espacio para entender tus registros. NEXO no mueve dinero entre tus cuentas.",style=MaterialTheme.typography.bodySmall,color=NexoTextoSecundario)
    }
    if(ayuda) AlertDialog(onDismissRequest={ayuda=false},title={Text("Recupera el acceso")},text={Text("Revisa que uses el correo con el que te registraste y respeta mayúsculas en la contraseña. Si el servidor está apagado, vuelve a intentarlo cuando esté disponible. Esta versión todavía no envía correos de recuperación.")},confirmButton={TextButton(onClick={ayuda=false}){Text("Entendido")}})
}
