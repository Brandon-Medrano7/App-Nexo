package com.nexo.app.util
import kotlin.test.*
class DivisasTest {
 @Test fun convierteYRedondea(){assertEquals("231.25",convertirAMxn("12,50","18.5"));assertEquals("0.02",convertirAMxn("0.01","1.5"))}
 @Test fun rechazaInvalidos(){listOf("0","-1","NaN","1e3","1.234","1,000.00","","1000000000").forEach{assertNull(convertirAMxn(it,"18.5"))};assertNull(convertirAMxn("1","-2"))}
}
