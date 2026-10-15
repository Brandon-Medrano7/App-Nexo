package com.example

import db.*
import com.example.com.nexo.app.model.Usuario
import com.example.com.nexo.app.model.Movimiento
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.*

class UxSqlTest {
    @Test fun cuentasYMovimientosPersistenSinDuplicar()=runBlocking {
        val url=System.getenv("NEXO_SQL_TEST_URL");org.junit.Assume.assumeTrue(url!=null);require(url!!.contains("databaseName=NexoSyncTest_"))
        DatabaseFactory.database=Database.connect(url,driver="com.microsoft.sqlserver.jdbc.SQLServerDriver",user="sa",password=System.getenv("NEXO_SQL_TEST_PASSWORD"))
        transaction(DatabaseFactory.database){SchemaUtils.create(Usuarios,Movimientos)}
        crearTablaRecibos()
        val cuentas=CuentasSql();val correo="ux@example.test";val clave="una-frase-de-prueba"
        cuentas.registrar(Usuario("Prueba UX",20,correo,clave))
        val id=cuentas.autenticar(correo,clave)!!
        assertNull(cuentas.autenticar(correo,"incorrecta"))
        assertFailsWith<CorreoOcupado>{cuentas.registrar(Usuario("Otra",21,correo,clave))}
        assertTrue(transaction(DatabaseFactory.database){ClavesNexo.esHash(Usuarios.selectAll().where{Usuarios.id eq id}.single()[Usuarios.password])})
        val anterior=transaction(DatabaseFactory.database){Usuarios.insert{it[nombre]="Anterior";it[edad]=20;it[Usuarios.correo]="legado@example.test";it[password]="ClaveAnterior";it[puntos]=0;it[fechaRegistro]=LocalDateTime.now()}[Usuarios.id]}
        assertEquals(anterior,cuentas.autenticar("legado@example.test","ClaveAnterior"))
        assertTrue(transaction(DatabaseFactory.database){ClavesNexo.esHash(Usuarios.selectAll().where{Usuarios.id eq anterior}.single()[Usuarios.password])})
        val m=Movimiento(0,id,2,-30.5,hoyNexo().toString(),"Comida","Gasto");val key=UUID.randomUUID().toString();val repo=MovimientosSql()
        val resultados=awaitAll(async(Dispatchers.IO){repo.guardar(id,key,m)},async(Dispatchers.IO){repo.guardar(id,key,m)})
        assertEquals(resultados[0].idMovimiento,resultados[1].idMovimiento);assertEquals(1,repo.listar(id).size)
        assertFailsWith<ConflictoNexo>{repo.guardar(id,key,m.copy(monto=-40.0))}
        assertFailsWith<IllegalArgumentException>{repo.guardar(anterior,UUID.randomUUID().toString(),m)}
        assertEquals(1,MovimientosSql().listar(id).size);assertEquals(0,repo.listar(anterior).size)
    }
}
