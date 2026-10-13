package com.blair.cumpleanos.dominio

/** Tiempo verbal de un evento contra hoy: "cumple" / "cumple HOY" / "cumplió". */
enum class Tiempo { FUTURO, HOY, PASADO }

/** Se decide aca y no en el recurso: cada tiempo es su propia clave de strings. */
fun tiempo(fechaDia: Int, hoy: Int): Tiempo = when {
    fechaDia > hoy -> Tiempo.FUTURO
    fechaDia == hoy -> Tiempo.HOY
    else -> Tiempo.PASADO
}
