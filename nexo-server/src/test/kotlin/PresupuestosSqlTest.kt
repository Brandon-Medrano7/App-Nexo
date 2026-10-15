package com.example

import db.DatabaseFactory
import db.Usuarios
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.*

class PresupuestosSqlTest {
    @Test fun persistenciaReintentosYConflictosEnSqlServer() = runBlocking {
        val url = System.getenv("NEXO_SQL_TEST_URL") ?: return@runBlocking
        require(url.contains("databaseName=NexoSyncTest_"))
        DatabaseFactory.database = Database.connect(url, driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver", user = "sa", password = System.getenv("NEXO_SQL_TEST_PASSWORD"))
        transaction(DatabaseFactory.database) { SchemaUtils.create(Usuarios, PresupuestosPago) }
        fun usuario(correoPrueba: String) = transaction(DatabaseFactory.database) {
            Usuarios.insert {
                it[nombre] = "Prueba automática"; it[edad] = 20; it[correo] = correoPrueba
                it[password] = "solo-fixture"; it[puntos] = 0; it[fechaRegistro] = LocalDateTime.now()
            }[Usuarios.id]
        }
        val a = usuario("a@example.test"); val b = usuario("b@example.test")
        val almacen = AlmacenPresupuestosSql()
        val hoy = LocalDate.now(java.time.ZoneId.of("America/Monterrey"))
        val p = PresupuestoDto(UUID.randomUUID().toString(), "1000.00", hoy.toString(), hoy.plusDays(14).toString())
        val guardado = almacen.guardar(a, p)
        assertEquals(1, guardado.version)
        assertEquals(guardado, almacen.guardar(a, p))
        assertEquals(1, almacen.listar(a).size); assertTrue(almacen.listar(b).isEmpty())
        assertFailsWith<ConflictoPresupuesto> { almacen.guardar(b, p) }
        assertFailsWith<ConflictoPresupuesto> { almacen.guardar(a, p.copy(id = UUID.randomUUID().toString())) }
        val editado = almacen.guardar(a, guardado.copy(importe = "900.00"))
        assertEquals(2, editado.version)
        assertFailsWith<ConflictoPresupuesto> { almacen.guardar(a, guardado.copy(importe = "800.00")) }
        val historico = p.copy(id = UUID.randomUUID().toString(), inicio = hoy.minusDays(14).toString(), pago = hoy.toString())
        val pasado = almacen.guardar(a, historico)
        assertFailsWith<ConflictoPresupuesto> { almacen.guardar(a, pasado.copy(importe = "200.00")) }
        assertEquals(2, AlmacenPresupuestosSql().listar(a).size)
        // Dos peticiones a la vez para el mismo periodo: una escritura, una respuesta idempotente.
        val otro = p.copy(id = UUID.randomUUID().toString())
        val respuestas = awaitAll(async(Dispatchers.IO) { almacen.guardar(b, otro) }, async(Dispatchers.IO) { almacen.guardar(b, otro) })
        assertEquals(respuestas[0], respuestas[1]); assertEquals(1, almacen.listar(b).size)
    }
}
