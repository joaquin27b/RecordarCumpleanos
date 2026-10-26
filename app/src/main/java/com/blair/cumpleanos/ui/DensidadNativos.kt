package com.blair.cumpleanos.ui

/**
 * Filas de datos minimas entre un nativo de la lista y el siguiente: con menos,
 * dos podrian entrar juntos en pantalla, y eso es causa de suspension en AdMob.
 */
const val SEPARACION_NATIVOS = 6

/** Hasta esta cantidad de filas va uno solo, despues de [ubicarNativos] `unicoTras`. */
const val POCAS_FILAS = 5

/**
 * Cuantas filas cuenta el historial: es una caja de alto fijo (scrollea por
 * dentro), asi que con muchos saludos ocupa lo mismo que con cuatro.
 */
const val FILAS_MAX_HISTORIAL = 4

/**
 * Despues de que secciones va un nativo en la lista principal.
 *
 * [filas] son las filas de datos de cada seccion, en el orden en que se ven
 * (ni encabezados ni anuncios). La ULTIMA es la del final de todo. Devuelve los
 * indices de las secciones tras las cuales va uno, en orden.
 *
 * - 0 filas: ninguno. La pantalla vacia no se llena de anuncios.
 * - 1 a [POCAS_FILAS]: uno solo, despues de la seccion [unicoTras] (en la lista,
 *   la ultima de cumpleaños). Dos no: con tan poco, entrarian juntos en pantalla.
 * - Mas: uno despues de la primera seccion con datos y los siguientes solo con
 *   [SEPARACION_NATIVOS] filas desde el anterior. Dos secciones cortas seguidas
 *   comparten uno: no va uno por seccion.
 * - Si el del final queda a menos de [SEPARACION_NATIVOS] filas del anterior, no
 *   van los dos: gana el del final y el anterior se saca. Salvo que el anterior
 *   sea el unico: ese se queda y el del final no va.
 */
fun ubicarNativos(filas: List<Int>, unicoTras: Int = filas.lastIndex): List<Int> {
    val total = filas.sum()
    if (total == 0) return emptyList()
    val final = filas.lastIndex
    if (total <= POCAS_FILAS) return listOf(unicoTras)
    val ubicados = mutableListOf<Int>()
    var desdeElAnterior = 0
    for ((i, n) in filas.withIndex()) {
        desdeElAnterior += n
        val toca = if (ubicados.isEmpty()) n > 0 else desdeElAnterior >= SEPARACION_NATIVOS
        if (!toca) continue
        ubicados += i
        desdeElAnterior = 0
    }
    // El del final quedo pegado al anterior: el anterior le cede el lugar. La
    // distancia al de antes solo crece, asi que la separacion se sigue cumpliendo.
    if (ubicados.last() != final && ubicados.size >= 2) ubicados[ubicados.lastIndex] = final
    return ubicados
}
