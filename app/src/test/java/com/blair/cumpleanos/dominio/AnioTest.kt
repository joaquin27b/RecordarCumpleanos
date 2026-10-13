package com.blair.cumpleanos.dominio

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AnioTest {

    private val hoy = LocalDate.of(2026, 10, 13)

    @Test fun `vacio es desconocido, no error`() =
        assertEquals(AnioNacimiento.Desconocido, validarAnio("  ", 5, 3, hoy))

    @Test fun `anio normal`() =
        assertEquals(AnioNacimiento.Valido(1990), validarAnio("1990", 5, 3, hoy))

    @Test fun `anio futuro`() =
        assertEquals(AnioNacimiento.Futuro, validarAnio("2027", 5, 3, hoy))

    @Test fun `este anio con fecha que todavia no llego es futuro`() =
        assertEquals(AnioNacimiento.Futuro, validarAnio("2026", 20, 12, hoy))

    @Test fun `este anio con fecha de hoy es valido`() =
        assertEquals(AnioNacimiento.Valido(2026), validarAnio("2026", 13, 10, hoy))

    @Test fun `29 de febrero en anio no bisiesto`() =
        assertEquals(AnioNacimiento.SinBisiesto, validarAnio("1990", 29, 2, hoy))

    @Test fun `29 de febrero en bisiesto`() =
        assertEquals(AnioNacimiento.Valido(1992), validarAnio("1992", 29, 2, hoy))

    @Test fun `menos de 4 cifras o basura`() {
        assertEquals(AnioNacimiento.Invalido, validarAnio("90", 5, 3, hoy))
        assertEquals(AnioNacimiento.Invalido, validarAnio("19a0", 5, 3, hoy))
        assertEquals(AnioNacimiento.Invalido, validarAnio("1850", 5, 3, hoy))
    }
}
