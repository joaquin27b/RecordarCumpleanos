package com.blair.cumpleanos.dominio

/** Dias despues del festejo en que todavia se pregunta. Mas viejo ya no importa. */
const val VENTANA_ASISTENCIA = 30

/** Festejos por los que se pregunta hoy: de ayer a 30 dias atras. El de hoy todavia no paso. */
fun ventanaAsistencia(hoy: Int): IntRange = (hoy - VENTANA_ASISTENCIA)..(hoy - 1)

/**
 * La tarjeta de la app: festejo cargado, ya pasado (desde el dia SIGUIENTE),
 * dentro de los ultimos 30 dias y sin respuesta. Queda hasta que se responda o
 * salga de la ventana.
 */
fun preguntaAsistencia(fechaFestejoDia: Int?, asistio: Boolean?, hoy: Int): Boolean =
    fechaFestejoDia != null && asistio == null && fechaFestejoDia in ventanaAsistencia(hoy)

/**
 * La notificacion, una sola vez por ocurrencia: solo si nunca se pregunto, ni
 * por notificacion ni con la tarjeta. Pregunto y no contesto = no se insiste;
 * la tarjeta sigue esperando en la app.
 */
fun notificaAsistencia(fechaFestejoDia: Int?, asistio: Boolean?, preguntadaDia: Int?, hoy: Int): Boolean =
    preguntaAsistencia(fechaFestejoDia, asistio, hoy) && preguntadaDia == null

/**
 * UNA sola tarjeta: la que pidio la notificacion tocada, si sigue pendiente; si
 * no, la del festejo mas reciente. [pendientes] viene del mas reciente al mas viejo.
 */
fun <T> elegirAsistencia(pendientes: List<T>, id: (T) -> Long, pedida: Long?): T? =
    pendientes.firstOrNull { pedida != null && id(it) == pedida } ?: pendientes.firstOrNull()
