package com.example.com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class MetaAhorro(
    val IdMeta: Int = 0,
    @SerialName("IdUsuario")
    val idUsuario: Int,
    @SerialName("NombreMeta")
    val nombreMeta: String,
    @SerialName("MontoObjetivo")
    val montoObjetivo: Double,
    @SerialName("FechaLimite")
    val fechaLimite: String,
    @SerialName("Activa")
    val activa: Boolean = true,
    @SerialName("TotalAhorrado")
    val totalAhorrado: Double
)