package com.example

import db.*
import com.example.com.nexo.app.model.Usuario
import org.jetbrains.exposed.sql.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ClavesNexo {
    private const val ITERACIONES=600000
    private val encoder=Base64.getUrlEncoder().withoutPadding()
    private fun derivar(password:String,sal:ByteArray,iteraciones:Int):ByteArray {
        val spec=PBEKeySpec(password.toCharArray(),sal,iteraciones,256)
        return try {SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded} finally {spec.clearPassword()}
    }
    fun hash(password:String):String {
        val sal=ByteArray(16).also {SecureRandom().nextBytes(it)}
        return listOf("pbkdf2",ITERACIONES.toString(),encoder.encodeToString(sal),encoder.encodeToString(derivar(password,sal,ITERACIONES))).joinToString("$")
    }
    fun esHash(valor:String)=valor.startsWith("pbkdf2$")
    fun verificar(password:String,guardada:String):Boolean {
        if(password.length>128) return false
        if(!esHash(guardada)) return MessageDigest.isEqual(password.toByteArray(),guardada.toByteArray())
        return runCatching {
            val partes=guardada.split('$');require(partes.size==4)
            val iteraciones=partes[1].toInt();require(iteraciones in 600000..1200000)
            val sal=Base64.getUrlDecoder().decode(partes[2]);val esperado=Base64.getUrlDecoder().decode(partes[3]);require(sal.size==16 && esperado.size==32)
            MessageDigest.isEqual(derivar(password,sal,iteraciones),esperado)
        }.getOrDefault(false)
    }
}
fun correoNexo(valor:String)=valor.trim().lowercase(Locale.ROOT)
fun validarCuenta(u:Usuario) {
    require(u.nombre.trim().length in 1..100) {"Escribe un nombre de hasta 100 caracteres."}
    require(u.edad in 13..120) {"Revisa tu edad (13 a 120 años)."}
    require(u.correo.trim().length<=100 && u.correo.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {"Escribe un correo válido."}
    require(u.password.length in 12..128) {"Usa una contraseña de 12 a 128 caracteres."}
}
class CorreoOcupado:IllegalStateException()
interface CuentasNexo {
    suspend fun registrar(u:Usuario)
    suspend fun autenticar(correo:String,password:String):Int?
}
class CuentasSql:CuentasNexo {
    override suspend fun registrar(u:Usuario) {
        validarCuenta(u)
        val hash=withContext(Dispatchers.Default){ClavesNexo.hash(u.password)}
        try { DatabaseFactory.dbQuery {
            if(Usuarios.selectAll().where{Usuarios.correo eq correoNexo(u.correo)}.any()) throw CorreoOcupado()
            Usuarios.insert {it[nombre]=u.nombre.trim();it[edad]=u.edad;it[correo]=correoNexo(u.correo);it[password]=hash;it[puntos]=0;it[fechaRegistro]=LocalDateTime.now()}
        } } catch(e:Exception) {
            if(e is CorreoOcupado || generateSequence<Throwable>(e){it.cause}.any { it is java.sql.SQLException && it.errorCode in listOf(2601,2627) }) throw CorreoOcupado()
            throw e
        }
    }
    override suspend fun autenticar(correo:String,password:String):Int? {
        if(correo.length>100 || password.length>128 || password.isEmpty()) return null
        val u=DatabaseFactory.dbQuery {Usuarios.selectAll().where{Usuarios.correo eq correoNexo(correo)}.singleOrNull()?.let{it[Usuarios.id] to it[Usuarios.password]}}
        val valida=withContext(Dispatchers.Default){ClavesNexo.verificar(password,u?.second?:CLAVE_INEXISTENTE)}
        if(u==null || !valida) return null
        if(!ClavesNexo.esHash(u.second)) {
            val hash=withContext(Dispatchers.Default){ClavesNexo.hash(password)}
            DatabaseFactory.dbQuery { Usuarios.update({(Usuarios.id eq u.first) and (Usuarios.password eq u.second)}) {it[Usuarios.password]=hash} }
        }
        return u.first
    }
    companion object {private val CLAVE_INEXISTENTE by lazy {ClavesNexo.hash("cuenta-inexistente-nexo")}}
}
