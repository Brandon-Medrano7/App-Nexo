package db


import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

//import org.jetbrains.exposed.sql.javatime.date

// Tabla Usuarios
object Usuarios : Table("Usuarios") {
    val id = integer("IdUsuario").autoIncrement()

    val nombre = varchar("Nombre", 100)
    val edad = integer("Edad")
    val correo = varchar("Correo", 100).uniqueIndex()
    val password = varchar("Contrasena", 100)
    val puntos = integer("Puntos")
    val fechaRegistro = datetime("FechaRegistro")
    //val edad1 = integer("Edad").nullable()

    // El error 'primaryKey overrides nothing' se quita usando override val
    override val primaryKey = PrimaryKey(id)
}

//Tabla Movimiento
object Movimientos : Table("Movimientos") {
    val id = integer("IdMovimiento").autoIncrement()
    val idUsuario = integer("IdUsuario") references Usuarios.id
    val idCategoria = integer("IdCategoria") // Ingreso o Gasto
    val monto = decimal("Monto", 18, 2)
    val tipo = varchar("Tipo", 30)
    val fecha = date("Fecha")
    val descripcion = varchar("Descripcion", 200)

    override val primaryKey = PrimaryKey(id)
}

object ProgresoMetas : Table("ProgresoMetas") {
    val idProgreso = integer("IdProgreso").autoIncrement()
    val idMeta = integer("IdMeta") references MetasAhorro.idMeta
    val idUsuario = integer("IdUsuario") references Usuarios.id // <--- AGREGA ESTO
    val montoAhorrado = decimal("MontoAhorrado", 10, 2)
    val fechaRegistro = datetime("FechaRegistro")

    override val primaryKey = PrimaryKey(idProgreso)
}
// Esta es la tabla principal de metas
object MetasAhorro : Table("MetasAhorro") {
    val idMeta = integer("IdMeta").autoIncrement()
    val idUsuario = integer("IdUsuario").references(Usuarios.id)
    val nombreMeta = varchar("NombreMeta", 100)
    val montoObjetivo = decimal("MontoObjetivo", 10, 2)
    val fechaCreacion = datetime("FechaCreacion")
    val fechaLimite = date("FechaLimite")
    val activa = bool("Activa")

    override val primaryKey = PrimaryKey(idMeta)
}