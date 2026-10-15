package com.example.com.nexo.app.model

import kotlinx.serialization.Serializable

@Serializable
data class UsuarioLogin (
    val correo: String,
    val password: String,
)