package com.blair.cumpleanos.dominio

/** Pasados 30 dias la app se calla sola: un cumpleanos viejo no es una deuda. */
const val UMBRAL_OLVIDADO = 30

enum class Estado { PROXIMO, HOY, PASADO, OLVIDADO }

/**
 * Estado derivado de una ocurrencia pendiente.
 *
 * Derivado, no persistido: si el telefono estuvo apagado tres semanas, la
 * pantalla muestra el estado correcto en el primer render.
 */
fun estado(fechaDia: Int, hoy: Int): Estado = when {
    fechaDia == hoy -> Estado.HOY
    fechaDia > hoy -> Estado.PROXIMO
    hoy - fechaDia <= UMBRAL_OLVIDADO -> Estado.PASADO
    else -> Estado.OLVIDADO
}
