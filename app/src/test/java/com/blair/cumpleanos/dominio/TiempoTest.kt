package com.blair.cumpleanos.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoTest {

    private val hoy = 20_000

    @Test fun `futuro hoy y pasado`() {
        assertEquals(Tiempo.FUTURO, tiempo(hoy + 3, hoy))
        assertEquals(Tiempo.HOY, tiempo(hoy, hoy))
        assertEquals(Tiempo.PASADO, tiempo(hoy - 3, hoy))
    }
}
