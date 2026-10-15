package com.nexo.app.util
import com.nexo.app.ui.viewModel.CompromisoRemoto
import java.math.BigDecimal
import java.time.LocalDate
import java.math.RoundingMode

data class DisponiblePlanificado(val reservado:BigDecimal,val libre:BigDecimal,val diario:BigDecimal)
fun disponiblePlanificado(disponible:BigDecimal,dias:Long,pago:LocalDate,compromisos:List<CompromisoRemoto>):DisponiblePlanificado {
    val reservado=compromisos.filter{it.estado=="PENDIENTE" && LocalDate.parse(it.fecha)<pago}.fold(BigDecimal.ZERO){t,c->t+c.importe.toBigDecimal()}
    val libre=disponible-reservado
    return DisponiblePlanificado(reservado,libre,if(dias>0)libre.max(BigDecimal.ZERO).divide(BigDecimal(dias),2,RoundingMode.DOWN)else BigDecimal.ZERO)
}
