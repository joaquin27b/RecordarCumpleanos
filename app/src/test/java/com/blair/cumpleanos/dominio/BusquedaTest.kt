package com.blair.cumpleanos.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

class BusquedaTest {

    private val nombres = listOf("Any", "Ány", "Ana", "Aníbal", "Ñandú")
    private val filas = nombres.map { ConClave(it, normalizar(it)) }

    private fun buscar(texto: String) = filtrarPorNombre(filas, normalizar(texto))

    @Test fun `a trae Any, Any con tilde, Ana y Anibal`() = assertEquals(listOf("Any", "Ány", "Ana", "Aníbal"), buscar("a"))
    @Test fun `an trae los cuatro que empiezan con an`() = assertEquals(listOf("Any", "Ány", "Ana", "Aníbal"), buscar("an"))
    @Test fun `any con tilde ignora la tilde`() = assertEquals(listOf("Any", "Ány"), buscar("ány"))
    @Test fun `mayusculas no importan`() = assertEquals(listOf("Any", "Ány"), buscar("ANY"))
    @Test fun `nandu encuentra la enie`() = assertEquals(listOf("Ñandú"), buscar("nandu"))
    @Test fun `enie encuentra la enie`() = assertEquals(listOf("Ñandú"), buscar("ñandu"))
    @Test fun `n encuentra la enie`() = assertEquals(listOf("Ñandú"), buscar("n"))
    @Test fun `zzz no trae nada`() = assertEquals(emptyList<String>(), buscar("zzz"))

    @Test fun `vacio normaliza a vacio`() {
        assertEquals("", normalizar(""))
        assertEquals("", normalizar("   "))
    }

    @Test fun `busca por inicio de cualquier palabra`() {
        val f = listOf(ConClave("Ana López", normalizar("Ana López")))
        assertEquals(listOf("Ana López"), filtrarPorNombre(f, normalizar("lop")))
        assertEquals(emptyList<String>(), filtrarPorNombre(f, normalizar("pez")))
    }

    @Test fun `la I turca no rompe la comparacion`() {
        val antes = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr"))
            assertEquals("ivan", normalizar("IVAN"))
        } finally {
            java.util.Locale.setDefault(antes)
        }
    }
}
