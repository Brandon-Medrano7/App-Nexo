package com.example

import com.fasterxml.jackson.databind.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*

fun Application.configureHTTP() {
    //install(HttpsRedirect) {
        // The port to redirect to. By default 443, the default HTTPS port.
        //sslPort = 443
        // 301 Moved Permanently, or 302 Found redirect.
        //permanentRedirect = true
    //}
}
