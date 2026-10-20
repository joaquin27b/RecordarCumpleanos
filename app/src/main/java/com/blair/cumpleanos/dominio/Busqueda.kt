package com.blair.cumpleanos.dominio

import java.text.Normalizer
import java.util.Locale

private val MARCAS = "\\p{Mn}".toRegex()
private val ESPACIOS = "\\s+".toRegex()

/**
 * Emojis y lo que los arma: simbolos (So), tonos de piel (Sk), el keycap (Me), el
 * ZWJ y las etiquetas de banderas (Cf) y los selectores de variacion. Por code
 * point: el regex de Java ve el par sustituto como un solo caracter.
 */
private val EMOJIS = "[\\p{So}\\p{Sk}\\p{Me}\\p{Cf}\\uFE0E\\uFE0F]".toRegex()

/**
 * Forma comparable de un nombre: sin tildes, sin emojis, en minusculas, espacios colapsados.
 *
 * En Kotlin y no en SQL: LIKE y COLLATE NOCASE de SQLite solo pliegan ASCII y el
 * SQLite de Android no trae ICU, asi que "Á" nunca matchea "a" desde la query.
 * Locale.ROOT: en turco la "I" baja a "ı" y rompe la comparacion.
 * NFD separa la ñ en n + tilde: "Ñandú" queda "nandu" y "n" la encuentra. A
 * proposito, para un buscador es lo esperable.
 */
fun normalizar(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(MARCAS, "")
        .replace(EMOJIS, "")
        .lowercase(Locale.ROOT)
        .trim()
        .replace(ESPACIOS, " ")

/**
 * True si alguna palabra del nombre empieza con la consulta. Ambos ya normalizados.
 *
 * Por inicio de palabra y no contains: "a" trae Ana y no Ñandú; "lop" trae Ana López.
 */
fun coincide(nombreNormalizado: String, consultaNormalizada: String): Boolean =
    nombreNormalizado.startsWith(consultaNormalizada) || nombreNormalizado.contains(" $consultaNormalizada")

/** Fila con su nombre ya normalizado: se calcula una vez por fila, no por tecleo. */
data class ConClave<T>(val fila: T, val clave: String)

fun <T> filtrarPorNombre(filas: List<ConClave<T>>, consultaNormalizada: String): List<T> =
    filas.filter { coincide(it.clave, consultaNormalizada) }.map { it.fila }
