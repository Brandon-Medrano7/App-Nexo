package com.example

import com.example.com.nexo.app.model.Usuario
import com.example.com.nexo.app.model.UsuarioLogin
import com.example.com.nexo.app.model.UsuarioResponse
import com.example.com.nexo.app.model.Movimiento
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private class LimiteAcceso {
    private val intentos=mutableMapOf<String,Pair<Long,Int>>()
    @Synchronized fun permitir(ip:String):Boolean {
        val ahora=System.currentTimeMillis()
        intentos.entries.removeIf{ahora-it.value.first>=60000}
        val previo=intentos[ip]?: (ahora to 0)
        if(previo.second>=30 || intentos.size>10000) return false
        intentos[ip]=previo.first to previo.second+1
        return true
    }
}
suspend fun ApplicationCall.usuarioNexo():Int? {
    val id=SesionesNexo.usuario(request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?:"")
    if(id==null) respond(HttpStatusCode.Unauthorized,MensajeNexo("Tu sesión venció. Vuelve a iniciar sesión."))
    return id
}
fun Application.configureRouting(cuentas:CuentasNexo=CuentasSql(),movimientos:RegistroMovimientos=MovimientosSql()) {
    val limite=LimiteAcceso()
    routing {
        get("/"){call.respondText("Servidor NEXO disponible")}
        post("/register") {
            if(!limite.permitir(call.request.local.remoteHost)){call.respond(HttpStatusCode.TooManyRequests,MensajeNexo("Espera un minuto antes de volver a intentar."));return@post}
            try {val u=call.receive<Usuario>();validarCuenta(u);cuentas.registrar(u);call.respond(HttpStatusCode.Created,MensajeNexo("Cuenta creada. Ya puedes iniciar sesión."))}
            catch(e:CorreoOcupado){call.respond(HttpStatusCode.Conflict,MensajeNexo("Ese correo ya tiene una cuenta."))}
            catch(e:IllegalArgumentException){call.respond(HttpStatusCode.BadRequest,MensajeNexo(e.message?:"Revisa tus datos."))}
            catch(e:io.ktor.server.plugins.BadRequestException){call.respond(HttpStatusCode.BadRequest,MensajeNexo("Revisa los datos enviados."))}
            catch(e:Exception){if(e is kotlinx.coroutines.CancellationException) throw e;call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos confirmar el registro. Prueba iniciar sesión antes de repetirlo."))}
        }
        post("/login") {
            if(!limite.permitir(call.request.local.remoteHost)){call.respond(HttpStatusCode.TooManyRequests,MensajeNexo("Espera un minuto antes de volver a intentar."));return@post}
            try {
                val u=call.receive<UsuarioLogin>();val id=cuentas.autenticar(u.correo,u.password)
                if(id==null) call.respond(HttpStatusCode.Unauthorized,MensajeNexo("Correo o contraseña incorrectos."))
                else call.respond(UsuarioResponse(idUsuario=id,token=SesionesNexo.crear(id)))
            } catch(e:io.ktor.server.plugins.BadRequestException){call.respond(HttpStatusCode.BadRequest,MensajeNexo("Revisa tus datos."))}
            catch(e:Exception){if(e is kotlinx.coroutines.CancellationException) throw e;call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos iniciar sesión. Intenta más tarde."))}
        }
        get("/movimientos/usuario/{id}") {
            val id=call.usuarioNexo()?:return@get
            if(call.parameters["id"]?.toIntOrNull()!=id){call.respond(HttpStatusCode.Forbidden,MensajeNexo("Solo puedes consultar tu cuenta."));return@get}
            try {call.respond(movimientos.listar(id))}catch(e:Exception){if(e is kotlinx.coroutines.CancellationException) throw e;call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("No pudimos cargar tus movimientos."))}
        }
        post("/movimientos") {
            val id=call.usuarioNexo()?:return@post
            try {
                val m=call.receive<Movimiento>()
                if(m.idUsuario!=id){call.respond(HttpStatusCode.Forbidden,MensajeNexo("Solo puedes registrar en tu cuenta."));return@post}
                call.respond(HttpStatusCode.Created,movimientos.guardar(id,call.request.headers["Idempotency-Key"].orEmpty(),m))
            } catch(e:ConflictoNexo){call.respond(HttpStatusCode.Conflict,MensajeNexo(e.message!!))}
            catch(e:IllegalArgumentException){call.respond(HttpStatusCode.BadRequest,MensajeNexo(e.message?:"Revisa tu movimiento."))}
            catch(e:io.ktor.server.plugins.BadRequestException){call.respond(HttpStatusCode.BadRequest,MensajeNexo("Revisa el formato del movimiento."))}
            catch(e:Exception){if(e is kotlinx.coroutines.CancellationException) throw e;call.respond(HttpStatusCode.ServiceUnavailable,MensajeNexo("Guardado sin confirmar. Reintenta la misma solicitud."))}
        }
        // Las rutas antiguas se cierran para impedir acceso por un ID sin sesión.
        get("/perfil/usuario/{id}") {
            val id=call.usuarioNexo()?:return@get
            if(call.parameters["id"]?.toIntOrNull()!=id) call.respond(HttpStatusCode.Forbidden)
            else call.respond(HttpStatusCode.Gone,MensajeNexo("Usa la versión actualizada de Perfil."))
        }
        get("/metas/usuario/{id}") {
            val id=call.usuarioNexo()?:return@get
            if(call.parameters["id"]?.toIntOrNull()!=id) call.respond(HttpStatusCode.Forbidden)
            else call.respond(HttpStatusCode.Gone,MensajeNexo("Usa la versión actualizada de Metas."))
        }
    }
}
