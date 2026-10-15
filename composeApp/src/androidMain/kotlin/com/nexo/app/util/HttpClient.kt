package com.nexo.app.util

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val client =  HttpClient {
    install(ContentNegotiation){
        json(Json{
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true

        })
    }

}