package com.blair.cumpleanos.dominio

import java.time.LocalDate
import java.time.YearMonth

/**
 * Proxima ocurrencia anual a partir de una dada.
 *
 * El ancla es diaHabitual, no el dia de fechaDia: nacido el 29/02, la
 * ocurrencia de un anio no bisiesto se clampea a 28 y sin el ancla ese
 * corrimiento quedaria para siempre.
 */
fun proxima(fechaDia: Int, diaHabitual: Int): Int {
    require(diaHabitual in 1..31) { "diaHabitual fuera de rango" }
    val ym = YearMonth.from(LocalDate.ofEpochDay(fechaDia.toLong())).plusYears(1)
    return ym.atDay(minOf(diaHabitual, ym.lengthOfMonth())).toEpochDay().toInt()
}

/**
 * Primera ocurrencia >= hoy para un dia/mes dados.
 *
 * El anio de nacimiento NO participa: sembrar desde el nacimiento generaria
 * una ocurrencia por anio vivido.
 */
fun proximaOcurrencia(dia: Int, mes: Int, hoy: Int): Int {
    require(dia in 1..31) { "dia fuera de rango" }
    require(mes in 1..12) { "mes fuera de rango" }
    val anio = LocalDate.ofEpochDay(hoy.toLong()).year
    val esteAnio = enAnio(dia, mes, anio)
    return if (esteAnio >= hoy) esteAnio else enAnio(dia, mes, anio + 1)
}

/** Clampea 29/02 a 28/02 en anios no bisiestos en vez de tirar DateTimeException. */
private fun enAnio(dia: Int, mes: Int, anio: Int): Int {
    val ym = YearMonth.of(anio, mes)
    return ym.atDay(minOf(dia, ym.lengthOfMonth())).toEpochDay().toInt()
}

/** Dia del mes de un epochDay. */
fun diaDelMes(epochDay: Int): Int = LocalDate.ofEpochDay(epochDay.toLong()).dayOfMonth

/** Mes de un epochDay, 1..12. */
fun mesDe(epochDay: Int): Int = LocalDate.ofEpochDay(epochDay.toLong()).monthValue

/** Anio de un epochDay. */
fun anioDe(epochDay: Int): Int = LocalDate.ofEpochDay(epochDay.toLong()).year
