package com.example

import db.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.*
import java.util.UUID

object RetirosNexo: Table("NexoRetirosMeta") {
    val id=integer("IdRetiro").autoIncrement()
    val meta=integer("IdMeta").references(MetasAhorro.idMeta)
    val usuario=integer("IdUsuario").references(Usuarios.id)
    val importe=decimal("Importe",10,2)
    val fecha=date("Fecha")
    override val primaryKey=PrimaryKey(id)
}
object RevisionesNexo: Table("NexoRevisiones") {
    val usuario=integer("IdUsuario").references(Usuarios.id)
    val dia=date("Dia")
    override val primaryKey=PrimaryKey(usuario,dia)
}
object EstilosNexo: Table("NexoEstilos") {
    val usuario=integer("IdUsuario").references(Usuarios.id)
    val tema=varchar("Tema",20).default("clasico")
    val accesorio=varchar("Accesorio",20).default("ninguno")
    val version=integer("VersionPerfil").default(0)
    override val primaryKey=PrimaryKey(usuario)
}
object VersionesMeta: Table("NexoVersionesMeta") {
    val meta=integer("IdMeta").references(MetasAhorro.idMeta)
    val version=integer("Version").default(0)
    override val primaryKey=PrimaryKey(meta)
}
object SolicitudesNexo: Table("NexoSolicitudes") {
    val id=varchar("IdSolicitud",36)
    val usuario=integer("IdUsuario").references(Usuarios.id)
    val huella=varchar("Huella",64)
    override val primaryKey=PrimaryKey(usuario,id)
}
fun hoyNexo(): LocalDate=LocalDate.now(ZoneId.of("America/Monterrey"))
fun rachaNexo(dias:Set<LocalDate>, hoy:LocalDate):Int {
    var dia=if(hoy in dias) hoy else hoy.minusDays(1)
    var total=0
    while(dia in dias) { total++; dia=dia.minusDays(1) }
    return total
}
fun requisitoPremio(opcion:String,tema:Boolean):Int? = if(tema) when(opcion){ "clasico"->0; "oceano"->3; "bosque"->7; else->null } else when(opcion){ "ninguno"->0; "bufanda"->3; "lentes"->7; "corona"->30; else->null }
fun importeMeta(texto:String):BigDecimal {
    require(texto.matches(Regex("[0-9]{1,8}(\\.[0-9]{1,2})?"))) { "Usa un importe en MXN con máximo 2 decimales y 8 enteros." }
    return texto.toBigDecimal().also { require(it>BigDecimal.ZERO) { "El importe debe ser mayor que cero." } }
}
fun validarAccionNexo(a:AccionNexo,hoy:LocalDate=hoyNexo()) {
    require(runCatching { UUID.fromString(a.solicitud).toString()==a.solicitud }.getOrDefault(false)) { "Solicitud inválida. Vuelve a abrir el formulario." }
    require(a.version>=0)
    require(a.accion in setOf("crear","editar","archivar","restaurar","aportar","retirar","perfil","revisar","tema","accesorio")) { "Acción no disponible." }
    if(a.accion in setOf("crear","editar")) {
        require(a.nombre.trim().length in 1..100) { "Escribe un nombre de hasta 100 caracteres." }
        importeMeta(a.importe)
        val fecha=runCatching{LocalDate.parse(a.fecha)}.getOrNull()
        require(fecha!=null && fecha>=hoy && fecha<=hoy.plusYears(20)) { "Elige una fecha entre hoy y los próximos 20 años." }
    }
    if(a.accion in setOf("aportar","retirar")) importeMeta(a.importe)
    if(a.accion=="perfil") {
        require(a.nombre.trim().length in 1..100) { "Escribe tu nombre (máximo 100 caracteres)." }
        require(a.edad in 13..120) { "Revisa tu edad (13 a 120 años)." }
    }
    if(a.accion=="tema" || a.accion=="accesorio") require(requisitoPremio(a.opcion,a.accion=="tema")!=null) { "Premio no disponible." }
}
class ConflictoNexo(message:String):IllegalStateException(message)
interface AlmacenBienestar {
    suspend fun estado(usuario:Int):EstadoNexo
    suspend fun aplicar(usuario:Int,accion:AccionNexo):EstadoNexo
}
class BienestarSql:AlmacenBienestar {
    private fun metas(usuario:Int):List<MetaNexo> {
        val versiones=VersionesMeta.selectAll().associate { it[VersionesMeta.meta] to it[VersionesMeta.version] }
        val ingresos=ProgresoMetas.selectAll().where { ProgresoMetas.idUsuario eq usuario }.groupBy { it[ProgresoMetas.idMeta] }.mapValues { (_,rows)-> rows.map { AporteNexo(it[ProgresoMetas.montoAhorrado].toPlainString(),it[ProgresoMetas.fechaRegistro].toLocalDate().toString()) } }
        val retiros=RetirosNexo.selectAll().where { RetirosNexo.usuario eq usuario }.groupBy { it[RetirosNexo.meta] }.mapValues { (_,rows)-> rows.map { AporteNexo(it[RetirosNexo.importe].toPlainString(),it[RetirosNexo.fecha].toString(),true) } }
        return MetasAhorro.selectAll().where { MetasAhorro.idUsuario eq usuario }.orderBy(MetasAhorro.idMeta to SortOrder.DESC).map {
            val historial=(ingresos[it[MetasAhorro.idMeta]].orEmpty()+retiros[it[MetasAhorro.idMeta]].orEmpty()).sortedByDescending { it.fecha }
            val total=historial.fold(BigDecimal.ZERO){t,a->t+(if(a.retiro) -a.importe.toBigDecimal() else a.importe.toBigDecimal())}
            MetaNexo(it[MetasAhorro.idMeta],it[MetasAhorro.nombreMeta],it[MetasAhorro.montoObjetivo].toPlainString(),total.toPlainString(),it[MetasAhorro.fechaLimite].toString(),it[MetasAhorro.activa],versiones[it[MetasAhorro.idMeta]]?:0,historial)
        }
    }
    private fun leer(usuario:Int):EstadoNexo {
        val u=Usuarios.selectAll().where { Usuarios.id eq usuario }.single()
        val estilo=EstilosNexo.selectAll().where { EstilosNexo.usuario eq usuario }.singleOrNull()
        val dias=RevisionesNexo.selectAll().where { RevisionesNexo.usuario eq usuario }.map { it[RevisionesNexo.dia] }.toSet()
        val hoy=hoyNexo()
        val movimientos=Movimientos.selectAll().where { Movimientos.idUsuario eq usuario }.toList()
        val saldo=movimientos.fold(BigDecimal.ZERO){t,r->t+r[Movimientos.monto]}
        val gastos=movimientos.filter { it[Movimientos.fecha]>=hoy.withDayOfMonth(1) && it[Movimientos.fecha]<=hoy && it[Movimientos.monto]<BigDecimal.ZERO }.fold(BigDecimal.ZERO){t,r->t-r[Movimientos.monto]}
        return EstadoNexo(PerfilNexo(u[Usuarios.nombre],u[Usuarios.correo],u[Usuarios.edad],u[Usuarios.fechaRegistro].toLocalDate().toString(),estilo?.get(EstilosNexo.version)?:0),metas(usuario),HabitosNexo(dias.size,rachaNexo(dias,hoy),dias.count { it>=hoy.with(java.time.DayOfWeek.MONDAY) && it<=hoy },hoy in dias,estilo?.get(EstilosNexo.tema)?:"clasico",estilo?.get(EstilosNexo.accesorio)?:"ninguno",saldo.toPlainString(),gastos.toPlainString(),hoy.toString()))
    }
    override suspend fun estado(usuario:Int)=DatabaseFactory.dbQuery { leer(usuario) }
    override suspend fun aplicar(usuario:Int,accion:AccionNexo):EstadoNexo=DatabaseFactory.dbQuery {
        org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec("SELECT IdUsuario FROM Usuarios WITH (UPDLOCK,HOLDLOCK) WHERE IdUsuario=$usuario") { require(it.next()) }
        val huella=MessageDigest.getInstance("SHA-256").digest(Json.encodeToString(accion).toByteArray()).joinToString(""){"%02x".format(it)}
        val anterior=SolicitudesNexo.selectAll().where { (SolicitudesNexo.usuario eq usuario) and (SolicitudesNexo.id eq accion.solicitud) }.singleOrNull()
        if(anterior!=null) {
            if(anterior[SolicitudesNexo.huella]!=huella) throw ConflictoNexo("La solicitud ya se usó con otros datos. Actualiza.")
            return@dbQuery leer(usuario)
        }
        validarAccionNexo(accion)
        if(EstilosNexo.selectAll().where { EstilosNexo.usuario eq usuario }.empty()) EstilosNexo.insert { it[EstilosNexo.usuario]=usuario }
        when(accion.accion) {
            "crear" -> {
                val id=MetasAhorro.insert {
                    it[idUsuario]=usuario; it[nombreMeta]=accion.nombre.trim(); it[montoObjetivo]=importeMeta(accion.importe)
                    it[fechaLimite]=LocalDate.parse(accion.fecha); it[activa]=true; it[fechaCreacion]=LocalDateTime.now(ZoneId.of("America/Monterrey"))
                }[MetasAhorro.idMeta]
                VersionesMeta.insert { it[meta]=id; it[version]=1 }
            }
            "perfil" -> {
                val v=EstilosNexo.selectAll().where { EstilosNexo.usuario eq usuario }.single()[EstilosNexo.version]
                if(v!=accion.version) throw ConflictoNexo("Tu perfil cambió. Actualiza antes de editarlo.")
                Usuarios.update({ Usuarios.id eq usuario }) { it[nombre]=accion.nombre.trim(); it[edad]=accion.edad }
                EstilosNexo.update({ EstilosNexo.usuario eq usuario }) { it[version]=v+1 }
            }
            "revisar" -> {
                val hoy=hoyNexo()
                if(RevisionesNexo.selectAll().where { (RevisionesNexo.usuario eq usuario) and (RevisionesNexo.dia eq hoy) }.empty()) RevisionesNexo.insert { it[RevisionesNexo.usuario]=usuario; it[dia]=hoy }
            }
            "tema", "accesorio" -> {
                val dias=RevisionesNexo.selectAll().where { RevisionesNexo.usuario eq usuario }.count()
                require(dias>=requisitoPremio(accion.opcion,accion.accion=="tema")!!) { "Este premio todavía no está desbloqueado." }
                EstilosNexo.update({ EstilosNexo.usuario eq usuario }) { if(accion.accion=="tema") it[tema]=accion.opcion else it[accesorio]=accion.opcion }
            }
            else -> {
                val m=metas(usuario).find { it.id==accion.meta } ?: throw ConflictoNexo("La meta no está disponible en tu cuenta.")
                if(m.version!=accion.version) throw ConflictoNexo("La meta cambió. Actualiza para ver su último progreso.")
                when(accion.accion) {
                    "editar" -> {
                        require(importeMeta(accion.importe)>=m.ahorrado.toBigDecimal()) { "El objetivo no puede ser menor al ahorro registrado." }
                        MetasAhorro.update({ MetasAhorro.idMeta eq m.id }) { it[nombreMeta]=accion.nombre.trim(); it[montoObjetivo]=importeMeta(accion.importe); it[fechaLimite]=LocalDate.parse(accion.fecha) }
                    }
                    "archivar", "restaurar" -> MetasAhorro.update({ MetasAhorro.idMeta eq m.id }) { it[activa]=accion.accion=="restaurar" }
                    "aportar", "retirar" -> {
                        require(m.activa) { "Restaura la meta antes de registrar cambios en su ahorro." }
                        val importe=importeMeta(accion.importe)*(if(accion.accion=="retirar") BigDecimal(-1) else BigDecimal.ONE)
                        val nuevo=m.ahorrado.toBigDecimal()+importe
                        require(nuevo>=BigDecimal.ZERO) { "No puedes retirar más de lo registrado en esta meta." }
                        require(importe<BigDecimal.ZERO || nuevo<=m.objetivo.toBigDecimal()) { "La aportación supera lo que falta para completar la meta." }
                        if(importe<BigDecimal.ZERO) RetirosNexo.insert { it[meta]=m.id; it[RetirosNexo.usuario]=usuario; it[RetirosNexo.importe]=importe.abs(); it[fecha]=hoyNexo() }
                        else ProgresoMetas.insert { it[idMeta]=m.id; it[idUsuario]=usuario; it[montoAhorrado]=importe; it[fechaRegistro]=LocalDateTime.now(ZoneId.of("America/Monterrey")) }
                    }
                }
                if(VersionesMeta.selectAll().where { VersionesMeta.meta eq m.id }.empty()) VersionesMeta.insert { it[meta]=m.id; it[version]=1 }
                else VersionesMeta.update({ VersionesMeta.meta eq m.id }) { it[version]=m.version+1 }
            }
        }
        SolicitudesNexo.insert { it[id]=accion.solicitud; it[SolicitudesNexo.usuario]=usuario; it[SolicitudesNexo.huella]=huella }
        leer(usuario)
    }
}
fun crearTablasBienestar()=transaction(DatabaseFactory.database) { SchemaUtils.create(MetasAhorro,ProgresoMetas,RetirosNexo,EstilosNexo,RevisionesNexo,VersionesMeta,SolicitudesNexo) }
fun Application.configureBienestar(almacen:AlmacenBienestar=BienestarSql()) {
    routing { route("/bienestar") {
        post("/salir") { SesionesNexo.revocar(call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?:""); call.respond(HttpStatusCode.OK,MensajeNexo("Sesión cerrada.")) }

        get {
            val usuario=SesionesNexo.usuario(call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?:"")
            if(usuario==null) { call.respond(HttpStatusCode.Unauthorized,MensajeNexo("Inicia sesión de nuevo para sincronizar tu cuenta.")); return@get }
            try { call.respond(almacen.estado(usuario)) } catch(e:Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos cargar tu cuenta. Reintenta en un momento.")) }
        }
        post {
            val usuario=SesionesNexo.usuario(call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?:"")
            if(usuario==null) { call.respond(HttpStatusCode.Unauthorized,MensajeNexo("Inicia sesión de nuevo para guardar tus cambios.")); return@post }
            try {
                val a=call.receive<AccionNexo>()
                call.respond(almacen.aplicar(usuario,a))
            } catch(e:ConflictoNexo) { call.respond(HttpStatusCode.Conflict,MensajeNexo(e.message?:"Actualiza los datos.")) }
            catch(e:io.ktor.server.plugins.BadRequestException) { call.respond(HttpStatusCode.BadRequest,MensajeNexo("Revisa el formato de los datos.")) }
            catch(e:IllegalArgumentException) { call.respond(HttpStatusCode.BadRequest,MensajeNexo(e.message?:"Revisa los datos.")) }
            catch(e:Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos confirmar el cambio. Reintenta la misma solicitud; no se duplicará.")) }
        }
    } }
}
