package com.nexo.app.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.nexo.app.ui.screens.*
import com.nexo.app.ui.viewModel.BienestarViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.nexo.app.data.local.SessionManager
import com.nexo.app.ui.screens.AddMovimientoScreen
import com.nexo.app.ui.screens.GastosScreen
import com.nexo.app.ui.screens.HomeScreen
import com.nexo.app.ui.screens.LoginScreen
import com.nexo.app.ui.screens.MainHome
import com.nexo.app.ui.screens.MetasScreen
import com.nexo.app.ui.screens.PerfilScreen
import com.nexo.app.ui.screens.RegistroScreen
import com.nexo.app.ui.screens.SuccessScreen
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.viewmodel.MetasViewModel
import com.nexo.app.ui.viewModel.PerfilViewModel

@Composable
fun AppNavigation(sessionManager: SessionManager) {
    val navController = rememberNavController()

    // 1. ViewModel de Movimientos con SessionManager
    val  movimientosVM : MovimientosViewModel = viewModel(
        factory = object : ViewModelProvider.Factory{
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MovimientosViewModel(sessionManager) as T
            }
        }
    )

    val bienestar: BienestarViewModel = viewModel(factory = object: ViewModelProvider.Factory {
        override fun <T: ViewModel> create(modelClass:Class<T>):T = BienestarViewModel(sessionManager) as T
    })
    CompositionLocalProvider(LocalEstiloNexo provides (bienestar.estado?.habitos?.tema ?: "clasico")) {
    MaterialTheme(shapes=androidx.compose.material3.Shapes(small=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),medium=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),large=androidx.compose.foundation.shape.RoundedCornerShape(26.dp)),colorScheme=darkColorScheme(primary=NexoAcento,onPrimary=NexoFondo,surface=NexoSuperficie,background=NexoFondo,onSurface=androidx.compose.ui.graphics.Color.White,onBackground=androidx.compose.ui.graphics.Color.White,secondary=NexoAcento,secondaryContainer=NexoSuperficie,onSecondaryContainer=NexoAcento,surfaceVariant=NexoSuperficie,onSurfaceVariant=NexoTextoSecundario,outline=NexoTextoSecundario.copy(alpha=0.45f))) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {



        //SE INICIA AQUI AL INICIAR LA APP GRACIAS AL "startDestination"
        composable(Routes.LOGIN) {
            LoginScreen(
                navController = navController,
                onIrARegistro = {
                    navController.navigate(Routes.REGISTRO)
                },
                sessionManager = sessionManager,
                movimientosVM = movimientosVM
            )
        }
        //Se dirige aquí en el caso de que el usuario no tenga una cuenta creada

        composable(Routes.REGISTRO) {
            RegistroScreen(
                navController = navController, //Aqui se le pasa como parametro el navController
                onVolverLogin = { navController.popBackStack() }
            )
        }

        //Se dirige aquí al crear cuenta, posterior iniciar sesión

        composable(Routes.HOME) {
            HomeScreen(
                onIrLogin = {
                    navController.navigate(Routes.HOME)
                },
                onIrRegistro = {
                    navController.navigate(Routes.REGISTRO)
                }
            )
        }

        //Se deben agregar las rutas de las demás pantallas
        composable(Routes.MAIN_HOME) {
            MainHome(navController, movimientosVM
            )
        }

        composable(Routes.MOVIMIENTOS){
            GastosScreen(navController, movimientosVM)
        }

        composable(Routes.METAS){
            MetasScreen(navController,  viewModel = bienestar)

        }


        composable(Routes.PERFIL){

            PerfilScreen(navController,
                bienestar,
            )

        }

        composable(Routes.ADD_MOVIMIENTO){
            AddMovimientoScreen(navController, movimientosVM
            )
        }

        composable(Routes.ASISTENTE) { AsistenteScreen(navController, sessionManager) }
        composable(Routes.SUCCESS){
            SuccessScreen(navController, movimientosVM
            )
        }
    }
}
}
}
