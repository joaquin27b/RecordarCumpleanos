package com.blair.cumpleanos.notif

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilencioTest {

    @Test fun `la alarma de las 00 05 no suena`() = assertTrue(silencioso(0))

    @Test fun `a las 7 todavia no suena`() = assertTrue(silencioso(7))

    @Test fun `desde las 8 suena`() = assertFalse(silencioso(HORA_SONORA))

    @Test fun `el tick de las 9 suena`() = assertFalse(silencioso(9))
}
