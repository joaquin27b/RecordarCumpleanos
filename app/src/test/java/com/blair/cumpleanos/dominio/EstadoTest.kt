package com.blair.cumpleanos.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

class EstadoTest {

    private val hoy = 20_000

    @Test fun `hoy`() = assertEquals(Estado.HOY, estado(hoy, hoy))

    @Test fun `manana es proximo`() = assertEquals(Estado.PROXIMO, estado(hoy + 1, hoy))

    @Test fun `ayer es pasado`() = assertEquals(Estado.PASADO, estado(hoy - 1, hoy))

    @Test fun `el dia 30 sigue siendo pasado`() =
        assertEquals(Estado.PASADO, estado(hoy - UMBRAL_OLVIDADO, hoy))

    @Test fun `el dia 31 ya es olvidado`() =
        assertEquals(Estado.OLVIDADO, estado(hoy - UMBRAL_OLVIDADO - 1, hoy))
}
