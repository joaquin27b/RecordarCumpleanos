package com.blair.cumpleanos.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val HOY = 20_746L

class PermisosTest {

    @Test fun `con todo concedido no se pide aunque nunca se haya pedido`() =
        assertFalse(debePedirPermisos(exenta = true, notificacionesOk = true, ultimoDia = null, hoy = HOY))

    @Test fun `sin la exencion se pide`() =
        assertTrue(debePedirPermisos(exenta = false, notificacionesOk = true, ultimoDia = null, hoy = HOY))

    @Test fun `no vuelve el mismo dia`() =
        assertFalse(debePedirPermisos(exenta = false, notificacionesOk = false, ultimoDia = HOY, hoy = HOY))

    @Test fun `vuelve al dia siguiente`() =
        assertTrue(debePedirPermisos(exenta = true, notificacionesOk = false, ultimoDia = HOY - 1, hoy = HOY))
}
