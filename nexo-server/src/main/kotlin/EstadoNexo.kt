package com.example

import kotlinx.serialization.Serializable

@Serializable data class MetaNexo(val id:Int, val nombre:String, val objetivo:String, val ahorrado:String, val fecha:String, val activa:Boolean, val version:Int, val historial:List<AporteNexo> = emptyList())
@Serializable data class PerfilNexo(val nombre:String, val correo:String, val edad:Int, val registro:String, val version:Int)
@Serializable data class HabitosNexo(val dias:Int=0, val racha:Int=0, val semana:Int=0, val revisadoHoy:Boolean=false, val tema:String="clasico", val accesorio:String="ninguno", val saldo:String="0.00", val gastosMes:String="0.00", val fechaServidor:String="")
@Serializable data class EstadoNexo(val perfil:PerfilNexo, val metas:List<MetaNexo>, val habitos:HabitosNexo)
@Serializable data class AccionNexo(val solicitud:String, val accion:String, val meta:Int=0, val version:Int=0, val nombre:String="", val importe:String="", val fecha:String="", val edad:Int=0, val opcion:String="")
@Serializable data class MensajeNexo(val mensaje:String)

@Serializable data class AporteNexo(val importe:String,val fecha:String,val retiro:Boolean=false)
