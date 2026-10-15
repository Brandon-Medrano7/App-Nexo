package com.nexo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nexo.app.data.local.SessionManager
import com.nexo.app.navigation.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)

        // 1. Inicializamos el SessionManager aquí
        val sessionManager = SessionManager(this)

        setContent {
            App(sessionManager)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App()
}