package com.example

import db.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.*

class CompromisosSqlTest {
    @Test fun persisteSinDuplicarYAislaCuentas()=runBlocking {
        val url=System.getenv("NEXO_SQL_TEST_URL")
        org.junit.Assume.assumeTrue(url!=null)
        require(url!!.contains("databaseName=NexoSyncTest_"))
        DatabaseFactory.database=Database.connect(url,driver="com.microsoft.sqlserver.jdbc.SQLServerDriver",user="sa",password=System.getenv("NEXO_SQL_TEST_PASSWORD"))
        transaction(DatabaseFactory.database){SchemaUtils.create(Usuarios,Movimientos)}
        crearTablaCompromisos();crearTablaCompromisos()
        fun usuario()=transaction(DatabaseFactory.database){Usuarios.insert{it[nombre]="QA Compromisos";it[edad]=20;it[correo]="${UUID.randomUUID()}@example.test";it[password]="no-es-una-cuenta-real";it[puntos]=0;it[fechaRegistro]=LocalDateTime.now()}[Usuarios.id]}
        val uno=usuario();val dos=usuario();val repo=CompromisosSql()
        val borrador=CompromisoDto(UUID.randomUUID().toString(),"Internet","400.00",hoyNexo().toString())
        val resultados=awaitAll(async(Dispatchers.IO){repo.guardar(uno,borrador)},async(Dispatchers.IO){repo.guardar(uno,borrador)})
        assertEquals(resultados[0],resultados[1]);assertEquals(1,repo.listar(uno).size)
        val guardado=resultados.first()
        assertTrue(repo.listar(dos).isEmpty())
        assertFailsWith<ConflictoCompromiso>{repo.guardar(dos,guardado.copy(nombre="Invadir"))}
        assertFailsWith<ConflictoCompromiso>{repo.guardar(uno,borrador.copy(importe="500"))}
        fun gasto(user:Int,importe:String)=transaction(DatabaseFactory.database){Movimientos.insert{it[idUsuario]=user;it[idCategoria]=2;it[monto]=importe.toBigDecimal();it[tipo]="Gasto";it[fecha]=hoyNexo();it[descripcion]="Prueba pago"}[Movimientos.id]}
        val ajeno=gasto(dos,"-400");val incorrecto=gasto(uno,"-399");val correcto=gasto(uno,"-400")
        assertFailsWith<IllegalArgumentException>{repo.guardar(uno,guardado.copy(estado="PAGADO",movimiento=ajeno))}
        assertFailsWith<IllegalArgumentException>{repo.guardar(uno,guardado.copy(estado="PAGADO",movimiento=incorrecto))}
        val pagado=repo.guardar(uno,guardado.copy(estado="PAGADO",movimiento=correcto))
        assertEquals(pagado,repo.guardar(uno,guardado.copy(estado="PAGADO",movimiento=correcto)))
        assertEquals(0,reservadoCompromisos(repo.listar(uno),hoyNexo().plusDays(1)).signum())
        val otro=repo.guardar(uno,borrador.copy(id=UUID.randomUUID().toString()))
        assertFailsWith<ConflictoCompromiso>{repo.guardar(uno,otro.copy(estado="PAGADO",movimiento=correcto))}
        val cancelado=repo.guardar(uno,otro.copy(estado="CANCELADO"))
        assertEquals(0,reservadoCompromisos(repo.listar(uno),hoyNexo().plusDays(1)).signum())
        val restaurado=repo.guardar(uno,cancelado.copy(estado="PENDIENTE"))
        assertEquals(BigDecimal("400.00"),reservadoCompromisos(repo.listar(uno),hoyNexo().plusDays(1)))
        val desvinculado=repo.guardar(uno,pagado.copy(estado="PENDIENTE",movimiento=null))
        assertEquals(BigDecimal("800.00"),reservadoCompromisos(repo.listar(uno),hoyNexo().plusDays(1)))
        assertEquals(restaurado,CompromisosSql().listar(uno).first{it.id==restaurado.id})
        assertEquals(2,transaction(DatabaseFactory.database){Movimientos.selectAll().where{Movimientos.idUsuario eq uno}.count()}.toInt())
        assertNull(desvinculado.movimiento)
    }
}
