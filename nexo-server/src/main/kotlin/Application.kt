package com.example

import org.jetbrains.exposed.sql.Database

import io.ktor.server.application.*
import db.DatabaseFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    //Se inicializa la Base de Datos
    DatabaseFactory.init()

    configureHTTP()
    configureSerialization()
    //configureDatabases()
    configureSecurity()
    crearTablaRecibos()
    configureRouting()
    crearTablaPresupuestos()
    configurePresupuestos()
    crearTablasBienestar()
    configureBienestar()
    crearTablaCompromisos()
    configureCompromisos()
    configureGuia()
    configureDivisas()
}
