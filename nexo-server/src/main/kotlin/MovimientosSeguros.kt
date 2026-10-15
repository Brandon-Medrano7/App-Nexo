package com.example

import db.*
import com.example.com.nexo.app.model.Movimiento
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import java.security.MessageDigest
import java.util.UUID

object RecibosMovimiento:Table("NexoRecibosMovimiento") {
    val usuario=integer("IdUsuario").references(Usuarios.id)
    val solicitud=varchar("Solicitud",36)
    val huella=varchar("Huella",64)
    val movimiento=integer("IdMovimiento").references(Movimientos.id)
    override val primaryKey=PrimaryKey(usuario,solicitud)
}
fun crearTablaRecibos()=transaction(DatabaseFactory.database){SchemaUtils.create(RecibosMovimiento)}
fun validarMovimiento(m:Movimiento,hoy:LocalDate=hoyNexo()) {
    require(m.monto.isFinite() && m.monto!=0.0 && BigDecimal.valueOf(m.monto).stripTrailingZeros().scale()<=2 && kotlin.math.abs(m.monto)<=999999999.99) {"Revisa el importe: máximo dos decimales y mayor que cero."}
    require(m.idCategoria in 1..4) {"Elige una categoría válida."}
    require(m.tipo==if(m.monto>0) "Ingreso" else "Gasto") {"El tipo no coincide con el importe."}
    val fecha=runCatching{LocalDate.parse(m.fecha)}.getOrNull()
    require(fecha!=null && fecha<=hoy && fecha>=LocalDate.of(1900,1,1)) {"Elige una fecha válida, de hoy o anterior."}
    require(m.descripcion?.trim()?.length in 1..200) {"Agrega un concepto de hasta 200 caracteres."}
}
interface RegistroMovimientos {
    suspend fun listar(usuario:Int):List<Movimiento>
    suspend fun guardar(usuario:Int,solicitud:String,m:Movimiento):Movimiento
}
class MovimientosSql:RegistroMovimientos {
    override suspend fun listar(usuario:Int)=DatabaseFactory.obtenerMovimientosPorUsuario(usuario)
    override suspend fun guardar(usuario:Int,solicitud:String,m:Movimiento):Movimiento=DatabaseFactory.dbQuery {
        require(runCatching{UUID.fromString(solicitud).toString()==solicitud}.getOrDefault(false)){"Falta el identificador del guardado. Actualiza la app."}
        require(m.idUsuario==usuario){"La cuenta del movimiento no coincide con la sesión."}
        org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec("SELECT IdUsuario FROM Usuarios WITH (UPDLOCK,HOLDLOCK) WHERE IdUsuario=$usuario"){require(it.next())}
        val huella=MessageDigest.getInstance("SHA-256").digest(Json.encodeToString(m).toByteArray()).joinToString(""){"%02x".format(it)}
        val previo=RecibosMovimiento.selectAll().where{(RecibosMovimiento.usuario eq usuario) and (RecibosMovimiento.solicitud eq solicitud)}.singleOrNull()
        if(previo!=null) {
            if(previo[RecibosMovimiento.huella]!=huella) throw ConflictoNexo("El guardado pendiente tiene otros datos. Confirma primero ese movimiento.")
            return@dbQuery m.copy(idMovimiento=previo[RecibosMovimiento.movimiento])
        }
        validarMovimiento(m)
        val id=Movimientos.insert {it[idUsuario]=usuario;it[idCategoria]=m.idCategoria;it[monto]=BigDecimal.valueOf(m.monto);it[tipo]=m.tipo;it[fecha]=LocalDate.parse(m.fecha);it[descripcion]=m.descripcion!!.trim()}[Movimientos.id]
        RecibosMovimiento.insert{it[RecibosMovimiento.usuario]=usuario;it[RecibosMovimiento.solicitud]=solicitud;it[RecibosMovimiento.huella]=huella;it[movimiento]=id}
        m.copy(idMovimiento=id,descripcion=m.descripcion!!.trim())
    }
}
