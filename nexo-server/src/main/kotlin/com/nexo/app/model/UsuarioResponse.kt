package com.example.com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UsuarioResponse (

    @SerialName("IdUsuario")
    val idUsuario: Int,
    @SerialName("Nombre")
    val nombre: String? = null,
    @SerialName("Correo")
    val correo: String? = null,
    val token: String? = null

)