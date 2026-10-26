package com.blair.cumpleanos.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Secciones: pasados, este año, proximo año, personas, historial. La ultima es el
 * final de todo, donde va el grande. N personas = N cumpleaños + N filas de personas.
 */
class DensidadNativosTest {

    private fun lista(esteAnio: Int, proximoAnio: Int, historial: Int = 0, pasados: Int = 0) =
        listOf(pasados, esteAnio, proximoAnio, esteAnio + proximoAnio + pasados, minOf(historial, FILAS_MAX_HISTORIAL))

    @Test fun `sin filas no hay ninguno`() =
        assertEquals(emptyList<Int>(), ubicarNativos(listOf(0, 0, 0, 0, 0)))

    @Test fun `con 2 personas va uno solo, el grande del final`() =
        assertEquals(listOf(4), ubicarNativos(lista(2, 0)))

    @Test fun `con 2 personas y historial van dos separados`() =
        // 2 cumpleaños, nativo, 2 personas y 4 saludos: 6 filas entre los dos.
        assertEquals(listOf(1, 4), ubicarNativos(lista(2, 0, historial = 4)))

    @Test fun `con 5 filas sigue siendo uno al final`() =
        assertEquals(listOf(4), ubicarNativos(listOf(1, 2, 0, 2, 0)))

    @Test fun `con 5 personas sin historial el grande no entra y queda uno`() =
        assertEquals(listOf(1), ubicarNativos(lista(5, 0)))

    @Test fun `el grande le gana al de personas`() =
        // 10 personas: tras este año, tras proximo año, tras personas... y el final
        // a 0 filas de personas: el de personas cede su lugar.
        assertEquals(listOf(1, 2, 4), ubicarNativos(lista(4, 6)))

    @Test fun `25 personas`() =
        assertEquals(listOf(1, 2, 4), ubicarNativos(lista(8, 17, historial = 30)))

    @Test fun `dos secciones cortas seguidas comparten uno`() =
        // Despues de pasados (2); este año suma 2 y proximo 2: recien en personas llega a 6.
        assertEquals(listOf(0, 4), ubicarNativos(listOf(2, 2, 2, 4, 0)))

    @Test fun `nunca hay menos de 6 filas entre dos`() {
        val r = java.util.Random(7)
        repeat(5_000) {
            val filas = List(4) { r.nextInt(12) } + r.nextInt(FILAS_MAX_HISTORIAL + 1)
            val u = ubicarNativos(filas)
            if (filas.sum() == 0) assertTrue(u.isEmpty())
            if (filas.sum() in 1..POCAS_FILAS) assertEquals(listOf(4), u)
            u.zipWithNext().forEach { (a, b) ->
                assertTrue("$filas -> $u", filas.subList(a + 1, b + 1).sum() >= SEPARACION_NATIVOS)
            }
        }
    }
}
