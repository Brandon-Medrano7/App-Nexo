package com.nexo.app.navigation

import androidx.compose.runtime.Composable
import com.nexo.app.data.local.SessionManager

@Composable
fun App(sessionManager: SessionManager) {

    AppNavigation(sessionManager = sessionManager)

}