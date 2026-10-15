package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//TRABAJAR SOBRE ESTO SOLO SI SE HARÁ LA PANTALLA DE BALANCE
@Serializable
data class BalanceGeneral(
    @SerialName("Nombre")
    val nombre: String,
    @SerialName("Balance")
    val balance: Double
)