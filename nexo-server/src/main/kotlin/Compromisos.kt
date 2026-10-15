package com.example

import db.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Serializable data class CompromisoDto(val id:String,val nombre:String,val importe:String,val fecha:String,val estado:String="PENDIENTE",val movimiento:Int?=null,val version:Int=0)
class ConflictoCompromiso(message:String):IllegalStateException(message)
fun validarCompromiso(c:CompromisoDto) {
    require(runCatching{UUID.fromString(c.id).toString()==c.id}.getOrDefault(false)){"Identificador inválido."}
    require(c.nombre.trim().length in 1..80){"Escribe un nombre de 1 a 80 caracteres."}
    require(Regex("[0-9]{1,9}(\\.[0-9]{1,2})?").matches(c.importe) && c.importe.toBigDecimal()>BigDecimal.ZERO){"Escribe un importe positivo con máximo dos decimales."}
    val fecha=runCatching{LocalDate.parse(c.fecha)}.getOrNull()
    require(fecha!=null && fecha>=LocalDate.of(2000,1,1) && fecha<=LocalDate.of(2100,12,31)){"Revisa la fecha de vencimiento."}
    require(c.estado in setOf("PENDIENTE","PAGADO","CANCELADO") && (c.estado=="PAGADO")== (c.movimiento!=null)){"Un pago necesita un gasto registrado; un pendiente no debe tenerlo."}
    require(c.movimiento==null || c.movimiento>0){"Elige un gasto válido."}
    require(c.version in 0..1000000000){"Versión inválida."}
}
fun reservadoCompromisos(lista:List<CompromisoDto>,pago:LocalDate):BigDecimal = lista.filter{it.estado=="PENDIENTE" && LocalDate.parse(it.fecha)<pago}.fold(BigDecimal.ZERO){total,c->total+c.importe.toBigDecimal()}

object Compromisos:Table("NexoCompromisos") {
    val id=varchar("IdCompromiso",36)
    val usuario=integer("IdUsuario").references(Usuarios.id)
    val nombre=varchar("Nombre",80)
    val importe=decimal("Importe",11,2)
    val fecha=date("Vencimiento")
    val estado=varchar("Estado",12)
    val movimiento=integer("IdMovimiento").references(Movimientos.id).nullable()
    val version=integer("Version")
    override val primaryKey=PrimaryKey(id)
    init {index(false,usuario,fecha)}
}
fun crearTablaCompromisos()=transaction(DatabaseFactory.database){SchemaUtils.create(Compromisos)}
interface AlmacenCompromisos {suspend fun listar(usuario:Int):List<CompromisoDto>;suspend fun guardar(usuario:Int,c:CompromisoDto):CompromisoDto}
class CompromisosSql:AlmacenCompromisos {
    private fun ResultRow.dto()=CompromisoDto(this[Compromisos.id],this[Compromisos.nombre],this[Compromisos.importe].toPlainString(),this[Compromisos.fecha].toString(),this[Compromisos.estado],this[Compromisos.movimiento],this[Compromisos.version])
    override suspend fun listar(usuario:Int)=DatabaseFactory.dbQuery{Compromisos.selectAll().where{Compromisos.usuario eq usuario}.orderBy(Compromisos.fecha).map{it.dto()}}
    override suspend fun guardar(usuario:Int,c:CompromisoDto):CompromisoDto=DatabaseFactory.dbQuery {
        validarCompromiso(c)
        TransactionManager.current().exec("SELECT IdUsuario FROM Usuarios WITH (UPDLOCK,HOLDLOCK) WHERE IdUsuario=$usuario"){require(it.next()){"Cuenta no disponible."}}
        val actual=Compromisos.selectAll().where{Compromisos.id eq c.id}.singleOrNull()
        if(actual!=null && actual[Compromisos.usuario]!=usuario)throw ConflictoCompromiso("Identificador no disponible.")
        val anterior=actual?.dto()
        val normal=c.copy(nombre=c.nombre.trim(),importe=c.importe.toBigDecimal().setScale(2).toPlainString())
        if(anterior!=null && anterior.copy(version=normal.version)==normal && anterior.version in normal.version..normal.version+1)return@dbQuery anterior
        if(anterior==null && c.version!=0 || anterior!=null && anterior.version!=c.version)throw ConflictoCompromiso("Este pago cambió. Actualiza antes de continuar.")
        if(c.estado=="PAGADO") {
            val m=Movimientos.selectAll().where{(Movimientos.id eq c.movimiento!!) and (Movimientos.idUsuario eq usuario)}.singleOrNull()
            require(m!=null && m[Movimientos.monto].signum()<0 && m[Movimientos.monto].abs().compareTo(c.importe.toBigDecimal())==0 && m[Movimientos.fecha]<=hoyNexo()){"Elige un gasto de tu cuenta por el importe exacto. Si pagaste otra cantidad, edita primero el pago previsto."}
            if(Compromisos.selectAll().where{(Compromisos.usuario eq usuario) and (Compromisos.movimiento eq c.movimiento)}.any{it[Compromisos.id]!=c.id})throw ConflictoCompromiso("Ese gasto ya está asociado a otro pago.")
        }
        val nuevo=normal.copy(version=c.version+1)
        fun escribir(s:org.jetbrains.exposed.sql.statements.UpdateBuilder<*>) {
            s[Compromisos.nombre]=nuevo.nombre;s[Compromisos.importe]=nuevo.importe.toBigDecimal();s[Compromisos.fecha]=LocalDate.parse(nuevo.fecha);s[Compromisos.estado]=nuevo.estado;s[Compromisos.movimiento]=nuevo.movimiento;s[Compromisos.version]=nuevo.version
        }
        if(actual==null)Compromisos.insert{it[id]=c.id;it[Compromisos.usuario]=usuario;escribir(it)}
        else Compromisos.update({(Compromisos.id eq c.id) and (Compromisos.usuario eq usuario)}){escribir(it)}
        nuevo
    }
}
fun Application.configureCompromisos(almacen:AlmacenCompromisos=CompromisosSql()) {
    routing {route("/compromisos") {
        get {
            val usuario=call.usuarioNexo()?:return@get
            try{call.respond(almacen.listar(usuario))}catch(e:Exception){if(e is CancellationException)throw e;call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos cargar tus pagos. Intenta de nuevo."))}
        }
        put {
            val usuario=call.usuarioNexo()?:return@put
            try{val c=call.receive<CompromisoDto>();validarCompromiso(c);call.respond(almacen.guardar(usuario,c))}
            catch(e:ConflictoCompromiso){call.respond(HttpStatusCode.Conflict,MensajeNexo(e.message!!))}
            catch(e:IllegalArgumentException){call.respond(HttpStatusCode.BadRequest,MensajeNexo(e.message?:"Revisa los datos."))}
            catch(e:io.ktor.server.plugins.BadRequestException){call.respond(HttpStatusCode.BadRequest,MensajeNexo("Revisa los datos del pago."))}
            catch(e:Exception){if(e is CancellationException)throw e;call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos confirmar el guardado. Actualiza antes de repetirlo."))}
        }
    }}
}
