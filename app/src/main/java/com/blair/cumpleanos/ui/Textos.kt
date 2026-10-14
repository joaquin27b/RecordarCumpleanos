package com.blair.cumpleanos.ui

import android.content.res.Resources
import android.text.format.DateFormat
import com.blair.cumpleanos.R
import com.blair.cumpleanos.dominio.Tiempo
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun fmt(skeleton: String, locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

/**
 * Dia, mes y anio: "25 de diciembre de 2026" / "December 25, 2026".
 *
 * El skeleton declara QUE campos se quieren; ICU decide orden y preposicion
 * segun el locale. Un patron fijo "d 'de' MMMM 'de' y" daria "25 de December".
 */
fun fechaDiaMesAnio(epochDay: Int, locale: Locale): String =
    fmt("dMMMMy", locale).format(LocalDate.ofEpochDay(epochDay.toLong()))

/** Sin anio: "25 de diciembre" / "December 25". Para notificaciones. */
fun fechaLarga(epochDay: Int, locale: Locale): String =
    fmt("dMMMM", locale).format(LocalDate.ofEpochDay(epochDay.toLong()))

/** Con el dia de la semana: "sábado, 18 de octubre" / "Saturday, October 18". El festejo. */
fun fechaConDia(epochDay: Int, locale: Locale): String =
    fmt("EEEEdMMMM", locale).format(LocalDate.ofEpochDay(epochDay.toLong()))

/** "Diciembre 2026" / "December 2026" — cabecera del historial. */
fun nombreMes(ym: YearMonth, locale: Locale): String =
    fmt("MMMMy", locale).format(ym.atDay(1))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

/**
 * "cumple 36 años" / "cumple HOY 36 años" / "cumplió 36 años". null sin año.
 *
 * Tres claves y no una con logica adentro: el tiempo verbal lo decide Kotlin
 * comparando la fecha del evento con hoy, el recurso solo pluraliza.
 */
fun textoEdad(res: Resources, edad: Int?, tiempo: Tiempo): String? = edad?.let {
    val id = when (tiempo) {
        Tiempo.FUTURO -> R.plurals.edad_futuro
        Tiempo.HOY -> R.plurals.edad_hoy
        Tiempo.PASADO -> R.plurals.edad_pasado
    }
    // La cantidad va DOS veces: una elige la forma, la otra llena el %d.
    res.getQuantityString(id, it, it)
}

/** "octubre" / "October": el mes solo, para el titulo del resumen mensual. */
fun nombreMesSolo(ym: YearMonth, locale: Locale): String =
    fmt("LLLL", locale).format(ym.atDay(1))

/** "OCT" / "OCT": mes corto en mayusculas, sin el punto de la abreviatura. */
fun mesCorto(epochDay: Int, locale: Locale): String =
    fmt("MMM", locale).format(LocalDate.ofEpochDay(epochDay.toLong()))
        .replace(".", "")
        .uppercase(locale)
