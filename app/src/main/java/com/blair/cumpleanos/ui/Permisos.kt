package com.blair.cumpleanos.ui

/**
 * Si en este arranque se piden los permisos que faltan (bateria y notificaciones),
 * directo con las ventanas del sistema. Igual que en Ya La Tomé.
 *
 * exenta y notificacionesOk salen del sistema en cada arranque, no de una
 * preferencia: con todo en orden no se pide nunca, y si el usuario revierte
 * algo vuelve solo. El tope de una vez por dia rige solo mientras falte algo:
 * quien dice que no lo vuelve a ver mañana, no en cada apertura.
 *
 * @param ultimoDia epochDay de la ultima vez que se pidieron; null si nunca.
 */
internal fun debePedirPermisos(exenta: Boolean, notificacionesOk: Boolean, ultimoDia: Long?, hoy: Long): Boolean {
    if (exenta && notificacionesOk) return false
    return ultimoDia == null || hoy - ultimoDia >= 1
}
