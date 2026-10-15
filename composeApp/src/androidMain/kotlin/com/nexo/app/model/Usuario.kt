package com.nexo.app.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Usuario(
    //@SerialName("IdUsuario")
    //val idUsuario: Int = 0, NO ES NECESARIO PORQUE LA BD LOS CREA AUTOMATICAMENTE
    @SerialName("nombre")
    val nombre: String,
    @SerialName("correo")
    val correo: String,
    @SerialName("edad")
    val edad: Int,
    @SerialName("password")
    val password: String,

    //@Contextual
    //@SerialName("FechaRegistro")
    //val fechaRegistro: Instant? = null,
    //@SerialName("Puntos")
    //val puntos: Int = 0
)