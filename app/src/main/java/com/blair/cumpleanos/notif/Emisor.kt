package com.blair.cumpleanos.notif

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.blair.cumpleanos.MainActivity
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.fechaConDia
import com.blair.cumpleanos.ui.fechaLarga
import java.util.Locale

/** Id de resumen fuera del rango de personaId para no pisar una notificacion de cumple. */
private const val ID_RESUMEN = Int.MAX_VALUE

/**
 * Los festejos viven en su propio espacio de ids: con personaId a secas, el
 * aviso del festejo pisaria al del cumple de la misma persona.
 */
private const val BASE_ID_FESTEJO = 1 shl 30

/** La pregunta de asistencia, en un tercer espacio: no pisa ni al cumple ni al festejo. */
private const val BASE_ID_ASISTENCIA = 3 shl 29

/** cumpleId de la pregunta de asistencia que abre la app al tocar la notificacion. */
const val EXTRA_ASISTENCIA = "asistencia"

object Emisor {

    /**
     * Una notificacion POR PERSONA, no por ocurrencia: el id es personaId, asi
     * el preaviso de D-7 se reemplaza por el de D-1 en vez de apilarse.
     *
     * Sin botones de accion: tocarla abre la app.
     */
    fun cumple(ctx: Context, personaId: Long, nombre: String, fechaDia: Int, diasRestantes: Int, edad: Int?, mudo: Boolean = false) {
        val loc: Locale = ctx.resources.configuration.locales[0]
        val res = ctx.resources
        val texto = when {
            // Sin año no se sabe cuantos cumple: el aviso no lo menciona.
            diasRestantes == 0 && edad != null -> res.getQuantityString(R.plurals.notif_cumple_hoy_edad, edad, nombre, edad)
            diasRestantes == 0 -> ctx.getString(R.string.notif_cumple_hoy, nombre)
            diasRestantes == 1 -> ctx.getString(R.string.notif_cumple_manana, nombre, fechaLarga(fechaDia, loc))
            else -> res.getQuantityString(R.plurals.notif_cumple_en, diasRestantes, nombre, diasRestantes, fechaLarga(fechaDia, loc))
        }
        emitir(ctx, personaId.toInt(), CANAL_CUMPLE, nombre, texto, NotificationCompat.PRIORITY_HIGH, mudo)
    }

    /** "festeja MAÑANA" el dia anterior, "festeja HOY" el dia del festejo. */
    fun festejo(ctx: Context, personaId: Long, nombre: String, fechaFestejoDia: Int, diasRestantes: Int, mudo: Boolean = false) {
        val loc: Locale = ctx.resources.configuration.locales[0]
        val texto = if (diasRestantes == 0) ctx.getString(R.string.notif_festeja_hoy, nombre)
        else ctx.getString(R.string.notif_festeja_manana, nombre, fechaConDia(fechaFestejoDia, loc))
        emitir(ctx, BASE_ID_FESTEJO + personaId.toInt(), CANAL_CUMPLE, nombre, texto, NotificationCompat.PRIORITY_HIGH, mudo)
    }

    /** "¿Fuiste al festejo de Patry?". Tocarla abre la app con la tarjeta arriba. */
    fun asistencia(ctx: Context, personaId: Long, cumpleId: Long, nombre: String, mudo: Boolean = false) {
        emitir(
            ctx, BASE_ID_ASISTENCIA + personaId.toInt(), CANAL_CUMPLE, nombre,
            ctx.getString(R.string.asistencia_pregunta, nombre), NotificationCompat.PRIORITY_HIGH, mudo,
        ) { putExtra(EXTRA_ASISTENCIA, cumpleId) }
    }

    fun resumen(ctx: Context, titulo: String, cuerpo: String) {
        emitir(ctx, ID_RESUMEN, CANAL_RESUMEN, titulo, cuerpo, NotificationCompat.PRIORITY_LOW)
    }

    private fun emitir(
        ctx: Context, id: Int, canal: String, titulo: String, texto: String, prioridad: Int,
        mudo: Boolean = false,
        extras: Intent.() -> Unit = {},
    ) {
        val abrir = PendingIntent.getActivity(
            ctx,
            id,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .apply(extras),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, canal)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(prioridad)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            // De madrugada sin sonido: queda en la pantalla de bloqueo para cuando se desbloquea.
            .setSilent(mudo)
            // Un nombre y una fecha: se puede leer con el telefono bloqueado.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revocado entre el chequeo y el notify.
        }
    }
}
