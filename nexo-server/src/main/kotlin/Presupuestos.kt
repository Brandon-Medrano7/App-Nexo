package com.example

import db.DatabaseFactory
import db.Usuarios
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.security.SecureRandom
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Sesiones de desarrollo: expiran en 12 h y se invalidan al reiniciar el servidor.
object SesionesNexo {
    private data class Sesion(val usuario: Int, val vence: Instant)
    private val sesiones = ConcurrentHashMap<String, Sesion>()
    private fun hash(token: String) = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    fun crear(usuario: Int): String {
        val ahora = Instant.now()
        sesiones.entries.removeIf { it.value.vence <= ahora }
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        sesiones[hash(token)] = Sesion(usuario, ahora.plusSeconds(43200))
        return token
    }
    fun revocar(token:String) { sesiones.remove(hash(token)) }
    fun usuario(token: String): Int? {
        val sesion = sesiones[hash(token)] ?: return null
        return sesion.usuario.takeIf { sesion.vence > Instant.now() }
    }
}

@Serializable data class PresupuestoDto(val id: String, val importe: String, val inicio: String, val pago: String, val version: Int = 0)
@Serializable data class ErrorPresupuesto(val mensaje: String)
class ConflictoPresupuesto(message: String): IllegalStateException(message)
fun validarPresupuesto(p: PresupuestoDto, hoy: LocalDate = LocalDate.now(ZoneId.of("America/Monterrey"))) {
    require(runCatching { UUID.fromString(p.id).toString() == p.id }.getOrDefault(false)) { "Identificador de periodo inválido." }
    require(p.importe.matches(Regex("[0-9]{1,9}(\\.[0-9]{1,2})?"))) { "Usa un importe positivo con máximo dos decimales." }
    require(BigDecimal(p.importe) > BigDecimal.ZERO) { "El presupuesto debe ser mayor que cero." }
    val inicio = runCatching { LocalDate.parse(p.inicio) }.getOrNull()
    val pago = runCatching { LocalDate.parse(p.pago) }.getOrNull()
    require(inicio != null && pago != null && pago > inicio && inicio <= hoy) { "Revisa las fechas del periodo." }
    require(p.version >= 0) { "Versión inválida." }
}
fun seCruzan(a: PresupuestoDto, b: PresupuestoDto) = a.inicio < b.pago && b.inicio < a.pago

object PresupuestosPago : Table("PresupuestosPago") {
    val id = varchar("IdPresupuesto", 36)
    val usuario = integer("IdUsuario").references(Usuarios.id)
    val importe = decimal("Importe", 11, 2)
    val inicio = date("Inicio")
    val pago = date("ProximoPago")
    val version = integer("Version")
    override val primaryKey = PrimaryKey(id)
    init { index(false, usuario, inicio) }
}
interface AlmacenPresupuestos {
    suspend fun listar(usuario: Int): List<PresupuestoDto>
    suspend fun guardar(usuario: Int, presupuesto: PresupuestoDto): PresupuestoDto
}
class AlmacenPresupuestosSql: AlmacenPresupuestos {
    private fun ResultRow.dto() = PresupuestoDto(this[PresupuestosPago.id], this[PresupuestosPago.importe].toPlainString(), this[PresupuestosPago.inicio].toString(), this[PresupuestosPago.pago].toString(), this[PresupuestosPago.version])
    override suspend fun listar(usuario: Int) = DatabaseFactory.dbQuery {
        PresupuestosPago.selectAll().where { PresupuestosPago.usuario eq usuario }.orderBy(PresupuestosPago.inicio to SortOrder.DESC).map { it.dto() }
    }
    override suspend fun guardar(usuario: Int, presupuesto: PresupuestoDto): PresupuestoDto = DatabaseFactory.dbQuery {
        // Serializa las escrituras de una cuenta, incluso desde dos procesos del servidor.
        org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec("SELECT IdUsuario FROM Usuarios WITH (UPDLOCK, HOLDLOCK) WHERE IdUsuario = $usuario") { rs ->
            require(rs.next()) { "La cuenta ya no está disponible." }
        }
        val anteriores = PresupuestosPago.selectAll().where { PresupuestosPago.usuario eq usuario }.map { it.dto() }
        val anterior = anteriores.find { it.id == presupuesto.id }
        val mismoContenido = anterior != null && BigDecimal(anterior.importe).compareTo(BigDecimal(presupuesto.importe)) == 0 && anterior.inicio == presupuesto.inicio && anterior.pago == presupuesto.pago
        // Repetir una petición confirmada no crea un segundo periodo.
        if (mismoContenido && (anterior!!.version == presupuesto.version || anterior.version == presupuesto.version + 1)) return@dbQuery anterior
        if (anterior == null && presupuesto.version != 0 || anterior != null && anterior.version != presupuesto.version)
            throw ConflictoPresupuesto("El periodo cambió en otro dispositivo. Actualiza antes de editarlo.")
        if (anterior != null && LocalDate.parse(anterior.pago) <= LocalDate.now(ZoneId.of("America/Monterrey")))
            throw ConflictoPresupuesto("Este periodo terminó y se conserva en el historial. Crea uno nuevo.")
        if (anteriores.any { it.id != presupuesto.id && seCruzan(it, presupuesto) })
            throw ConflictoPresupuesto("Las fechas se cruzan con otro periodo guardado.")
        val nuevo = presupuesto.copy(importe = BigDecimal(presupuesto.importe).setScale(2).toPlainString(), version = presupuesto.version + 1)
        if (anterior == null) {
            if (PresupuestosPago.selectAll().where { PresupuestosPago.id eq presupuesto.id }.any()) throw ConflictoPresupuesto("Identificador de periodo no disponible.")
            PresupuestosPago.insert {
                it[id] = nuevo.id; it[PresupuestosPago.usuario] = usuario; it[importe] = BigDecimal(nuevo.importe)
                it[inicio] = LocalDate.parse(nuevo.inicio); it[pago] = LocalDate.parse(nuevo.pago); it[version] = nuevo.version
            }
        } else PresupuestosPago.update({ (PresupuestosPago.id eq nuevo.id) and (PresupuestosPago.usuario eq usuario) }) {
            it[importe] = BigDecimal(nuevo.importe); it[inicio] = LocalDate.parse(nuevo.inicio); it[pago] = LocalDate.parse(nuevo.pago); it[version] = nuevo.version
        }
        nuevo
    }
}
fun Application.configurePresupuestos(almacen: AlmacenPresupuestos = AlmacenPresupuestosSql()) {
    routing {
        route("/presupuestos") {
            get {
                val usuario = SesionesNexo.usuario(call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: "")
                if (usuario == null) { call.respond(HttpStatusCode.Unauthorized, ErrorPresupuesto("Inicia sesión de nuevo para sincronizar tu presupuesto.")); return@get }
                try { call.respond(almacen.listar(usuario)) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.ServiceUnavailable, ErrorPresupuesto("No se pudo consultar el presupuesto. Intenta más tarde.")) }
            }
            put {
                val usuario = SesionesNexo.usuario(call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: "")
                if (usuario == null) { call.respond(HttpStatusCode.Unauthorized, ErrorPresupuesto("Inicia sesión de nuevo para sincronizar tu presupuesto.")); return@put }
                val datos = try { call.receive<PresupuestoDto>().also { validarPresupuesto(it) } }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.BadRequest, ErrorPresupuesto("Revisa importe, fechas e identificador del periodo.")); return@put }
                try { call.respond(almacen.guardar(usuario, datos)) }
                catch (e: ConflictoPresupuesto) { call.respond(HttpStatusCode.Conflict, ErrorPresupuesto(e.message!!)) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.ServiceUnavailable, ErrorPresupuesto("No pudimos confirmar el guardado. Actualiza antes de intentarlo otra vez.")) }
            }
        }
    }
}
fun crearTablaPresupuestos() { transaction(DatabaseFactory.database) { SchemaUtils.create(PresupuestosPago) } }
