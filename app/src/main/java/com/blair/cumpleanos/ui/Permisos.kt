package com.blair.cumpleanos.ui

/**
 * Si en este arranque se pide el permiso de notificaciones, directo con la
 * ventana del sistema. Bateria no: Play la revisa y el aviso de las 00:05 no la
 * necesita (setAndAllowWhileIdle corre en reposo).
 *
 * notificacionesOk sale del sistema en cada arranque, no de una
 * preferencia: con todo en orden no se pide nunca, y si el usuario revierte
 * algo vuelve solo. El tope de una vez por dia rige solo mientras falte algo:
 * quien dice que no lo vuelve a ver mañana, no en cada apertura.
 *
 * @param ultimoDia epochDay de la ultima vez que se pidieron; null si nunca.
 */
internal fun debePedirPermisos(notificacionesOk: Boolean, ultimoDia: Long?, hoy: Long): Boolean {
    if (notificacionesOk) return false
    return ultimoDia == null || hoy - ultimoDia >= 1
}
