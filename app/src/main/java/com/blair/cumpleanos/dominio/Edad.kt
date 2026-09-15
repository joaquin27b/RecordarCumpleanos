package com.blair.cumpleanos.dominio

/**
 * Anios que cumple en ESA ocurrencia. null si no se conoce el anio de nacimiento.
 *
 * Se resta contra el anio de la ocurrencia y no contra el actual: quien cumple
 * en febrero cae en el grupo "proximo anio" y debe mostrar la edad de 2027.
 */
fun edad(fechaDia: Int, anioNacimiento: Int?): Int? {
    if (anioNacimiento == null) return null
    val e = anioDe(fechaDia) - anioNacimiento
    return if (e >= 0) e else null
}
