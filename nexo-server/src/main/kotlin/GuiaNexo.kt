@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Serializable data class SolicitudGuia(val tema:String,val consentimientoExterno:Boolean=false)
@Serializable data class RespuestaGuia(val titulo:String,val texto:String,@kotlinx.serialization.EncodeDefault val fuente:String="Resumen local",val fecha:String)
// Solo cifras agregadas. Nunca incluye correo, nombre, contraseña, token ni conceptos de movimientos.
data class ContextoGuia(val fecha:String,val saldo:BigDecimal,val gastosMes:BigDecimal,val metasActivas:Int,val metasCompletas:Int,val faltanteMetas:BigDecimal,val diasRevision:Int,val presupuesto:BigDecimal?=null,val gastosPeriodo:BigDecimal=BigDecimal.ZERO,val diasPago:Long?=null,val comprometido:BigDecimal=BigDecimal.ZERO)
interface MotorGuia {
    val externo:Boolean
    suspend fun responder(tema:String,contexto:ContextoGuia):RespuestaGuia
}
interface FuenteGuia {suspend fun contexto(usuario:Int):ContextoGuia}
class FuenteGuiaSql:FuenteGuia {
    override suspend fun contexto(usuario:Int):ContextoGuia {
        val estado=BienestarSql().estado(usuario);val hoy=hoyNexo();val activas=estado.metas.filter{it.activa}
        val periodo=AlmacenPresupuestosSql().listar(usuario).firstOrNull{LocalDate.parse(it.inicio)<=hoy && LocalDate.parse(it.pago)>hoy}
        val gastos=if(periodo==null)BigDecimal.ZERO else db.DatabaseFactory.obtenerMovimientosPorUsuario(usuario).filter{it.fecha>=periodo.inicio && it.fecha<=hoy.toString() && it.monto<0}.fold(BigDecimal.ZERO){t,m->t-BigDecimal.valueOf(m.monto)}
        return ContextoGuia(hoy.toString(),estado.habitos.saldo.toBigDecimal(),estado.habitos.gastosMes.toBigDecimal(),activas.size,activas.count{it.ahorrado.toBigDecimal()>=it.objetivo.toBigDecimal()},activas.fold(BigDecimal.ZERO){t,m->t+(m.objetivo.toBigDecimal()-m.ahorrado.toBigDecimal()).max(BigDecimal.ZERO)},estado.habitos.dias,periodo?.importe?.toBigDecimal(),gastos,periodo?.let{ChronoUnit.DAYS.between(hoy,LocalDate.parse(it.pago))},periodo?.let{reservadoCompromisos(CompromisosSql().listar(usuario),LocalDate.parse(it.pago))}?:BigDecimal.ZERO)
    }
}
class MotorGuiaLocal:MotorGuia {
    override val externo=false
    override suspend fun responder(tema:String,contexto:ContextoGuia):RespuestaGuia {
        fun dinero(v:BigDecimal)=java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-MX")).format(v)
        val c=contexto
        val (titulo,texto)=when(tema) {
            "gastos"->"Así van tus registros" to "Tu saldo registrado es ${dinero(c.saldo)} y tus gastos de este mes hasta hoy suman ${dinero(c.gastosMes)}. Consulta Movimientos para revisar categorías y detectar si falta algo. Estas cifras no son una consulta a tu banco."
            "metas"->"Un paso hacia tus metas" to if(c.metasActivas==0)"Todavía no tienes metas activas. Empieza con una cantidad y una fecha que puedas sostener; después registra solo el dinero que ya hayas separado." else "Tienes ${c.metasActivas} metas activas, de las cuales ${c.metasCompletas} están completas. Faltan ${dinero(c.faltanteMetas)} para completar el conjunto. No hace falta aportar todos los días: ajusta tus objetivos a tus posibilidades."
            "habitos"->"Constancia sin presión" to "Has revisado ${c.diasRevision} días. Los estilos se desbloquean con 3 y 7 días acumulados; la corona, con 30. Un día sin gastos también cuenta y descansar no borra tus premios. Puedes confirmar tu revisión desde Perfil."
            "presupuesto"-> {
                val presupuesto=c.presupuesto;val dias=c.diasPago
                "Hasta tu próxima paga" to if(presupuesto==null || dias==null || dias<=0) "Configura un periodo vigente en Inicio para calcular cuánto queda hasta tu próxima paga. El presupuesto usa tus gastos registrados; no consulta tu saldo bancario."
                else {
                    val restante=presupuesto-c.gastosPeriodo-c.comprometido
                    if(restante< BigDecimal.ZERO)"Tus gastos y pagos pendientes superan el presupuesto de este periodo por ${dinero(restante.abs())}. Quedan $dias días hasta la fecha de pago. Revisa los movimientos y ajusta tu plan si cambió tu situación."
                    else "Después de reservar ${dinero(c.comprometido)} para pagos pendientes, quedan ${dinero(restante)} del presupuesto y $dias días hasta la fecha de pago. Eso equivale a ${dinero(restante.divide(BigDecimal(dias),2,RoundingMode.DOWN))} al día como referencia, sin considerar gastos que todavía no hayas registrado."
                }
            }
            else->throw IllegalArgumentException("Elige una de las consultas disponibles.")
        }
        return RespuestaGuia(titulo,texto,fecha=c.fecha)
    }
}
fun Application.configureGuia(fuente:FuenteGuia=FuenteGuiaSql(),motor:MotorGuia=MotorGuiaLocal(),externoHabilitado:Boolean=false) {
    routing {post("/asistente/consulta") {
        val usuario=call.usuarioNexo()?:return@post
        try {
            val solicitud=call.receive<SolicitudGuia>()
            require(solicitud.tema in setOf("gastos","metas","habitos","presupuesto")){"Elige una consulta disponible."}
            if(motor.externo && (!externoHabilitado || !solicitud.consentimientoExterno)){call.respond(HttpStatusCode.Forbidden,MensajeNexo("La IA externa no está habilitada con tu autorización."));return@post}
            val contexto=fuente.contexto(usuario)
            val respuesta=kotlinx.coroutines.withTimeout(12000){motor.responder(solicitud.tema,contexto)}
            call.respond(respuesta)
        }catch(e:IllegalArgumentException){call.respond(HttpStatusCode.BadRequest,MensajeNexo(e.message?:"Revisa tu consulta."))}
        catch(e:io.ktor.server.plugins.BadRequestException){call.respond(HttpStatusCode.BadRequest,MensajeNexo("Revisa la consulta."))}
        catch(e:Exception){if(e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException)throw e;call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos preparar el resumen. Intenta de nuevo."))}
    }}
}
