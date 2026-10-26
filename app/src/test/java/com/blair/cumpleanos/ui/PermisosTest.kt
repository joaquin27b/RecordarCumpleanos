package com.blair.cumpleanos.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val HOY = 20_746L

class PermisosTest {

    @Test fun `con notificaciones permitidas no se pide aunque nunca se haya pedido`() =
        assertFalse(debePedirPermisos(notificacionesOk = true, ultimoDia = null, hoy = HOY))

    @Test fun `sin notificaciones se pide`() =
        assertTrue(debePedirPermisos(notificacionesOk = false, ultimoDia = null, hoy = HOY))

    @Test fun `no vuelve el mismo dia`() =
        assertFalse(debePedirPermisos(notificacionesOk = false, ultimoDia = HOY, hoy = HOY))

    @Test fun `vuelve al dia siguiente`() =
        assertTrue(debePedirPermisos(notificacionesOk = false, ultimoDia = HOY - 1, hoy = HOY))
}
