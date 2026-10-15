package com.nexo.app.model

data class CategoriaFinanciera(
    val IdCategoria: Int = 0,
    val Nombre: String,
    val Tipo: String // "Ingreso" o "Gasto"
)