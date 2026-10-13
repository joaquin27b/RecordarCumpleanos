package com.blair.cumpleanos.dominio

import java.time.LocalDate
import java.time.Year

/** Resultado de validar el año de nacimiento tipeado en el alta. */
sealed interface AnioNacimiento {
    /** Campo vacio o "no sé el año": no se calcula edad. */
    data object Desconocido : AnioNacimiento
    data class Valido(val anio: Int) : AnioNacimiento
    /** No son 4 cifras o es anterior a 1900. */
    data object Invalido : AnioNacimiento
    /** Nadie nace en el futuro: año posterior, o este año con fecha que no llego. */
    data object Futuro : AnioNacimiento
    /** Nacido el 29/02 en un año no bisiesto. */
    data object SinBisiesto : AnioNacimiento
}

/**
 * Valida el año contra el dia y mes elegidos y contra hoy.
 *
 * El año va aparte del calendario: vacio es valido y significa "no se sabe".
 * hoy entra por parametro, nunca se lee del sistema aca adentro.
 */
fun validarAnio(texto: String, dia: Int, mes: Int, hoy: LocalDate): AnioNacimiento {
    val t = texto.trim()
    if (t.isEmpty()) return AnioNacimiento.Desconocido
    val anio = t.toIntOrNull()
    if (anio == null || t.length != 4 || anio < 1900) return AnioNacimiento.Invalido
    if (anio > hoy.year) return AnioNacimiento.Futuro
    if (dia == 29 && mes == 2 && !Year.isLeap(anio.toLong())) return AnioNacimiento.SinBisiesto
    if (LocalDate.of(anio, mes, dia).isAfter(hoy)) return AnioNacimiento.Futuro
    return AnioNacimiento.Valido(anio)
}
