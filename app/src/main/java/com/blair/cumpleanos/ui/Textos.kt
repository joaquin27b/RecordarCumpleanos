package com.blair.cumpleanos.ui

import android.content.res.Resources
import android.text.format.DateFormat
import com.blair.cumpleanos.R
import com.blair.cumpleanos.dominio.Estado
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

/** "Diciembre 2026" / "December 2026" — cabecera del historial. */
fun nombreMes(ym: YearMonth, locale: Locale): String =
    fmt("MMMMy", locale).format(ym.atDay(1))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

/** "Cumple HOY" / "En 3 días" / "Hace 12 días" */
fun cuerpoCumple(res: Resources, diasRestantes: Int): String = when {
    diasRestantes < 0 ->
        res.getQuantityString(R.plurals.cumple_hace_dias, -diasRestantes, -diasRestantes)
    diasRestantes == 0 -> res.getString(R.string.cumple_hoy)
    diasRestantes == 1 -> res.getString(R.string.cumple_manana)
    else -> res.getQuantityString(R.plurals.cumple_en_dias, diasRestantes, diasRestantes)
}

/** "cumple 36 años". null si no se conoce el anio de nacimiento. */
fun textoEdad(res: Resources, edad: Int?): String? =
    edad?.let { res.getQuantityString(R.plurals.edad_cumple, it, it) }

fun etiquetaEstado(res: Resources, estado: Estado): String = when (estado) {
    Estado.HOY -> res.getString(R.string.estado_hoy)
    Estado.PROXIMO -> res.getString(R.string.estado_proximo)
    Estado.PASADO, Estado.OLVIDADO -> res.getString(R.string.estado_pasado)
}

/** "OCT" / "OCT": mes corto en mayusculas, sin el punto de la abreviatura. */
fun mesCorto(epochDay: Int, locale: Locale): String =
    fmt("MMM", locale).format(LocalDate.ofEpochDay(epochDay.toLong()))
        .replace(".", "")
        .uppercase(locale)
