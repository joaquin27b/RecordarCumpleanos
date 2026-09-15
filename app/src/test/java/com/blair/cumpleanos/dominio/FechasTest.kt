package com.blair.cumpleanos.dominio

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

private fun d(a: Int, m: Int, dd: Int) = LocalDate.of(a, m, dd).toEpochDay().toInt()

class FechasTest {

    @Test fun `anual preserva dia y mes`() =
        assertEquals(d(2027, 3, 15), proxima(d(2026, 3, 15), 15))

    @Test fun `29 de febrero se clampea a 28 en anio no bisiesto`() =
        assertEquals(d(2025, 2, 28), proxima(d(2024, 2, 29), 29))

    @Test fun `el ancla recupera el 29 en el proximo bisiesto`() =
        assertEquals(d(2028, 2, 29), proxima(d(2027, 2, 28), 29))

    @Test fun `sin ancla el 29 quedaria corrido para siempre`() {
        var f = d(2024, 2, 29)
        repeat(4) { f = proxima(f, 29) }
        assertEquals(d(2028, 2, 29), f)
    }

    @Test fun `ocurrencia futura del mismo anio`() =
        assertEquals(d(2026, 12, 25), proximaOcurrencia(25, 12, d(2026, 9, 14)))

    @Test fun `ocurrencia ya pasada salta al anio siguiente`() =
        assertEquals(d(2027, 3, 10), proximaOcurrencia(10, 3, d(2026, 9, 14)))

    @Test fun `el cumpleanos de hoy es la ocurrencia de hoy`() =
        assertEquals(d(2026, 9, 14), proximaOcurrencia(14, 9, d(2026, 9, 14)))

    @Test fun `alta de un 29 de febrero no explota en anio no bisiesto`() =
        assertEquals(d(2027, 2, 28), proximaOcurrencia(29, 2, d(2026, 9, 14)))

    @Test fun `el 31 no explota en un mes de 30`() =
        assertEquals(d(2026, 11, 30), proximaOcurrencia(31, 11, d(2026, 9, 14)))

    @Test(expected = IllegalArgumentException::class)
    fun `mes fuera de rango`() { proximaOcurrencia(1, 13, d(2026, 9, 14)) }
}
