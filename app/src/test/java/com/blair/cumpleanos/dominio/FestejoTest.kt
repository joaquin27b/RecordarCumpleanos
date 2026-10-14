package com.blair.cumpleanos.dominio

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
}
