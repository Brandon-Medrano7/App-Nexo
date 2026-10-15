package com.example.com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PerfilUsuario(
    @SerialName("IdUsuario")
    val idUsuario: Int,
    @SerialName("Nombre")
    val nombre: String,
    @SerialName("Edad")
    val edad: Int,
    @SerialName("Correo")
    val correo: String,
    @SerialName("FechaRegistro")
    val fechaRegistro: String,
    @SerialName("Puntos")
    val puntos: Int
)
