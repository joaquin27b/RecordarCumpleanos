package com.blair.cumpleanos.ui

/**
 * Filas de datos minimas entre un nativo de la lista y el siguiente: con menos,
 * dos podrian entrar juntos en pantalla, y eso es causa de suspension en AdMob.
 */
const val SEPARACION_NATIVOS = 6

/** Hasta esta cantidad de filas va uno solo, al final de todo. */
const val POCAS_FILAS = 5

/**
 * Despues de que secciones va un nativo en la lista principal.
 *
 * [filas] son las filas de datos (cumpleaños y personas; ni encabezados ni
 * anuncios) de cada seccion, en el orden en que se ven. Devuelve los indices de
 * las secciones tras las cuales va uno, en orden, como mucho [maximo].
 *
 * - 0 filas: ninguno. La pantalla vacia no se llena de anuncios.
 * - 1 a [POCAS_FILAS]: uno, despues de la ultima seccion.
 * - Mas: uno despues de la primera seccion con datos y los siguientes solo con
 *   [SEPARACION_NATIVOS] filas desde el anterior. Dos secciones cortas seguidas
 *   comparten uno: no va uno por seccion.
 */
fun ubicarNativos(filas: List<Int>, maximo: Int = Int.MAX_VALUE): List<Int> {
    val total = filas.sum()
    if (total == 0 || maximo <= 0) return emptyList()
    if (total <= POCAS_FILAS) return listOf(filas.lastIndex)
    val ubicados = mutableListOf<Int>()
    var desdeElAnterior = 0
    for ((i, n) in filas.withIndex()) {
        desdeElAnterior += n
        val toca = if (ubicados.isEmpty()) n > 0 else desdeElAnterior >= SEPARACION_NATIVOS
        if (!toca) continue
        ubicados += i
        if (ubicados.size == maximo) break
        desdeElAnterior = 0
    }
    return ubicados
}
