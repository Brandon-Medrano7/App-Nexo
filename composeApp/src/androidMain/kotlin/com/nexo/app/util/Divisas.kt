package com.nexo.app.util

import java.math.BigDecimal
import java.math.RoundingMode

fun convertirAMxn(importe:String,tasa:String):String? {
    val limpio=importe.trim().replace(',','.')
    if(!Regex("[0-9]{1,9}(\\.[0-9]{1,2})?").matches(limpio))return null
    val cantidad=limpio.toBigDecimalOrNull()?:return null
    val tipo=tasa.toBigDecimalOrNull()?:return null
    if(cantidad<=BigDecimal.ZERO || tipo<=BigDecimal.ZERO || tipo>=BigDecimal("1000000"))return null
    return (cantidad*tipo).setScale(2,RoundingMode.HALF_UP).toPlainString()
}
