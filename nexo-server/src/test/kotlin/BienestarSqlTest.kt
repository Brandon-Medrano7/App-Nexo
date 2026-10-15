package com.example

import db.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.*
import java.util.UUID
import kotlin.test.*

class BienestarSqlTest {
    @Test fun metasPerfilPremiosYReintentos()=runBlocking {
        val url=System.getenv("NEXO_SQL_TEST_URL")
        org.junit.Assume.assumeTrue("Requiere SQL Server aislado",url!=null)
        require(url!!.contains("databaseName=NexoSyncTest_"))
        DatabaseFactory.database=Database.connect(url,driver="com.microsoft.sqlserver.jdbc.SQLServerDriver",user="sa",password=System.getenv("NEXO_SQL_TEST_PASSWORD"))
        transaction(DatabaseFactory.database){SchemaUtils.create(Usuarios,Movimientos)}
        crearTablasBienestar()
        fun user(email:String)=transaction(DatabaseFactory.database) {Usuarios.insert {it[nombre]="Prueba";it[edad]=20;it[correo]=email;it[password]="fixture";it[puntos]=0;it[fechaRegistro]=LocalDateTime.now()}[Usuarios.id]}
        val a=user("metas-a@example.test");val b=user("metas-b@example.test");val sql=BienestarSql()
        fun accion(tipo:String)=AccionNexo(UUID.randomUUID().toString(),tipo)
        val crear=accion("crear").copy(nombre="Laptop",importe="1000",fecha=hoyNexo().plusMonths(2).toString())
        val m=sql.aplicar(a,crear).metas.single()
        assertEquals(m,sql.aplicar(a,crear).metas.single());assertTrue(sql.estado(b).metas.isEmpty())
        assertFailsWith<ConflictoNexo>{sql.aplicar(a,crear.copy(importe="500"))}
        val aporte=accion("aportar").copy(meta=m.id,version=m.version,importe="300")
        assertFailsWith<ConflictoNexo>{sql.aplicar(b,aporte)}
        val resultados=awaitAll(async(Dispatchers.IO){sql.aplicar(a,aporte)},async(Dispatchers.IO){sql.aplicar(a,aporte)})
        assertEquals("300.00",resultados[0].metas.single().ahorrado);assertEquals(1,resultados[1].metas.single().historial.size)
        var actual=sql.estado(a).metas.single()
        assertFailsWith<ConflictoNexo>{sql.aplicar(a,aporte.copy(solicitud=UUID.randomUUID().toString()))}
        assertFailsWith<IllegalArgumentException>{sql.aplicar(a,accion("retirar").copy(meta=m.id,version=actual.version,importe="400"))}
        actual=sql.aplicar(a,accion("retirar").copy(meta=m.id,version=actual.version,importe="100")).metas.single()
        assertEquals("200.00",actual.ahorrado)
        actual=sql.aplicar(a,accion("archivar").copy(meta=m.id,version=actual.version)).metas.single();assertFalse(actual.activa)
        assertFailsWith<IllegalArgumentException>{sql.aplicar(a,accion("aportar").copy(meta=m.id,version=actual.version,importe="10"))}
        actual=sql.aplicar(a,accion("restaurar").copy(meta=m.id,version=actual.version)).metas.single();assertTrue(actual.activa)
        actual=sql.aplicar(a,accion("editar").copy(meta=m.id,version=actual.version,nombre="Laptop nueva",importe="900",fecha=hoyNexo().plusDays(50).toString())).metas.single()
        assertEquals("Laptop nueva",actual.nombre)
        actual=sql.aplicar(a,accion("aportar").copy(meta=m.id,version=actual.version,importe="700")).metas.single();assertEquals("900.00",actual.ahorrado)
        assertFailsWith<IllegalArgumentException>{sql.aplicar(a,accion("aportar").copy(meta=m.id,version=actual.version,importe="1"))}
        assertFailsWith<IllegalArgumentException>{sql.aplicar(a,accion("tema").copy(opcion="bosque"))}
        val revision=accion("revisar")
        awaitAll(async(Dispatchers.IO){sql.aplicar(a,revision)},async(Dispatchers.IO){sql.aplicar(a,accion("revisar"))})
        assertEquals(1,sql.estado(a).habitos.dias)
        transaction(DatabaseFactory.database){(1..6).forEach { n->RevisionesNexo.insert{it[usuario]=a;it[dia]=hoyNexo().minusDays(n.toLong())} }}
        val premio=sql.aplicar(a,accion("tema").copy(opcion="bosque"));assertEquals("bosque",premio.habitos.tema);assertEquals(7,premio.habitos.racha)
        assertEquals("lentes",sql.aplicar(a,accion("accesorio").copy(opcion="lentes")).habitos.accesorio)
        assertFailsWith<IllegalArgumentException>{sql.aplicar(a,accion("accesorio").copy(opcion="corona"))}
        val perfil=accion("perfil").copy(nombre="Nombre nuevo",edad=21,version=0)
        assertEquals("Nombre nuevo",sql.aplicar(a,perfil).perfil.nombre)
        assertFailsWith<ConflictoNexo>{sql.aplicar(a,perfil.copy(solicitud=UUID.randomUUID().toString(),nombre="Obsoleto"))}
        assertEquals("Nombre nuevo",BienestarSql().estado(a).perfil.nombre)
        assertEquals(3,BienestarSql().estado(a).metas.single().historial.size)
        assertEquals("clasico",sql.estado(b).habitos.tema)
    }
}
