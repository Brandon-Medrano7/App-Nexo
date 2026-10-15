package com.example.com.nexo.app.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Usuario(
//@SerialName("IdUsuario")
//val idUsuario: Int = 0, LA BD LOS CREA AUTOMATICAMENTE

@SerialName("nombre")
val nombre: String,

@SerialName("edad")
val edad: Int,

@SerialName("correo")
val correo: String,

@SerialName("password")
val password: String
)