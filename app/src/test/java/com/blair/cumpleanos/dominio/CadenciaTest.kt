package com.blair.cumpleanos.dominio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CadenciaTest {

    private val hoy = 20_000

    @Test fun `D-0 avisa siempre`() =
        assertTrue(Cadencia.avisa(hoy, hoy, null, ""))

    @Test fun `D-0 avisa aunque no haya preavisos configurados`() =
        assertTrue(Cadencia.avisa(hoy, hoy, null, "30,15,7,1"))

    @Test fun `no reavisa el mismo dia`() =
        assertFalse(Cadencia.avisa(hoy, hoy, hoy, null))

    @Test fun `pasado no reavisa`() =
        assertFalse(Cadencia.avisa(hoy - 1, hoy, null, "30,15,7,1"))

    @Test fun `preaviso del default`() =
        assertTrue(Cadencia.avisa(hoy + 7, hoy, null, null))

    @Test fun `dia que no es preaviso no avisa`() =
        assertFalse(Cadencia.avisa(hoy + 15, hoy, null, null))

    @Test fun `preaviso configurado explicitamente`() =
        assertTrue(Cadencia.avisa(hoy + 15, hoy, null, "30,15,7,1"))

    @Test fun `csv vacio deja solo D-0`() {
        assertArrayEquals(intArrayOf(), Cadencia.preavisosEfectivos(""))
        assertFalse(Cadencia.avisa(hoy + 7, hoy, null, ""))
    }

    /** CSV que dejaron en la base los testers de la v1: tiene que seguir parseando igual. */
    @Test fun `csv de la v1 sigue parseando igual`() {
        assertArrayEquals(intArrayOf(7, 1), Cadencia.preavisosEfectivos("7,1"))
        assertArrayEquals(intArrayOf(30, 15, 7, 1), Cadencia.preavisosEfectivos("30,15,7,1"))
        assertTrue(Cadencia.avisa(hoy + 7, hoy, null, "7,1"))
        assertTrue(Cadencia.avisa(hoy + 1, hoy, null, "7,1"))
        assertFalse(Cadencia.avisa(hoy + 3, hoy, null, "7,1"))
        assertFalse(Cadencia.avisa(hoy + 15, hoy, null, "7,1"))
    }

    @Test fun `null cae al default`() =
        assertArrayEquals(Cadencia.POR_DEFECTO, Cadencia.preavisosEfectivos(null))

    @Test fun `resumen una vez por mes`() {
        assertTrue(avisaResumen(null, 2026, 9, 1))
        assertFalse(avisaResumen(202609, 2026, 9, 14))
        assertTrue(avisaResumen(202609, 2026, 10, 1))
    }

    @Test fun `resumen atrasado se emite igual`() =
        assertTrue(avisaResumen(202608, 2026, 9, 4))
}
