package db

//Conexion a SQL Server (JDBC)

import com.example.com.nexo.app.model.Movimiento
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    lateinit var database: Database //Linea agregada
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(
            kotlinx.coroutines.Dispatchers.IO,
            db = database
        ) {block()}

    suspend fun obtenerMovimientosPorUsuario(userId: Int): List<Movimiento> = dbQuery{
        Movimientos
            .selectAll()
            .where { Movimientos.idUsuario eq userId }
            .map { row ->
                Movimiento(
                    idMovimiento = row[Movimientos.id],
                    idUsuario = row[Movimientos.idUsuario],
                    idCategoria = row[Movimientos.idCategoria],
                    monto = row[Movimientos.monto].toDouble(), // Convertimos BigDecimal a Double para la API
                    tipo = row[Movimientos.tipo],
                    fecha = row[Movimientos.fecha].toString(), // Convertimos la fecha a String
                    descripcion = row[Movimientos.descripcion]
                )
            }
    }
    fun init() {
        try {
            database = Database.connect(createHikariDataSource())
            // Crea las tablas en la BD si aún no existen
            transaction(database) {
                println("DEBUG: Conectado a: ${connection.metadata { databaseProductVersion }}")
                SchemaUtils.create(Usuarios, Movimientos)
            }
        }catch (e: Exception){
            println("ERROR DE CONEXIÓN: No se pudo conectar a la base de datos...")
            println("Detalle:  ${e.message}")
        }
    }
    private fun createHikariDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            // Reemplaza con tus datos reales de DBeaver
            jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=Nexo_Pruebas;encrypt=false;trustServerCertificate=true"
            username = "sa"
            password = "BrandonHD564!"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}