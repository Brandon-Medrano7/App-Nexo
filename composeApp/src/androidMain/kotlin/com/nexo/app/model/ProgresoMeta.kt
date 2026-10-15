package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class ProgresoMetas(
    @SerialName("IdProgreso")
    val idProgreso: Int = 0,
    @SerialName("IdMeta")
    val idMeta: Int,
    @SerialName("MontoAhorrado")
    val montoAhorrado: Double,
    @SerialName("FechaRegistro")
    val fechaRegistro: String
)