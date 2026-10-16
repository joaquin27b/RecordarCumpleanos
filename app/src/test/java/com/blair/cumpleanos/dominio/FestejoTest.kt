package com.blair.cumpleanos.dominio

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FestejoTest {

    private val hoy = 20_000
    private val cumple = hoy - 4

    @Test fun `avisa el dia anterior y el dia del festejo`() {
        assertTrue(avisaFestejo(hoy + 1, cumple, hoy, null))
        assertTrue(avisaFestejo(hoy, cumple, hoy, null))
    }

    @Test fun `no avisa antes del dia anterior`() =
        assertFalse(avisaFestejo(hoy + 2, cumple, hoy, null))

    @Test fun `un festejo que ya paso no avisa`() =
        assertFalse(avisaFestejo(hoy - 1, cumple, hoy, null))

    @Test fun `sin festejo no avisa`() =
        assertFalse(avisaFestejo(null, cumple, hoy, null))

    @Test fun `el mismo dia del cumple no duplica - gana el cumple`() {
        assertFalse(avisaFestejo(hoy, hoy, hoy, null))
        assertFalse(avisaFestejo(hoy + 1, hoy + 1, hoy, null))
    }

    @Test fun `una vez por dia`() {
        assertFalse(avisaFestejo(hoy, cumple, hoy, hoy))
        // El de ayer (dia anterior) no tapa el de hoy (dia del festejo).
        assertTrue(avisaFestejo(hoy, cumple, hoy, hoy - 1))
    }

    // Patry: hoy 14/10/2026, cumple del 5/10 ya saludado, ocurrencia abierta la
    // del 5/10/2027 con el festejo del 18/10/2026 colgado.
    private val hoyPatry = LocalDate.of(2026, 10, 14).toEpochDay().toInt()
    private val cumple2027 = LocalDate.of(2027, 10, 5).toEpochDay().toInt()
    private val festejo18 = LocalDate.of(2026, 10, 18).toEpochDay().toInt()

    @Test fun `Patry - manda el festejo del 18 y no el cumple de 2027`() {
        assertEquals(festejo18, fechaQueManda(cumple2027, festejo18, hoyPatry))
        assertTrue(mandaFestejo(cumple2027, festejo18, hoyPatry))
        assertEquals(2026, anioDe(fechaQueManda(cumple2027, festejo18, hoyPatry)))
    }

    @Test fun `Patry - el festejo avisa la vispera y el dia aunque el cumple este cerrado`() {
        assertTrue(avisaFestejo(festejo18, cumple2027, festejo18 - 1, null))
        assertTrue(avisaFestejo(festejo18, cumple2027, festejo18, festejo18 - 1))
    }

    @Test fun `un festejo que ya paso no manda`() {
        assertEquals(cumple2027, fechaQueManda(cumple2027, festejo18, festejo18 + 1))
        assertFalse(mandaFestejo(cumple2027, festejo18, festejo18 + 1))
    }

    @Test fun `sin festejo manda el cumple`() {
        assertEquals(cumple, fechaQueManda(cumple, null, cumple - 3))
        assertFalse(mandaFestejo(cumple, null, cumple - 3))
    }

    @Test fun `festejo despues del cumple pendiente - manda el cumple`() =
        assertFalse(mandaFestejo(hoy + 2, hoy + 5, hoy))

    @Test fun `festejo el mismo dia - manda el cumple`() {
        assertEquals(hoy + 2, fechaQueManda(hoy + 2, hoy + 2, hoy))
        assertFalse(mandaFestejo(hoy + 2, hoy + 2, hoy))
    }
}
