package com.blair.cumpleanos.dominio

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun d(a: Int, m: Int, dd: Int) = LocalDate.of(a, m, dd).toEpochDay().toInt()

class EdadTest {

    @Test fun `sin anio de nacimiento no hay edad`() =
        assertNull(edad(d(2026, 12, 25), null))

    @Test fun `edad contra el anio de la ocurrencia`() =
        assertEquals(36, edad(d(2026, 12, 25), 1990))

    @Test fun `grupo proximo anio usa el anio de la ocurrencia`() =
        assertEquals(37, edad(d(2027, 3, 10), 1990))

    @Test fun `nacido un 29 de febrero cumple en la ocurrencia clampeada`() =
        assertEquals(35, edad(d(2027, 2, 28), 1992))

    @Test fun `recien nacido cumple cero`() =
        assertEquals(0, edad(d(2026, 12, 25), 2026))

    @Test fun `anio futuro no devuelve edad negativa`() =
        assertNull(edad(d(2026, 12, 25), 2030))
}
