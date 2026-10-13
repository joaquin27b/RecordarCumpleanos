package com.blair.cumpleanos.dominio

object Cadencia {

    /** Opciones que se le ofrecen al usuario. D-0 no esta: siempre avisa. */
    val DISPONIBLES = intArrayOf(30, 15, 7, 3, 1)

    /** Lo que trae una persona recien creada si el usuario no toca nada. */
    val POR_DEFECTO = intArrayOf(7, 1)

    /**
     * null = nunca configurado, se usa el default.
     * "" = el usuario apago todos los preavisos; queda solo D-0.
     */
    fun preavisosEfectivos(csv: String?): IntArray =
        if (csv == null) POR_DEFECTO
        else csv.split(',').mapNotNull { it.trim().toIntOrNull() }.toIntArray()

    /**
     * D-0 no es configurable: es la promesa central de la app.
     *
     * A diferencia de un vencimiento, PASADO no reavisa a diario: el saludo
     * tardio se ofrece en pantalla, no se machaca por notificacion.
     */
    fun avisa(fechaDia: Int, hoy: Int, avisoUltimoDia: Int?, preavisosCsv: String?): Boolean {
        if (avisoUltimoDia == hoy) return false
        val restantes = fechaDia - hoy
        if (restantes == 0) return true
        if (restantes < 0) return false
        return restantes in preavisosEfectivos(preavisosCsv)
    }
}

/** Dia del mes en que se emite el resumen. */
const val DIA_RESUMEN = 1

/**
 * True si toca emitir el resumen de este mes calendario.
 *
 * La guarda es "ya lo mande este mes", no "hoy es 1": con el telefono apagado
 * del 1 al 4, el tick del dia 4 lo emite igual.
 */
fun avisaResumen(ultimoYm: Int?, anio: Int, mes: Int, diaDelMes: Int): Boolean {
    val ym = anio * 100 + mes
    return diaDelMes >= DIA_RESUMEN && ultimoYm != ym
}
