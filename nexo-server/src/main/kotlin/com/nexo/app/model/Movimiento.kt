package com.example.com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Movimiento(
    @SerialName("IdMovimiento")
    val idMovimiento: Int,
    @SerialName("IdUsuario")
    val idUsuario: Int,
    @SerialName("IdCategoria")
    val idCategoria: Int,
    @SerialName("Monto")
    val monto: Double,
    @SerialName("Fecha")
    val fecha: String,
    @SerialName("Descripcion")
    val descripcion: String?,
    @SerialName("Tipo")
    val tipo: String
)