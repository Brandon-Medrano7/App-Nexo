package com.nexo.app.util

import java.io.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException

/** No confunde un contrato JSON incompatible con una desconexión. */
fun mensajeErrorConsulta(error: Throwable): String {
    val causas = generateSequence(error) { it.cause }.take(12).toList()
    return when {
        causas.any { it is TimeoutCancellationException || it is java.net.SocketTimeoutException } ->
            "La consulta tardó demasiado. Vuelve a intentar."
        causas.any { it is SerializationException } ->
            "La respuesta no se pudo leer. Actualiza la app o vuelve a intentar."
        causas.any { it is IOException } ->
            "No pudimos conectar con NEXO. Revisa tu conexión y que el servidor esté disponible."
        else -> "No pudimos completar la consulta. Vuelve a intentar."
    }
}
