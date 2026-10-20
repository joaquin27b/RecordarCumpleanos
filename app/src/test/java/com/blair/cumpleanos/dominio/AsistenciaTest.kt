package com.blair.cumpleanos.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsistenciaTest {

    private val hoy = 20_000

    @Test fun `el dia del festejo no se pregunta`() = assertFalse(preguntaAsistencia(hoy, null, hoy))
    @Test fun `al dia siguiente si`() = assertTrue(preguntaAsistencia(hoy - 1, null, hoy))
    @Test fun `a los 30 dias todavia`() = assertTrue(preguntaAsistencia(hoy - 30, null, hoy))
    @Test fun `a los 31 ya no importa`() = assertFalse(preguntaAsistencia(hoy - 31, null, hoy))
    @Test fun `festejo futuro no`() = assertFalse(preguntaAsistencia(hoy + 2, null, hoy))
    @Test fun `sin festejo no`() = assertFalse(preguntaAsistencia(null, null, hoy))
    @Test fun `respondido si o no, no`() {
        assertFalse(preguntaAsistencia(hoy - 1, true, hoy))
        assertFalse(preguntaAsistencia(hoy - 1, false, hoy))
    }

    @Test fun `la notificacion sale si nunca se pregunto`() = assertTrue(notificaAsistencia(hoy - 1, null, null, hoy))
    @Test fun `no se repregunta el mismo dia`() = assertFalse(notificaAsistencia(hoy - 1, null, hoy, hoy))
    @Test fun `preguntado e ignorado no insiste otro dia`() = assertFalse(notificaAsistencia(hoy - 5, null, hoy - 4, hoy))

    private data class P(val id: Long)
    private val pendientes = listOf(P(3), P(2), P(1))

    @Test fun `una sola, la mas reciente`() = assertEquals(P(3), elegirAsistencia(pendientes, P::id, null))
    @Test fun `la que pidio la notificacion`() = assertEquals(P(1), elegirAsistencia(pendientes, P::id, 1))
    @Test fun `pedida ya respondida cae a la mas reciente`() = assertEquals(P(3), elegirAsistencia(pendientes, P::id, 9))
    @Test fun `ninguna`() = assertNull(elegirAsistencia(emptyList<P>(), P::id, null))
}
