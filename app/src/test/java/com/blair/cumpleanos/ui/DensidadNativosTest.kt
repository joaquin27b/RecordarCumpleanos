package com.blair.cumpleanos.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Secciones: pasados, este año, proximo año, personas, historial (siempre 0). */
class DensidadNativosTest {

    @Test fun `sin filas no hay ninguno`() =
        assertEquals(emptyList<Int>(), ubicarNativos(listOf(0, 0, 0, 0, 0)))

    @Test fun `con 3 personas va uno solo al final`() =
        assertEquals(listOf(4), ubicarNativos(listOf(0, 0, 0, 3, 0)))

    @Test fun `con 5 filas sigue siendo uno al final`() =
        assertEquals(listOf(4), ubicarNativos(listOf(1, 2, 0, 2, 0)))

    @Test fun `con 6 filas va despues de la primera seccion con datos`() =
        assertEquals(listOf(1), ubicarNativos(listOf(0, 3, 0, 3, 0)))

    @Test fun `dos secciones cortas seguidas comparten uno`() =
        // Despues de pasados (2); este año suma 2 y proximo 2: recien en personas llega a 6.
        assertEquals(listOf(0, 3), ubicarNativos(listOf(2, 2, 2, 4, 0)))

    @Test fun `12 personas con sus 12 cumpleaños`() =
        assertEquals(listOf(1, 3), ubicarNativos(listOf(0, 8, 4, 12, 0)))

    @Test fun `respeta el maximo de turnos`() =
        assertEquals(2, ubicarNativos(listOf(6, 6, 6, 6, 0), maximo = 2).size)

    @Test fun `nunca hay menos de 6 filas entre dos`() {
        val r = java.util.Random(7)
        repeat(2_000) {
            val filas = List(4) { r.nextInt(9) } + 0
            val u = ubicarNativos(filas)
            if (filas.sum() > POCAS_FILAS) {
                u.zipWithNext().forEach { (a, b) ->
                    assertTrue("$filas -> $u", filas.subList(a + 1, b + 1).sum() >= SEPARACION_NATIVOS)
                }
            } else {
                assertTrue(u.size <= 1)
            }
        }
    }
}
